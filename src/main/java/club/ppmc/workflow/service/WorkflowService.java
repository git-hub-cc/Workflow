package club.ppmc.workflow.service;

import club.ppmc.workflow.domain.*;
import club.ppmc.workflow.dto.CompleteTaskRequest;
import club.ppmc.workflow.dto.DeployWorkflowRequest;
import club.ppmc.workflow.dto.HistoryActivityDto;
import club.ppmc.workflow.dto.TaskDto;
import club.ppmc.workflow.dto.WorkflowTemplateResponse;
import club.ppmc.workflow.exception.ResourceNotFoundException;
import club.ppmc.workflow.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.*;
import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.history.HistoricTaskInstance;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author 你的名字
 * @description 工作流核心服务
 */
@Service
@RequiredArgsConstructor
@Transactional
public class WorkflowService {

    private final RepositoryService repositoryService;
    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final HistoryService historyService;
    private final IdentityService identityService; // 用于设置流程发起人

    private final WorkflowTemplateRepository templateRepository;
    private final WorkflowInstanceRepository instanceRepository;
    private final FormDefinitionRepository formDefinitionRepository;
    private final FormSubmissionRepository formSubmissionRepository;
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
                .tenantId("default")
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
     * 获取或创建一个工作流模板的 DTO。
     * 如果模板已存在，则返回它。如果不存在，则在内存中生成一个默认模板 DTO。
     * @param formId 表单定义 ID
     * @return WorkflowTemplateResponse
     */
    @Transactional(readOnly = true)
    public WorkflowTemplateResponse getOrCreateWorkflowTemplate(Long formId) {
        // 1. 验证表单是否存在
        formDefinitionRepository.findById(formId)
                .orElseThrow(() -> new ResourceNotFoundException("未找到表单定义 ID: " + formId));

        // 2. 尝试查找已存在的模板
        return templateRepository.findByFormDefinitionId(formId)
                .map(template -> {
                    // 3a. 如果存在，则转换并返回
                    WorkflowTemplateResponse dto = new WorkflowTemplateResponse();
                    dto.setFormDefinitionId(template.getFormDefinition().getId());
                    dto.setBpmnXml(template.getBpmnXml());
                    dto.setProcessDefinitionKey(template.getProcessDefinitionKey());
                    return dto;
                })
                .orElseGet(() -> {
                    // 3b. 如果不存在，则生成一个包含基础审批流程的默认 DTO
                    String processDefinitionKey = "Process_Form_" + formId;
                    String defaultXml = String.format("""
                        <?xml version="1.0" encoding="UTF-8"?>
                        <bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" xmlns:di="http://www.omg.org/spec/DD/20100524/DI" xmlns:camunda="http://camunda.org/schema/1.0/bpmn" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" id="Definitions_1" targetNamespace="http://bpmn.io/schema/bpmn" exporter="Camunda Modeler" exporterVersion="5.20.0">
                          <bpmn:process id="%s" name="新流程" isExecutable="true">
                            <bpmn:startEvent id="StartEvent_1" name="开始">
                              <bpmn:outgoing>Flow_1</bpmn:outgoing>
                            </bpmn:startEvent>
                            <bpmn:sequenceFlow id="Flow_1" sourceRef="StartEvent_1" targetRef="Activity_ManagerApprove" />
                            <bpmn:userTask id="Activity_ManagerApprove" name="部门经理审批" camunda:assignee="manager001">
                              <bpmn:incoming>Flow_1</bpmn:incoming>
                              <bpmn:outgoing>Flow_2</bpmn:outgoing>
                            </bpmn:userTask>
                            <bpmn:endEvent id="EndEvent_1" name="结束">
                              <bpmn:incoming>Flow_2</bpmn:incoming>
                            </bpmn:endEvent>
                            <bpmn:sequenceFlow id="Flow_2" sourceRef="Activity_ManagerApprove" targetRef="EndEvent_1" />
                          </bpmn:process>
                          <bpmndi:BPMNDiagram id="BPMNDiagram_1">
                            <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="%s">
                              <bpmndi:BPMNShape id="StartEvent_1_di" bpmnElement="StartEvent_1">
                                <dc:Bounds x="179" y="102" width="36" height="36" />
                                <bpmndi:BPMNLabel>
                                  <dc:Bounds x="185" y="145" width="22" height="14" />
                                </bpmndi:BPMNLabel>
                              </bpmndi:BPMNShape>
                              <bpmndi:BPMNShape id="Activity_ManagerApprove_di" bpmnElement="Activity_ManagerApprove">
                                <dc:Bounds x="270" y="80" width="100" height="80" />
                              </bpmndi:BPMNShape>
                              <bpmndi:BPMNShape id="EndEvent_1_di" bpmnElement="EndEvent_1">
                                <dc:Bounds x="432" y="102" width="36" height="36" />
                                <bpmndi:BPMNLabel>
                                  <dc:Bounds x="438" y="145" width="22" height="14" />
                                </bpmndi:BPMNLabel>
                              </bpmndi:BPMNShape>
                              <bpmndi:BPMNEdge id="Flow_1_di" bpmnElement="Flow_1">
                                <di:waypoint x="215" y="120" />
                                <di:waypoint x="270" y="120" />
                              </bpmndi:BPMNEdge>
                              <bpmndi:BPMNEdge id="Flow_2_di" bpmnElement="Flow_2">
                                <di:waypoint x="370" y="120" />
                                <di:waypoint x="432" y="120" />
                              </bpmndi:BPMNEdge>
                            </bpmndi:BPMNPlane>
                          </bpmndi:BPMNDiagram>
                        </bpmn:definitions>
                        """, processDefinitionKey, processDefinitionKey);

                    WorkflowTemplateResponse dto = new WorkflowTemplateResponse();
                    dto.setFormDefinitionId(formId);
                    dto.setBpmnXml(defaultXml);
                    dto.setProcessDefinitionKey(processDefinitionKey);
                    return dto;
                });
    }


