package club.ppmc.workflow.service;

import club.ppmc.workflow.domain.*;
import club.ppmc.workflow.dto.*;
import club.ppmc.workflow.exception.ResourceNotFoundException;
import club.ppmc.workflow.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.*;
import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.camunda.bpm.engine.history.HistoricTaskInstance;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

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
@Slf4j
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
    // --- 【新增】 ---
    private final FileAttachmentRepository fileAttachmentRepository;

    private final ObjectMapper objectMapper;

    // ... [deployWorkflow, updateWorkflowTemplate, getOrCreateWorkflowTemplate methods remain unchanged] ...
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

    public WorkflowTemplateResponse updateWorkflowTemplate(Long formId, UpdateWorkflowTemplateRequest request) {
        FormDefinition formDef = formDefinitionRepository.findById(formId)
                .orElseThrow(() -> new ResourceNotFoundException("未找到表单定义 ID: " + formId));

        WorkflowTemplate template = templateRepository.findByFormDefinitionId(formId)
                .orElse(new WorkflowTemplate());

        template.setFormDefinition(formDef);
        template.setBpmnXml(request.getBpmnXml());
        template.setProcessDefinitionKey(request.getProcessDefinitionKey());

        WorkflowTemplate savedTemplate = templateRepository.save(template);

        WorkflowTemplateResponse dto = new WorkflowTemplateResponse();
        dto.setFormDefinitionId(savedTemplate.getFormDefinition().getId());
        dto.setBpmnXml(savedTemplate.getBpmnXml());
        dto.setProcessDefinitionKey(savedTemplate.getProcessDefinitionKey());
        return dto;
    }

    @Transactional(readOnly = true)
    public WorkflowTemplateResponse getOrCreateWorkflowTemplate(Long formId) {
        formDefinitionRepository.findById(formId)
                .orElseThrow(() -> new ResourceNotFoundException("未找到表单定义 ID: " + formId));

        return templateRepository.findByFormDefinitionId(formId)
                .map(template -> {
                    WorkflowTemplateResponse dto = new WorkflowTemplateResponse();
                    dto.setFormDefinitionId(template.getFormDefinition().getId());
                    dto.setBpmnXml(template.getBpmnXml());
                    dto.setProcessDefinitionKey(template.getProcessDefinitionKey());
                    return dto;
                })
                .orElseGet(() -> {
                    // --- 【修改】默认模板现在使用服务任务来动态查找审批人 ---
                    String processDefinitionKey = "Process_Form_" + formId;
                    String defaultXml = String.format("""
                        <?xml version="1.0" encoding="UTF-8"?>
                        <bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" xmlns:camunda="http://camunda.org/schema/1.0/bpmn" xmlns:di="http://www.omg.org/spec/DD/20100524/DI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" id="Definitions_0z3f9b2" targetNamespace="http://bpmn.io/schema/bpmn" exporter="Camunda Modeler" exporterVersion="5.22.0">
                          <bpmn:process id="%s" name="新流程" isExecutable="true" camunda:historyTimeToLive="P180D">
                            <bpmn:startEvent id="StartEvent_1" name="开始">
                              <bpmn:outgoing>Flow_Start_FindManager</bpmn:outgoing>
                            </bpmn:startEvent>
                            <bpmn:serviceTask id="Task_FindManager" name="查找审批人" camunda:delegateExpression="${findManagerDelegate}">
                              <bpmn:extensionElements>
                                <camunda:inputOutput>
                                  <camunda:inputParameter name="managerLevel">1</camunda:inputParameter>
                                </camunda:inputOutput>
                              </bpmn:extensionElements>
                              <bpmn:incoming>Flow_Start_FindManager</bpmn:incoming>
                              <bpmn:outgoing>Flow_FindManager_Approve</bpmn:outgoing>
                            </bpmn:serviceTask>
                            <bpmn:userTask id="Task_ManagerApprove" name="上级审批" camunda:assignee="${assignee}">
                              <bpmn:incoming>Flow_FindManager_Approve</bpmn:incoming>
                              <bpmn:outgoing>Flow_Approve_End</bpmn:outgoing>
                            </bpmn:userTask>
                            <bpmn:endEvent id="EndEvent_1" name="结束">
                              <bpmn:incoming>Flow_Approve_End</bpmn:incoming>
                            </bpmn:endEvent>
                            <bpmn:sequenceFlow id="Flow_Start_FindManager" sourceRef="StartEvent_1" targetRef="Task_FindManager" />
                            <bpmn:sequenceFlow id="Flow_FindManager_Approve" sourceRef="Task_FindManager" targetRef="Task_ManagerApprove" />
                            <bpmn:sequenceFlow id="Flow_Approve_End" sourceRef="Task_ManagerApprove" targetRef="EndEvent_1" />
                          </bpmn:process>
                          <bpmndi:BPMNDiagram id="BPMNDiagram_1">
                            <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="%s">
                              <bpmndi:BPMNShape id="_BPMNShape_StartEvent_2" bpmnElement="StartEvent_1">
                                <dc:Bounds x="179" y="99" width="36" height="36" />
                                <bpmndi:BPMNLabel>
                                  <dc:Bounds x="185" y="142" width="22" height="14" />
                                </bpmndi:BPMNLabel>
                              </bpmndi:BPMNShape>
                              <bpmndi:BPMNShape id="Activity_1c8n5n3_di" bpmnElement="Task_FindManager">
                                <dc:Bounds x="270" y="77" width="100" height="80" />
                              </bpmndi:BPMNShape>
                              <bpmndi:BPMNShape id="Activity_0k3xje3_di" bpmnElement="Task_ManagerApprove">
                                <dc:Bounds x="430" y="77" width="100" height="80" />
                              </bpmndi:BPMNShape>
                              <bpmndi:BPMNShape id="Event_1k297se_di" bpmnElement="EndEvent_1">
                                <dc:Bounds x="592" y="99" width="36" height="36" />
                                <bpmndi:BPMNLabel>
                                  <dc:Bounds x="598" y="142" width="22" height="14" />
                                </bpmndi:BPMNLabel>
                              </bpmndi:BPMNShape>
                              <bpmndi:BPMNEdge id="Flow_0d6k7k4_di" bpmnElement="Flow_Start_FindManager">
                                <di:waypoint x="215" y="117" />
                                <di:waypoint x="270" y="117" />
                              </bpmndi:BPMNEdge>
                              <bpmndi:BPMNEdge id="Flow_0y1b99a_di" bpmnElement="Flow_FindManager_Approve">
                                <di:waypoint x="370" y="117" />
                                <di:waypoint x="430" y="117" />
                              </bpmndi:BPMNEdge>
                              <bpmndi:BPMNEdge id="Flow_11x042r_di" bpmnElement="Flow_Approve_End">
                                <di:waypoint x="530" y="117" />
                                <di:waypoint x="592" y="117" />
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


    public void startWorkflow(FormSubmission submission) {
        try {
            identityService.setAuthenticatedUserId(submission.getSubmitterId());
            templateRepository.findByFormDefinitionId(submission.getFormDefinition().getId()).ifPresent(template -> {
                try {
                    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                            .processDefinitionKey(template.getProcessDefinitionKey())
                            .latestVersion()
                            .singleResult();

                    if (processDefinition == null) {
                        log.warn("工作流模板存在 (form ID: {}), 但其流程定义 key '{}' 并未部署在 Camunda 引擎中。" +
                                        "将跳过工作流启动 (submission ID: {}).",
                                submission.getFormDefinition().getId(),
                                template.getProcessDefinitionKey(),
                                submission.getId());
                        return;
                    }

                    Map<String, Object> variables = objectMapper.readValue(submission.getDataJson(), new TypeReference<>() {});
                    // --- 【核心修改：简化变量设置】 ---
                    // 启动时只设置最核心的、业务无关的变量。
                    // 审批人查找等业务逻辑已移入BPMN流程定义中（通过Delegate）。
                    variables.put("submitterId", submission.getSubmitterId());
                    variables.put("formSubmissionId", submission.getId());
                    // --- 【修改结束】 ---

                    ProcessInstance camundaInstance = runtimeService.startProcessInstanceByKey(
                            template.getProcessDefinitionKey(),
                            submission.getId().toString(),
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
            identityService.clearAuthentication();
        }
    }

    public void completeUserTask(String camundaTaskId, CompleteTaskRequest request) {
        Task task = taskService.createTaskQuery().taskId(camundaTaskId).singleResult();
        if (task == null) {
            throw new ResourceNotFoundException("在 Camunda 中未找到任务 ID: " + camundaTaskId);
        }

        String processInstanceId = task.getProcessInstanceId();

        WorkflowInstance instance = instanceRepository.findByProcessInstanceId(processInstanceId)
                .orElseThrow(() -> new IllegalStateException("数据不一致：找不到流程实例 " + processInstanceId + " 对应的本地实例"));
        FormSubmission submission = instance.getFormSubmission();

        // --- 【核心修改：处理表单和附件更新】 ---
        if (request.getUpdatedFormData() != null && !request.getUpdatedFormData().isBlank()) {
            submission.setDataJson(request.getUpdatedFormData());
            log.info("已更新申请单 (ID: {}) 的表单数据。", submission.getId());

            try {
                Map<String, Object> updatedVariables = objectMapper.readValue(request.getUpdatedFormData(), new TypeReference<>() {});
                runtimeService.setVariables(processInstanceId, updatedVariables);
                log.info("已从重新提交的表单数据中更新流程实例 (ID: {}) 的变量。", processInstanceId);
            } catch (JsonProcessingException e) {
                log.error("解析更新后的表单数据失败，流程实例 ID: {}", processInstanceId, e);
                throw new RuntimeException("无效的 JSON 格式 (updatedFormData)", e);
            }
        }

        if (!CollectionUtils.isEmpty(request.getAttachmentIds())) {
            // 1. 解除旧附件的关联
            submission.getAttachments().clear();

            // 2. 关联新附件
            List<FileAttachment> newAttachments = fileAttachmentRepository.findAllById(request.getAttachmentIds());
            for (FileAttachment attachment : newAttachments) {
                attachment.setFormSubmission(submission);
            }
            submission.getAttachments().addAll(newAttachments);
            log.info("已更新申请单 (ID: {}) 的附件列表。", submission.getId());
        }
        formSubmissionRepository.save(submission);
        // --- 【修改结束】 ---


        if (request.getApprovalComment() != null && !request.getApprovalComment().isEmpty()) {
            taskService.setVariableLocal(camundaTaskId, "approvalComment", request.getApprovalComment());
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", request.getDecision().equals(CompleteTaskRequest.Decision.APPROVED));

        taskService.complete(camundaTaskId, variables);
    }

    // ... [getPendingTasksForUser, getTaskDetails, convertCamundaTaskToDto, getWorkflowHistoryBySubmissionId, isTaskOwner, etc. methods remain unchanged] ...
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

    @Transactional(readOnly = true)
    public List<HistoryActivityDto> getWorkflowHistoryBySubmissionId(Long submissionId) {
        FormSubmission submission = formSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("未找到提交记录 ID: " + submissionId));

        if (submission.getWorkflowInstance() == null) {
            return Collections.emptyList();
        }

        String processInstanceId = submission.getWorkflowInstance().getProcessInstanceId();

        List<HistoricActivityInstance> activityInstances = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricActivityInstanceStartTime().asc()
                .list();

        List<HistoricTaskInstance> taskInstances = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .finished()
                .list();
        Map<String, HistoricTaskInstance> taskInstanceMap = taskInstances.stream()
                .collect(Collectors.toMap(HistoricTaskInstance::getId, Function.identity()));

        List<HistoricVariableInstance> variableInstances = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId)
                .variableName("approvalComment")
                .list();
        Map<String, HistoricVariableInstance> taskComments = variableInstances.stream()
                .filter(v -> v.getTaskId() != null)
                .collect(Collectors.toMap(HistoricVariableInstance::getTaskId, Function.identity()));

        Map<String, Object> processVariables = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId)
                .list().stream()
                .collect(Collectors.toMap(HistoricVariableInstance::getName, HistoricVariableInstance::getValue));

        Set<String> userIds = Stream.concat(
                        activityInstances.stream().map(HistoricActivityInstance::getAssignee),
                        Stream.of(submission.getSubmitterId())
                )
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<HistoryActivityDto> historyList = new ArrayList<>();

        historyList.add(HistoryActivityDto.builder()
                .activityName("发起申请")
                .activityType("startEvent")
                .assigneeId(submission.getSubmitterId())
                .assigneeName(userMap.getOrDefault(submission.getSubmitterId(), new User()).getName())
                .startTime(submission.getCreatedAt())
                .endTime(submission.getCreatedAt())
                .build());

        for (HistoricActivityInstance activity : activityInstances) {
            if (!activity.getActivityType().equals("userTask") && !activity.getActivityType().endsWith("EndEvent") && !activity.getActivityType().equals("serviceTask")) {
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

    public boolean isTaskOwner(String camundaTaskId, String username) {
        if (username == null) return false;
        Task task = taskService.createTaskQuery().taskId(camundaTaskId).singleResult();
        return task != null && username.equals(task.getAssignee());
    }

    public boolean isSubmissionOwner(Long submissionId, String username) {
        if (username == null) return false;
        return formSubmissionRepository.findById(submissionId)
                .map(FormSubmission::getSubmitterId)
                .map(username::equals)
                .orElse(false);
    }

    public boolean isTaskAssigneeForSubmission(Long submissionId, String username) {
        if (username == null) {
            return false;
        }

        Optional<String> processInstanceIdOpt = formSubmissionRepository.findById(submissionId)
                .map(FormSubmission::getWorkflowInstance)
                .map(WorkflowInstance::getProcessInstanceId);

        if (processInstanceIdOpt.isEmpty()) {
            return false;
        }

        String processInstanceId = processInstanceIdOpt.get();

        long historicTaskCount = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .taskAssignee(username)
                .count();

        if (historicTaskCount > 0) {
            log.debug("权限检查: 用户 '{}' 是申请单 #{} 的历史审批人。", username, submissionId);
            return true;
        }

        long activeTaskCount = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .taskAssignee(username)
                .active()
                .count();

        if (activeTaskCount > 0) {
            log.debug("权限检查: 用户 '{}' 是申请单 #{} 的当前待办人。", username, submissionId);
            return true;
        }

        log.debug("权限检查: 用户 '{}' 不是申请单 #{} 的任何阶段审批人。", username, submissionId);
        return false;
    }
}