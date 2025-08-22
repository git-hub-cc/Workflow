package club.ppmc.workflow.service;

import club.ppmc.workflow.aop.LogOperation;
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
import org.camunda.bpm.engine.task.TaskQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author cc
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
    private final FileAttachmentRepository fileAttachmentRepository;
    private final RoleRepository roleRepository; // 【核心新增】

    private final ObjectMapper objectMapper;

    @LogOperation(module = "流程管理", action = "部署流程", targetIdExpression = "#request.processDefinitionKey")
    public void deployWorkflow(DeployWorkflowRequest request) {
        FormDefinition formDef = formDefinitionRepository.findById(request.getFormDefinitionId())
                .orElseThrow(() -> new ResourceNotFoundException("未找到表单定义 ID: " + request.getFormDefinitionId()));

        Deployment deployment = repositoryService.createDeployment()
                .addString(request.getProcessDefinitionKey() + ".bpmn", request.getBpmnXml())
                .name("Deployment for form: " + formDef.getName())
                .deploy();

        WorkflowTemplate template = templateRepository.findByFormDefinitionId(request.getFormDefinitionId())
                .orElse(new WorkflowTemplate());

        template.setFormDefinition(formDef);
        template.setBpmnXml(request.getBpmnXml());
        template.setProcessDefinitionKey(request.getProcessDefinitionKey());
        template.setCamundaDeploymentId(deployment.getId());

        templateRepository.save(template);
    }

    @LogOperation(module = "流程管理", action = "保存流程草稿", targetIdExpression = "#request.processDefinitionKey")
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
                    String processDefinitionKey = "Process_Form_" + formId;
                    String defaultXml = String.format("""
                        <?xml version="1.0" encoding="UTF-8"?>
                        <bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" xmlns:camunda="http://camunda.org/schema/1.0/bpmn" xmlns:di="http://www.omg.org/spec/DD/20100524/DI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" id="Definitions_0zla03s" targetNamespace="http://bpmn.io/schema/bpmn" exporter="Camunda Modeler" exporterVersion="5.20.0">
                          <bpmn:process id="%s" name="新流程" isExecutable="true" camunda:historyTimeToLive="P180D">
                            <bpmn:startEvent id="StartEvent_1" name="开始">
                              <bpmn:outgoing>Flow_Start_To_Approve</bpmn:outgoing>
                            </bpmn:startEvent>
                            <bpmn:sequenceFlow id="Flow_Start_To_Approve" sourceRef="StartEvent_1" targetRef="Activity_ManagerApprove" />
                            <bpmn:userTask id="Activity_ManagerApprove" name="上级审批" camunda:assignee="${managerId}">
                              <bpmn:incoming>Flow_Start_To_Approve</bpmn:incoming>
                              <bpmn:outgoing>Flow_Approve_To_Gateway</bpmn:outgoing>
                            </bpmn:userTask>
                            <bpmn:exclusiveGateway id="Gateway_Approved" name="审批是否通过?">
                              <bpmn:incoming>Flow_Approve_To_Gateway</bpmn:incoming>
                              <bpmn:outgoing>Flow_Approved</bpmn:outgoing>
                              <bpmn:outgoing>Flow_Rejected</bpmn:outgoing>
                            </bpmn:exclusiveGateway>
                            <bpmn:sequenceFlow id="Flow_Approve_To_Gateway" sourceRef="Activity_ManagerApprove" targetRef="Gateway_Approved" />
                            <bpmn:sequenceFlow id="Flow_Approved" name="通过" sourceRef="Gateway_Approved" targetRef="Activity_Archive">
                              <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">${approved}</bpmn:conditionExpression>
                            </bpmn:sequenceFlow>
                            <bpmn:sequenceFlow id="Flow_Rejected" name="拒绝" sourceRef="Gateway_Approved" targetRef="Activity_Reject" />
                            <bpmn:serviceTask id="Activity_Archive" name="归档(通过)" camunda:delegateExpression="${archiveProcessDelegate}">
                              <bpmn:incoming>Flow_Approved</bpmn:incoming>
                              <bpmn:outgoing>Flow_Archive_ToEnd</bpmn:outgoing>
                            </bpmn:serviceTask>
                            <bpmn:endEvent id="EndEvent_Approved" name="审批通过">
                              <bpmn:incoming>Flow_Archive_ToEnd</bpmn:incoming>
                            </bpmn:endEvent>
                            <bpmn:sequenceFlow id="Flow_Archive_ToEnd" sourceRef="Activity_Archive" targetRef="EndEvent_Approved" />
                            <bpmn:serviceTask id="Activity_Reject" name="归档(拒绝)" camunda:delegateExpression="${rejectProcessDelegate}">
                              <bpmn:incoming>Flow_Rejected</bpmn:incoming>
                              <bpmn:outgoing>Flow_Reject_ToEnd</bpmn:outgoing>
                            </bpmn:serviceTask>
                            <bpmn:endEvent id="EndEvent_Rejected" name="审批拒绝">
                              <bpmn:incoming>Flow_Reject_ToEnd</bpmn:incoming>
                            </bpmn:endEvent>
                            <bpmn:sequenceFlow id="Flow_Reject_ToEnd" sourceRef="Activity_Reject" targetRef="EndEvent_Rejected" />
                          </bpmn:process>
                          <bpmndi:BPMNDiagram id="BPMNDiagram_1">
                            <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="%s">
                              <bpmndi:BPMNShape id="_BPMNShape_StartEvent_2" bpmnElement="StartEvent_1">
                                <dc:Bounds x="179" y="102" width="36" height="36" />
                                <bpmndi:BPMNLabel>
                                  <dc:Bounds x="185" y="145" width="22" height="14" />
                                </bpmndi:BPMNLabel>
                              </bpmndi:BPMNShape>
                              <bpmndi:BPMNShape id="Activity_1c76uyj_di" bpmnElement="Activity_ManagerApprove">
                                <dc:Bounds x="270" y="80" width="100" height="80" />
                              </bpmndi:BPMNShape>
                              <bpmndi:BPMNShape id="Gateway_0j5x0jd_di" bpmnElement="Gateway_Approved" isMarkerVisible="true">
                                <dc:Bounds x="435" y="95" width="50" height="50" />
                                <bpmndi:BPMNLabel>
                                  <dc:Bounds x="424" y="65" width="72" height="14" />
                                </bpmndi:BPMNLabel>
                              </bpmndi:BPMNShape>
                              <bpmndi:BPMNShape id="Activity_0sq1112_di" bpmnElement="Activity_Archive">
                                <dc:Bounds x="540" y="80" width="100" height="80" />
                              </bpmndi:BPMNShape>
                              <bpmndi:BPMNShape id="Event_1l8csf7_di" bpmnElement="EndEvent_Approved">
                                <dc:Bounds x="702" y="102" width="36" height="36" />
                                <bpmndi:BPMNLabel>
                                  <dc:Bounds x="695" y="145" width="51" height="14" />
                                </bpmndi:BPMNLabel>
                              </bpmndi:BPMNShape>
                              <bpmndi:BPMNShape id="Activity_0s140wi_di" bpmnElement="Activity_Reject">
                                <dc:Bounds x="540" y="200" width="100" height="80" />
                                <bpmndi:BPMNLabel />
                              </bpmndi:BPMNShape>
                              <bpmndi:BPMNShape id="Event_06d8x10_di" bpmnElement="EndEvent_Rejected">
                                <dc:Bounds x="702" y="222" width="36" height="36" />
                                <bpmndi:BPMNLabel>
                                  <dc:Bounds x="695" y="265" width="51" height="14" />
                                </bpmndi:BPMNLabel>
                              </bpmndi:BPMNShape>
                              <bpmndi:BPMNEdge id="Flow_00yq16d_di" bpmnElement="Flow_Start_To_Approve">
                                <di:waypoint x="215" y="120" />
                                <di:waypoint x="270" y="120" />
                              </bpmndi:BPMNEdge>
                              <bpmndi:BPMNEdge id="Flow_1c79aab_di" bpmnElement="Flow_Approve_To_Gateway">
                                <di:waypoint x="370" y="120" />
                                <di:waypoint x="435" y="120" />
                              </bpmndi:BPMNEdge>
                              <bpmndi:BPMNEdge id="Flow_08vqy1p_di" bpmnElement="Flow_Approved">
                                <di:waypoint x="485" y="120" />
                                <di:waypoint x="540" y="120" />
                                <bpmndi:BPMNLabel>
                                  <dc:Bounds x="506" y="102" width="22" height="14" />
                                </bpmndi:BPMNLabel>
                              </bpmndi:BPMNEdge>
                              <bpmndi:BPMNEdge id="Flow_1y5bbd1_di" bpmnElement="Flow_Rejected">
                                <di:waypoint x="460" y="145" />
                                <di:waypoint x="460" y="240" />
                                <di:waypoint x="540" y="240" />
                                <bpmndi:BPMNLabel>
                                  <dc:Bounds x="466" y="190" width="22" height="14" />
                                </bpmndi:BPMNLabel>
                              </bpmndi:BPMNEdge>
                              <bpmndi:BPMNEdge id="Flow_0k64d3v_di" bpmnElement="Flow_Archive_ToEnd">
                                <di:waypoint x="640" y="120" />
                                <di:waypoint x="702" y="120" />
                              </bpmndi:BPMNEdge>
                              <bpmndi:BPMNEdge id="Flow_0x6o019_di" bpmnElement="Flow_Reject_ToEnd">
                                <di:waypoint x="640" y="240" />
                                <di:waypoint x="702" y="240" />
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

                    User submitter = userRepository.findById(submission.getSubmitterId())
                            .orElseThrow(() -> new IllegalStateException("数据异常：找不到提交人，ID: " + submission.getSubmitterId()));

                    variables.put("submitterId", submission.getSubmitterId());
                    variables.put("submitterName", submitter.getName());
                    variables.put("formName", submission.getFormDefinition().getName());
                    variables.put("formSubmissionId", submission.getId());
                    variables.put("initiator", submitter.getId()); // Camunda specific variable

                    if (submitter.getManager() != null) {
                        String managerId = submitter.getManager().getId();
                        variables.put("managerId", managerId);
                        log.info("已为提交者 '{}' 找到上级经理 '{}'，并设置为流程变量 'managerId'。", submission.getSubmitterId(), managerId);
                    } else {
                        throw new IllegalStateException("提交者 '" + submitter.getName() + "' 没有设置上级经理，无法启动需要上级审批的流程。");
                    }

                    // --- 【核心新增】注入角色变量，以供流程定义使用 ---
                    roleRepository.findByName("FINANCE_APPROVER").ifPresent(role ->
                            variables.put("financeRole", role.getName())
                    );

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

    @LogOperation(module = "任务处理", action = "完成任务", targetIdExpression = "#camundaTaskId")
    public void completeUserTask(String camundaTaskId, CompleteTaskRequest request) {
        Task task = taskService.createTaskQuery().taskId(camundaTaskId).singleResult();
        if (task == null) {
            throw new ResourceNotFoundException("在 Camunda 中未找到任务 ID: " + camundaTaskId);
        }

        String processInstanceId = task.getProcessInstanceId();

        WorkflowInstance instance = instanceRepository.findByProcessInstanceId(processInstanceId)
                .orElseThrow(() -> new IllegalStateException("数据不一致：找不到流程实例 " + processInstanceId + " 对应的本地实例"));
        FormSubmission submission = instance.getFormSubmission();

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

        if (request.getAttachmentIds() != null) {
            Set<Long> existingIds = submission.getAttachments().stream()
                    .map(FileAttachment::getId)
                    .collect(Collectors.toSet());
            Set<Long> newIds = new HashSet<>(request.getAttachmentIds());

            List<FileAttachment> toRemove = submission.getAttachments().stream()
                    .filter(att -> !newIds.contains(att.getId()))
                    .collect(Collectors.toList());

            Set<Long> toAddIds = new HashSet<>(newIds);
            toAddIds.removeAll(existingIds);

            if (!toRemove.isEmpty()) {
                submission.getAttachments().removeAll(toRemove);
                log.info("从申请单 #{} 中移除了 {} 个旧附件。", submission.getId(), toRemove.size());
            }

            if (!toAddIds.isEmpty()) {
                List<FileAttachment> attachmentsToAdd = fileAttachmentRepository.findAllById(toAddIds);
                for (FileAttachment att : attachmentsToAdd) {
                    att.setFormSubmission(submission);
                    submission.getAttachments().add(att);
                }
                log.info("为申请单 #{} 新增了 {} 个附件关联。", submission.getId(), attachmentsToAdd.size());
            }
        }
        formSubmissionRepository.save(submission);

        if (request.getApprovalComment() != null && !request.getApprovalComment().isEmpty()) {
            taskService.setVariableLocal(camundaTaskId, "approvalComment", request.getApprovalComment());
        }

        Map<String, Object> variables = new HashMap<>();
        boolean isApproved = request.getDecision().equals(CompleteTaskRequest.Decision.APPROVED);

        variables.put("approved", isApproved);
        variables.put("passed", isApproved);
        variables.put("qc_passed", isApproved);

        taskService.complete(camundaTaskId, variables);
    }

    @Transactional(readOnly = true)
    public Page<TaskDto> getPendingTasksForUser(String assigneeId, String keyword, Pageable pageable) {
        User user = userRepository.findById(assigneeId)
                .orElseThrow(() -> new ResourceNotFoundException("未找到用户: " + assigneeId));
        List<String> userGroupIds = user.getUserGroups().stream()
                .map(UserGroup::getName)
                .collect(Collectors.toList());

        TaskQuery query = taskService.createTaskQuery().active();

        TaskQuery orQuery = query.or()
                .taskAssignee(assigneeId)
                .taskCandidateUser(assigneeId);

        if (!userGroupIds.isEmpty()) {
            orQuery.taskCandidateGroupIn(userGroupIds);
        }

        query = orQuery.endOr();


        if (StringUtils.hasText(keyword)) {
            query.processVariableValueLike("formName", "%" + keyword + "%");
        }

        long total = query.count();

        List<Task> camundaTasks = query.orderByTaskCreateTime().desc()
                .listPage((int) pageable.getOffset(), pageable.getPageSize());

        List<TaskDto> taskDtos = camundaTasks.stream()
                .map(this::convertCamundaTaskToDto)
                .collect(Collectors.toList());

        return new PageImpl<>(taskDtos, pageable, total);
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

        dto.setSubmitterName((String) variables.getOrDefault("submitterName", "未知"));
        dto.setFormName((String) variables.getOrDefault("formName", "未知表单"));

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

        // 【核心修复】增加排序和合并函数，解决 Duplicate Key 问题
        Map<String, Object> processVariables = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByTenantId().asc() // 1. 保证按时间升序排列
                .list().stream()
                .collect(Collectors.toMap(
                        HistoricVariableInstance::getName,
                        HistoricVariableInstance::getValue,
                        (oldValue, newValue) -> newValue // 2. 如果 key 重复，总是取新值
                ));

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

            if (activity.getActivityType().equals("userTask") && activity.getTaskId() != null) {
                HistoricVariableInstance commentVar = taskComments.get(activity.getTaskId());
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
        if (!StringUtils.hasText(username)) {
            return false;
        }

        Optional<User> userOpt = userRepository.findById(username);
        List<String> userGroupIds = userOpt.map(user ->
                user.getUserGroups().stream()
                        .map(UserGroup::getName)
                        .collect(Collectors.toList())
        ).orElse(Collections.emptyList());

        TaskQuery query = taskService.createTaskQuery()
                .taskId(camundaTaskId)
                .or()
                .taskAssignee(username)
                .taskCandidateUser(username);

        if (!userGroupIds.isEmpty()) {
            query.taskCandidateGroupIn(userGroupIds);
        }

        long count = query.endOr().count();

        return count > 0;
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

        List<String> userGroupIds = userRepository.findById(username)
                .map(user -> user.getUserGroups().stream()
                        .map(UserGroup::getName)
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());

        TaskQuery activeQuery = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .active()
                .or()
                .taskAssignee(username)
                .taskCandidateUser(username);

        if (!userGroupIds.isEmpty()) {
            activeQuery.taskCandidateGroupIn(userGroupIds);
        }

        if (activeQuery.endOr().count() > 0) {
            log.debug("权限检查: 用户 '{}' 是申请单 #{} 的当前待办参与者。", username, submissionId);
            return true;
        }


        long historicTaskCount = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .taskAssignee(username)
                .finished()
                .count();

        if (historicTaskCount > 0) {
            log.debug("权限检查: 用户 '{}' 是申请单 #{} 的历史审批人。", username, submissionId);
            return true;
        }

        log.debug("权限检查: 用户 '{}' 不是申请单 #{} 的任何阶段参与者。", username, submissionId);
        return false;
    }
}