    /**
     * 启动一个新的工作流实例
     * @param submission 触发流程的表单提交记录
     */
    public void startWorkflow(FormSubmission submission) {
        // 在启动流程前，设置 Camunda 的认证用户，这样 Camunda 会自动记录此用户为流程的发起人
        try {
            identityService.setAuthenticatedUserId(submission.getSubmitterId());
            templateRepository.findByFormDefinitionId(submission.getFormDefinition().getId()).ifPresent(template -> {
                try {
                    Map<String, Object> variables = objectMapper.readValue(submission.getDataJson(), new TypeReference<>() {});
                    variables.put("submitterId", submission.getSubmitterId());
                    variables.put("formSubmissionId", submission.getId());

                    // 处理动态审批人: 约定如果表单数据中有名为 "nextAssignee" 的字段，则将其作为流程变量
                    if (variables.containsKey("nextAssignee")) {
                        Object assignee = variables.get("nextAssignee");
                        if (assignee instanceof String && !((String) assignee).isEmpty()) {
                            variables.put("nextAssignee", assignee);
                        }
                    }

                    ProcessInstance camundaInstance = runtimeService.startProcessInstanceByKey(
                            template.getProcessDefinitionKey(),
                            submission.getId().toString(), // 设置 BusinessKey 为 submissionId
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
        } finally {
            // 清理线程局部变量，这是一个非常重要的好习惯
            identityService.clearAuthentication();
        }
    }

    /**
     * 完成一个用户任务
     * @param camundaTaskId Camunda 中的任务 ID
     * @param request 包含决定和评论
     */
    public void completeUserTask(String camundaTaskId, CompleteTaskRequest request) {
        Task task = taskService.createTaskQuery().taskId(camundaTaskId).singleResult();
        if (task == null) {
            throw new ResourceNotFoundException("在 Camunda 中未找到任务 ID: " + camundaTaskId);
        }

        // 将审批意见作为任务的局部变量存储
        if (request.getApprovalComment() != null && !request.getApprovalComment().isEmpty()) {
            taskService.setVariableLocal(camundaTaskId, "approvalComment", request.getApprovalComment());
        }

        // 设置用于网关判断的流程变量
        Map<String, Object> variables = Map.of(
                "approved", request.getDecision().equals(CompleteTaskRequest.Decision.APPROVED)
        );

        taskService.complete(camundaTaskId, variables);
    }

    /**
     * 根据用户ID获取其待办任务列表
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

    /**
     * 获取单个任务详情
     * @param camundaTaskId 任务ID
     * @return 任务DTO
     */
    @Transactional(readOnly = true)
    public TaskDto getTaskDetails(String camundaTaskId) {
        Task task = taskService.createTaskQuery().taskId(camundaTaskId).singleResult();
        if (task == null) {
            throw new ResourceNotFoundException("在 Camunda 中未找到任务 ID: " + camundaTaskId);
        }
        return convertCamundaTaskToDto(task);
    }

    /**
     * 内部方法，将 Camunda Task 转换为 TaskDto
     */
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

    /**
     * 根据业务ID（FormSubmission ID）获取工作流历史记录
     * @param submissionId 表单提交 ID
     * @return 历史活动 DTO 列表
     */
    @Transactional(readOnly = true)
    public List<HistoryActivityDto> getWorkflowHistoryBySubmissionId(Long submissionId) {
        FormSubmission submission = formSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("未找到提交记录 ID: " + submissionId));

        if (submission.getWorkflowInstance() == null) {
            return Collections.emptyList();
        }

        String processInstanceId = submission.getWorkflowInstance().getProcessInstanceId();

        // 1. 获取所有活动实例 (事件、任务、网关等)
        List<HistoricActivityInstance> activityInstances = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricActivityInstanceStartTime().asc()
                .list();

        // 2. 获取所有已完成的任务实例，用于提取审批意见
        List<HistoricTaskInstance> taskInstances = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .finished()
                .list();
        Map<String, HistoricTaskInstance> taskInstanceMap = taskInstances.stream()
                .collect(Collectors.toMap(HistoricTaskInstance::getId, Function.identity()));

        // 3. 获取任务相关的局部变量 (审批意见)
        List<HistoricVariableInstance> variableInstances = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId)
                .variableName("approvalComment")
                .list();
        Map<String, HistoricVariableInstance> taskComments = variableInstances.stream()
                .filter(v -> v.getTaskId() != null)
                .collect(Collectors.toMap(HistoricVariableInstance::getTaskId, Function.identity()));

        // 4. 获取全局变量 (最终决定)
        Map<String, Object> processVariables = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId)
                .list().stream()
                .collect(Collectors.toMap(HistoricVariableInstance::getName, HistoricVariableInstance::getValue));

        // 5. 预加载所有可能的用户信息
        Set<String> userIds = Stream.concat(
                        activityInstances.stream().map(HistoricActivityInstance::getAssignee),
                        Stream.of(submission.getSubmitterId())
                )
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<HistoryActivityDto> historyList = new ArrayList<>();

        // 6. 添加一个“发起申请”的虚拟节点
        historyList.add(HistoryActivityDto.builder()
                .activityName("发起申请")
                .activityType("startEvent")
                .assigneeId(submission.getSubmitterId())
                .assigneeName(userMap.getOrDefault(submission.getSubmitterId(), new User()).getName())
                .startTime(submission.getCreatedAt())
                .endTime(submission.getCreatedAt())
                .build());

        // 7. 组合真实活动节点数据
        for (HistoricActivityInstance activity : activityInstances) {
            if (!activity.getActivityType().equals("userTask") && !activity.getActivityType().endsWith("EndEvent")) {
                continue;
            }

            HistoryActivityDto.HistoryActivityDtoBuilder dtoBuilder = HistoryActivityDto.builder()
                    .activityId(activity.getId())
                    .activityName(activity.getActivityName())
                    .activityType(activity.getActivityType())
                    .assigneeId(activity.getAssignee())
                    .assigneeName(Optional.ofNullable(activity.getAssignee()).map(id -> userMap.getOrDefault(id, new User()).getName()).orElse(null))
                    .startTime(Optional.ofNullable(activity.getStartTime()).map(d -> d.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()).orElse(null))
                    .endTime(Optional.ofNullable(activity.getEndTime()).map(d -> d.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()).orElse(null))
                    .durationInMillis(activity.getDurationInMillis());

            // 【修正】使用 activity.getId() 来关联任务实例和变量
            if (activity.getActivityType().equals("userTask") && taskInstanceMap.containsKey(activity.getId())) {
                HistoricVariableInstance commentVar = taskComments.get(activity.getId());
                if (commentVar != null) {
                    dtoBuilder.comment((String) commentVar.getValue());
                }
            }

            if(activity.getActivityType().endsWith("EndEvent")) {
                Object approvedVar = processVariables.get("approved");
                if (approvedVar instanceof Boolean) {
                    dtoBuilder.decision(((Boolean) approvedVar) ? "APPROVED" : "REJECTED");
                    dtoBuilder.activityName(((Boolean) approvedVar) ? "审批通过" : "审批拒绝");
                } else {
                    dtoBuilder.activityName("流程结束");
                }
            }

            historyList.add(dtoBuilder.build());
        }
        return historyList;
    }

    /**
     * 权限校验方法: 检查用户是否是指定任务的办理人
     * @param camundaTaskId 任务ID
     * @param username 用户名 (ID)
     * @return 如果是，返回 true
     */
    public boolean isTaskOwner(String camundaTaskId, String username) {
        if (username == null) return false;
        Task task = taskService.createTaskQuery().taskId(camundaTaskId).singleResult();
        return task != null && username.equals(task.getAssignee());
    }

    /**
     * 权限校验方法: 检查用户是否是指定申请的提交人
     * @param submissionId 申请ID
     * @param username 用户名 (ID)
     * @return 如果是，返回 true
     */
    public boolean isSubmissionOwner(Long submissionId, String username) {
        if (username == null) return false;
        return formSubmissionRepository.findById(submissionId)
                .map(FormSubmission::getSubmitterId)
                .map(username::equals)
                .orElse(false);
    }
}