package club.ppmc.workflow.service;

import club.ppmc.workflow.domain.FormDefinition;
import club.ppmc.workflow.domain.FormSubmission;
import club.ppmc.workflow.domain.WorkflowInstance;
import club.ppmc.workflow.domain.WorkflowTemplate;
import club.ppmc.workflow.dto.CompleteTaskRequest;
import club.ppmc.workflow.dto.DeployWorkflowRequest;
import club.ppmc.workflow.dto.TaskDto;
import club.ppmc.workflow.exception.ResourceNotFoundException;
import club.ppmc.workflow.repository.FormDefinitionRepository;
import club.ppmc.workflow.repository.UserRepository;
import club.ppmc.workflow.repository.WorkflowInstanceRepository;
import club.ppmc.workflow.repository.WorkflowTemplateRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author 你的名字
 * @description 工作流核心服务 (已重构，集成 Camunda)
 */
@Service
@RequiredArgsConstructor
@Transactional
public class WorkflowService {

    private final RepositoryService repositoryService;
    private final RuntimeService runtimeService;
    private final TaskService taskService;

    private final WorkflowTemplateRepository templateRepository;
    private final WorkflowInstanceRepository instanceRepository;
    private final FormDefinitionRepository formDefinitionRepository;
    private final UserRepository userRepository;

    private final ObjectMapper objectMapper;

    /**
     * 部署一个新的工作流定义到 Camunda 引擎
     * @param request 包含 BPMN XML 和关联信息的请求
     */
    public void deployWorkflow(DeployWorkflowRequest request) {
        FormDefinition formDef = formDefinitionRepository.findById(request.getFormDefinitionId())
                .orElseThrow(() -> new ResourceNotFoundException("未找到表单定义 ID: " + request.getFormDefinitionId()));

        Deployment deployment = repositoryService.createDeployment()
                .addString(request.getProcessDefinitionKey() + ".bpmn", request.getBpmnXml())
                .name("Deployment for form: " + formDef.getName())
                .tenantId("default") // 建议为部署设置一个租户ID
                .deploy();

        WorkflowTemplate template = templateRepository.findByFormDefinitionId(request.getFormDefinitionId())
                .orElse(new WorkflowTemplate());

        template.setFormDefinition(formDef);
        template.setBpmnXml(request.getBpmnXml());
        template.setProcessDefinitionKey(request.getProcessDefinitionKey());
        template.setCamundaDeploymentId(deployment.getId());

        templateRepository.save(template);
    }

    /**
     * 启动一个新的工作流实例
     * @param submission 触发流程的表单提交记录
     */
    public void startWorkflow(FormSubmission submission) {
        templateRepository.findByFormDefinitionId(submission.getFormDefinition().getId()).ifPresent(template -> {
            try {
                Map<String, Object> variables = objectMapper.readValue(submission.getDataJson(), new TypeReference<Map<String, Object>>() {});
                variables.put("submitterId", submission.getSubmitterId());
                variables.put("formSubmissionId", submission.getId());

                ProcessInstance camundaInstance = runtimeService.startProcessInstanceByKey(
                        template.getProcessDefinitionKey(),
                        variables
                );

                WorkflowInstance localInstance = new WorkflowInstance();
                localInstance.setTemplate(template);
                localInstance.setFormSubmission(submission);
                localInstance.setProcessInstanceId(camundaInstance.getId());

                instanceRepository.save(localInstance);
                submission.setWorkflowInstance(localInstance);

            } catch (JsonProcessingException e) {
                throw new RuntimeException("解析表单数据以启动工作流失败", e);
            }
        });
    }

    /**
     * 完成一个用户任务
     * @param camundaTaskId Camunda 中的任务 ID
     * @param request 包含决定和评论
     */
    public void completeUserTask(String camundaTaskId, CompleteTaskRequest request) {
        String decisionVariable = "approved";
        Map<String, Object> variables = Map.of(
                decisionVariable, request.getDecision().equals(CompleteTaskRequest.Decision.APPROVED)
        );

        taskService.complete(camundaTaskId, variables);
    }

    /**
     * 根据用户ID获取其待办任务列表 (直接从 Camunda 查询)
     * @param assigneeId 办理人ID
     * @return 任务 DTO 列表
     */
    @Transactional(readOnly = true)
    public List<TaskDto> getPendingTasksForUser(String assigneeId) {
        List<Task> camundaTasks = taskService.createTaskQuery()
                .taskAssignee(assigneeId)
                .active()
                .orderByTaskCreateTime().desc()
                .list();

        return camundaTasks.stream()
                .map(this::convertCamundaTaskToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TaskDto getTaskDetails(String camundaTaskId) {
        Task task = taskService.createTaskQuery().taskId(camundaTaskId).singleResult();
        if (task == null) {
            throw new ResourceNotFoundException("在 Camunda 中未找到任务 ID: " + camundaTaskId);
        }
        return convertCamundaTaskToDto(task);
    }

    private TaskDto convertCamundaTaskToDto(Task camundaTask) {
        TaskDto dto = new TaskDto();
        dto.setCamundaTaskId(camundaTask.getId());
        dto.setStepName(camundaTask.getName());
        dto.setCreatedAt(camundaTask.getCreateTime());

        Map<String, Object> variables = runtimeService.getVariables(camundaTask.getProcessInstanceId());

        Object formSubmissionIdObj = variables.get("formSubmissionId");
        if (formSubmissionIdObj instanceof Number) {
            dto.setFormSubmissionId(((Number) formSubmissionIdObj).longValue());
        }

        Object submitterIdObj = variables.get("submitterId");
        if (submitterIdObj instanceof String) {
            userRepository.findById((String)submitterIdObj).ifPresent(user -> dto.setSubmitterName(user.getName()));
        }

        instanceRepository.findByProcessInstanceId(camundaTask.getProcessInstanceId()).ifPresent(instance -> {
            dto.setFormName(instance.getTemplate().getFormDefinition().getName());
        });

        return dto;
    }
}