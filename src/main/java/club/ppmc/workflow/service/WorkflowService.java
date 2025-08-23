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
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.camunda.bpm.engine.history.HistoricTaskInstance;
import org.camunda.bpm.engine.history.HistoricTaskInstanceQuery;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.task.TaskQuery;
// --- 【核心新增】引入 Camunda Model API 和 Regex 相关包 ---
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.ConditionExpression;
import org.camunda.bpm.model.bpmn.instance.ExclusiveGateway;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    private final RoleRepository roleRepository;

    private final ObjectMapper objectMapper;

    // --- 【核心新增】注入 Spring 上下文 ---
    private final ApplicationContext applicationContext;

    // ... (deployWorkflow, updateWorkflowTemplate, getOrCreateWorkflowTemplate 方法保持不变)
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
      <bpmn:outgoing>Flow_1p6l8ge</bpmn:outgoing>
    </bpmn:startEvent>
    <bpmn:sequenceFlow id="Flow_1p6l8ge" sourceRef="StartEvent_1" targetRef="Activity_0bmvcuj" />
    <bpmn:exclusiveGateway id="Gateway_0wgpc9l">
      <bpmn:incoming>Flow_0rg62ya</bpmn:incoming>
      <bpmn:outgoing>Flow_1olivqv</bpmn:outgoing>
      <bpmn:outgoing>Flow_0s8894b</bpmn:outgoing>
    </bpmn:exclusiveGateway>
    <bpmn:sequenceFlow id="Flow_0rg62ya" sourceRef="Activity_0bmvcuj" targetRef="Gateway_0wgpc9l" />
    <bpmn:sequenceFlow id="Flow_1olivqv" name="继续" sourceRef="Gateway_0wgpc9l" targetRef="Activity_1xlcl7u">
      <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">${preparationOutcome == 'proceed'}</bpmn:conditionExpression>
    </bpmn:sequenceFlow>
    <bpmn:exclusiveGateway id="Gateway_1pbw0x6">
      <bpmn:incoming>Flow_0imfrb2</bpmn:incoming>
      <bpmn:outgoing>Flow_0twujez</bpmn:outgoing>
      <bpmn:outgoing>Flow_0omffa0</bpmn:outgoing>
      <bpmn:outgoing>Flow_110p3r9</bpmn:outgoing>
    </bpmn:exclusiveGateway>
    <bpmn:sequenceFlow id="Flow_0imfrb2" sourceRef="Activity_1xlcl7u" targetRef="Gateway_1pbw0x6" />
    <bpmn:endEvent id="Event_0mtkumc">
      <bpmn:incoming>Flow_0twujez</bpmn:incoming>
    </bpmn:endEvent>
    <bpmn:sequenceFlow id="Flow_0twujez" name="同意" sourceRef="Gateway_1pbw0x6" targetRef="Event_0mtkumc">
      <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">${taskOutcome == 'approved'}</bpmn:conditionExpression>
    </bpmn:sequenceFlow>
    <bpmn:endEvent id="Event_1agf6lw">
      <bpmn:incoming>Flow_0omffa0</bpmn:incoming>
    </bpmn:endEvent>
    <bpmn:sequenceFlow id="Flow_0omffa0" name="拒绝" sourceRef="Gateway_1pbw0x6" targetRef="Event_1agf6lw">
      <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">${taskOutcome == 'rejected'}</bpmn:conditionExpression>
    </bpmn:sequenceFlow>
    <bpmn:sequenceFlow id="Flow_110p3r9" name="打回发起人" sourceRef="Gateway_1pbw0x6" targetRef="Activity_0bmvcuj">
      <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">${taskOutcome == 'returnToInitiator'}</bpmn:conditionExpression>
    </bpmn:sequenceFlow>
    <bpmn:userTask id="Activity_0bmvcuj" name="发起" camunda:assignee="${initiator}">
      <bpmn:incoming>Flow_1p6l8ge</bpmn:incoming>
      <bpmn:incoming>Flow_110p3r9</bpmn:incoming>
      <bpmn:outgoing>Flow_0rg62ya</bpmn:outgoing>
    </bpmn:userTask>
    <bpmn:userTask id="Activity_1xlcl7u" name="审核" camunda:assignee="${managerId}">
      <bpmn:incoming>Flow_1olivqv</bpmn:incoming>
      <bpmn:outgoing>Flow_0imfrb2</bpmn:outgoing>
    </bpmn:userTask>
    <bpmn:endEvent id="Event_0pvvl3v">
      <bpmn:incoming>Flow_0s8894b</bpmn:incoming>
    </bpmn:endEvent>
    <bpmn:sequenceFlow id="Flow_0s8894b" name="终止" sourceRef="Gateway_0wgpc9l" targetRef="Event_0pvvl3v">
      <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">${preparationOutcome == 'terminate'}</bpmn:conditionExpression>
    </bpmn:sequenceFlow>
  </bpmn:process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="%s">
      <bpmndi:BPMNShape id="_BPMNShape_StartEvent_2" bpmnElement="StartEvent_1">
        <dc:Bounds x="179" y="102" width="36" height="36" />
        <bpmndi:BPMNLabel>
          <dc:Bounds x="185" y="145" width="22" height="14" />
        </bpmndi:BPMNLabel>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="Gateway_0wgpc9l_di" bpmnElement="Gateway_0wgpc9l" isMarkerVisible="true">
        <dc:Bounds x="425" y="95" width="50" height="50" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="Gateway_1pbw0x6_di" bpmnElement="Gateway_1pbw0x6" isMarkerVisible="true">
        <dc:Bounds x="685" y="95" width="50" height="50" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="Event_0mtkumc_di" bpmnElement="Event_0mtkumc">
        <dc:Bounds x="792" y="102" width="36" height="36" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="Event_1agf6lw_di" bpmnElement="Event_1agf6lw">
        <dc:Bounds x="792" y="212" width="36" height="36" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="Activity_18y6bq4_di" bpmnElement="Activity_0bmvcuj">
        <dc:Bounds x="270" y="80" width="100" height="80" />
        <bpmndi:BPMNLabel />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="Activity_0lz1pwx_di" bpmnElement="Activity_1xlcl7u">
        <dc:Bounds x="530" y="80" width="100" height="80" />
        <bpmndi:BPMNLabel />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="Event_0pvvl3v_di" bpmnElement="Event_0pvvl3v">
        <dc:Bounds x="532" y="212" width="36" height="36" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNEdge id="Flow_1p6l8ge_di" bpmnElement="Flow_1p6l8ge">
        <di:waypoint x="215" y="120" />
        <di:waypoint x="270" y="120" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_0rg62ya_di" bpmnElement="Flow_0rg62ya">
        <di:waypoint x="370" y="120" />
        <di:waypoint x="425" y="120" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_1olivqv_di" bpmnElement="Flow_1olivqv">
        <di:waypoint x="475" y="120" />
        <di:waypoint x="530" y="120" />
        <bpmndi:BPMNLabel>
          <dc:Bounds x="492" y="102" width="22" height="14" />
        </bpmndi:BPMNLabel>
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_0imfrb2_di" bpmnElement="Flow_0imfrb2">
        <di:waypoint x="630" y="120" />
        <di:waypoint x="685" y="120" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_0twujez_di" bpmnElement="Flow_0twujez">
        <di:waypoint x="735" y="120" />
        <di:waypoint x="792" y="120" />
        <bpmndi:BPMNLabel>
          <dc:Bounds x="753" y="102" width="22" height="14" />
        </bpmndi:BPMNLabel>
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_0omffa0_di" bpmnElement="Flow_0omffa0">
        <di:waypoint x="710" y="145" />
        <di:waypoint x="710" y="230" />
        <di:waypoint x="792" y="230" />
        <bpmndi:BPMNLabel>
          <dc:Bounds x="714" y="185" width="22" height="14" />
        </bpmndi:BPMNLabel>
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_110p3r9_di" bpmnElement="Flow_110p3r9">
        <di:waypoint x="710" y="95" />
        <di:waypoint x="710" y="-30" />
        <di:waypoint x="320" y="-30" />
        <di:waypoint x="320" y="80" />
        <bpmndi:BPMNLabel>
          <dc:Bounds x="488" y="-48" width="55" height="14" />
        </bpmndi:BPMNLabel>
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_0s8894b_di" bpmnElement="Flow_0s8894b">
        <di:waypoint x="450" y="145" />
        <di:waypoint x="450" y="230" />
        <di:waypoint x="532" y="230" />
        <bpmndi:BPMNLabel>
          <dc:Bounds x="454" y="185" width="22" height="14" />
        </bpmndi:BPMNLabel>
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
     * 【核心修改】重载 startWorkflow 方法，增加 initialAction 参数
     */
    public void startWorkflow(FormSubmission submission, String initialAction) {
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

                    // --- 【核心新增逻辑】自动完成第一个任务 ---
                    if (StringUtils.hasText(initialAction)) {
                        autoCompleteFirstTask(camundaInstance.getId(), submission.getSubmitterId(), initialAction);
                    }
                    // --- 【新增逻辑结束】 ---

                } catch (JsonProcessingException e) {
                    throw new RuntimeException("解析表单数据以启动工作流失败", e);
                }
            });
        } finally {
            identityService.clearAuthentication();
        }
    }

    /**
     * 【重载】为了兼容旧的调用，保留一个不带 initialAction 的版本
     */
    public void startWorkflow(FormSubmission submission) {
        startWorkflow(submission, null);
    }

    /**
     * 【新增】自动完成流程的第一个任务
     */
    private void autoCompleteFirstTask(String processInstanceId, String submitterId, String action) {
        Task firstTask = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .taskAssignee(submitterId)
                .singleResult();

        if (firstTask != null) {
            log.info("找到流程实例 {} 的第一个任务 {}，准备以动作 '{}' 自动完成。", processInstanceId, firstTask.getId(), action);
            CompleteTaskRequest request = new CompleteTaskRequest();
            // 假设第一个任务后的网关是基于 preparationOutcome
            request.setPreparationOutcome(action);
            // 默认决策是 APPROVED，因为这个动作只是为了驱动流程，而不是真正的审批
            request.setDecision(CompleteTaskRequest.Decision.APPROVED);

            // 直接调用内部方法完成任务
            completeUserTaskInternal(firstTask.getId(), request);
        } else {
            log.warn("无法为流程实例 {} 找到分配给 {} 的初始任务，无法自动完成。", processInstanceId, submitterId);
        }
    }


    /**
     * 【核心重构】将 completeUserTask 拆分为外部接口和内部实现
     */
    @LogOperation(module = "任务处理", action = "完成任务", targetIdExpression = "#camundaTaskId")
    public void completeUserTask(String camundaTaskId, CompleteTaskRequest request) {
        completeUserTaskInternal(camundaTaskId, request);
    }

    /**
     * 内部任务完成逻辑，不带日志注解，以便被其他Service方法调用
     */
    private void completeUserTaskInternal(String camundaTaskId, CompleteTaskRequest request) {
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

        // --- 【核心修改】开始 ---

        // 1. 设置通用的 `taskOutcome`，用于驱动流程离开当前任务节点
        if (request.getDecision() != null) {
            String outcome = switch (request.getDecision()) {
                case APPROVED -> "approved";
                case REJECTED -> "rejected";
                case RETURN_TO_INITIATOR -> "returnToInitiator";
                case RETURN_TO_PREVIOUS -> "returnToPrevious";
            };
            variables.put("taskOutcome", outcome);
        }

        // 2. 检查并设置新的、独立的 `preparationOutcome` 变量
        if (request.getPreparationOutcome() != null && !request.getPreparationOutcome().isBlank()) {
            variables.put("preparationOutcome", request.getPreparationOutcome());
            log.info("为流程实例 {} 设置准备阶段决策变量: {}", processInstanceId, request.getPreparationOutcome());
        }

        // --- 【核心修改】结束 ---


        // 兼容旧流程可能需要的变量
        if (request.getDecision() != null) {
            boolean isApproved = request.getDecision().equals(CompleteTaskRequest.Decision.APPROVED);
            variables.put("approved", isApproved);
            variables.put("passed", isApproved);
            variables.put("qc_passed", isApproved);
        }

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
    public Page<CompletedTaskDto> getCompletedTasksForUser(String assigneeId, String keyword, Pageable pageable) {
        HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery()
                .taskAssignee(assigneeId)
                .finished();

        if (StringUtils.hasText(keyword)) {
            query.processVariableValueLike("formName", "%" + keyword + "%");
        }

        long total = query.count();

        List<HistoricTaskInstance> historicTasks = query.orderByHistoricTaskInstanceEndTime().desc()
                .listPage((int) pageable.getOffset(), pageable.getPageSize());

        if (historicTasks.isEmpty()) {
            return Page.empty(pageable);
        }

        List<String> taskIds = historicTasks.stream().map(HistoricTaskInstance::getId).collect(Collectors.toList());
        Map<String, String> commentsMap = historyService.createHistoricVariableInstanceQuery()
                .taskIdIn(taskIds.toArray(new String[0]))
                .variableName("approvalComment")
                .list()
                .stream()
                .collect(Collectors.toMap(HistoricVariableInstance::getTaskId, v -> (String) v.getValue()));

        Set<String> processInstanceIds = historicTasks.stream().map(HistoricTaskInstance::getProcessInstanceId).collect(Collectors.toSet());
        Map<String, Map<String, Object>> processVariablesMap = new HashMap<>();
        if (!processInstanceIds.isEmpty()) {
            historyService.createHistoricVariableInstanceQuery()
                    .processInstanceIdIn(processInstanceIds.toArray(new String[0]))
                    .list()
                    .forEach(var -> processVariablesMap
                            .computeIfAbsent(var.getProcessInstanceId(), k -> new HashMap<>())
                            .put(var.getName(), var.getValue()));
        }

        List<CompletedTaskDto> dtos = historicTasks.stream()
                .map(task -> {
                    Map<String, Object> pVars = processVariablesMap.getOrDefault(task.getProcessInstanceId(), Collections.emptyMap());

                    // --- 【核心修改】优先从 taskOutcome 获取决策，并提供更详细的决策信息 ---
                    String decision = "UNKNOWN";
                    Object taskOutcomeObj = pVars.get("taskOutcome");
                    if (taskOutcomeObj instanceof String) {
                        String outcome = (String) taskOutcomeObj;
                        decision = switch (outcome) {
                            case "approved" -> "APPROVED";
                            case "rejected" -> "REJECTED";
                            case "returnToInitiator" -> "RETURN_TO_INITIATOR";
                            case "returnToPrevious" -> "RETURN_TO_PREVIOUS";
                            default -> outcome.toUpperCase();
                        };
                    } else {
                        // 兼容旧流程
                        Object approvedVar = pVars.get("approved");
                        if (approvedVar instanceof Boolean) {
                            decision = (Boolean) approvedVar ? "APPROVED" : "REJECTED";
                        }
                    }
                    // --- 【修改结束】 ---

                    return CompletedTaskDto.builder()
                            .camundaTaskId(task.getId())
                            .stepName(task.getName())
                            .startTime(task.getStartTime())
                            .endTime(task.getEndTime())
                            .durationInMillis(task.getDurationInMillis())
                            .formSubmissionId((Long) pVars.get("formSubmissionId"))
                            .formName((String) pVars.get("formName"))
                            .submitterName((String) pVars.get("submitterName"))
                            .decision(decision)
                            .comment(commentsMap.get(task.getId()))
                            .build();
                })
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, total);
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
            Long submissionId = ((Number) formSubmissionIdObj).longValue();
            dto.setFormSubmissionId(submissionId);

            // --- 【阶段一核心修改】根据 submissionId 查找并设置 formDefinitionId ---
            formSubmissionRepository.findById(submissionId).ifPresent(submission -> {
                if (submission.getFormDefinition() != null) {
                    dto.setFormDefinitionId(submission.getFormDefinition().getId());
                }
            });
            // --- 【修改结束】 ---
        }

        dto.setSubmitterName((String) variables.getOrDefault("submitterName", "未知"));
        dto.setFormName((String) variables.getOrDefault("formName", "未知表单"));

        List<String> decisions = new ArrayList<>();
        try {
            BpmnModelInstance modelInstance = repositoryService.getBpmnModelInstance(camundaTask.getProcessDefinitionId());
            FlowNode taskNode = modelInstance.getModelElementById(camundaTask.getTaskDefinitionKey());

            Collection<SequenceFlow> decisionFlows = findDecisionFlows(taskNode, 0);

            Pattern pattern = Pattern.compile("\\$\\{taskOutcome\\s*==\\s*'([^']*)'\\}");

            for (SequenceFlow flow : decisionFlows) {
                ConditionExpression condition = flow.getConditionExpression();
                if (condition != null && condition.getTextContent() != null) {
                    Matcher matcher = pattern.matcher(condition.getTextContent().trim());
                    if (matcher.matches()) {
                        String outcome = matcher.group(1);
                        String decision = outcome.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase();
                        decisions.add(decision);
                    }
                }
            }

            if (decisions.isEmpty()) {
                log.warn("任务 '{}' (定义KEY: {}) 未找到基于 'taskOutcome' 的条件分支, 将提供默认操作。",
                        camundaTask.getId(), camundaTask.getTaskDefinitionKey());
                decisions.addAll(List.of("APPROVED", "REJECTED"));
            }

        } catch (Exception e) {
            log.error("解析任务 {} 的可用决策时发生错误, 将提供默认操作。", camundaTask.getId(), e);
            decisions.addAll(List.of("APPROVED", "REJECTED"));
        }
        dto.setAvailableDecisions(decisions);

        return dto;
    }

    /**
     * 【核心新增】递归辅助方法，用于查找流程中的决策点。
     * 它会沿着单一、无条件的路径前进，直到找到一个排他网关或一个有多个条件分支的节点。
     * @param currentNode 当前开始查找的节点
     * @param depth 递归深度，防止无限循环
     * @return 决策点的出口连线集合
     */
    private Collection<SequenceFlow> findDecisionFlows(FlowNode currentNode, int depth) {
        if (depth > 10) { // 防止无限循环的保护机制
            log.warn("查找决策点时递归深度超过10，已中止。节点ID: {}", currentNode.getId());
            return Collections.emptyList();
        }

        Collection<SequenceFlow> outgoingFlows = currentNode.getOutgoing();

        // 情况1: 当前节点本身就是决策点 (有多个出口，或只有一个但带条件)
        if (outgoingFlows.size() > 1 || (outgoingFlows.size() == 1 && outgoingFlows.iterator().next().getConditionExpression() != null)) {
            return outgoingFlows;
        }

        // 情况2: 当前节点有一个无条件的出口
        if (outgoingFlows.size() == 1) {
            SequenceFlow singleFlow = outgoingFlows.iterator().next();
            FlowNode targetNode = singleFlow.getTarget();

            // 目标是排他网关，这是最常见的决策点
            if (targetNode instanceof ExclusiveGateway) {
                return targetNode.getOutgoing();
            }

            // 目标是其他类型的节点，则继续递归查找
            if (targetNode != null) {
                return findDecisionFlows(targetNode, depth + 1);
            }
        }

        // 情况3: 找不到决策点（例如，流程在此处结束）
        return Collections.emptyList();
    }
    // --- 【核心重构】结束 ---

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
                .orderByTenantId().asc()
                .list().stream()
                .collect(Collectors.toMap(
                        HistoricVariableInstance::getName,
                        HistoricVariableInstance::getValue,
                        (oldValue, newValue) -> newValue
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

    @Transactional(readOnly = true)
    public BpmnXmlDto getWorkflowDiagram(Long submissionId) {
        FormSubmission submission = formSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("未找到提交记录 ID: " + submissionId));

        WorkflowInstance instance = Optional.ofNullable(submission.getWorkflowInstance())
                .orElseThrow(() -> new ResourceNotFoundException("该申请未关联任何工作流实例"));

        WorkflowTemplate template = instance.getTemplate();
        if (template == null || !StringUtils.hasText(template.getBpmnXml())) {
            throw new ResourceNotFoundException("未找到该工作流实例关联的流程图定义");
        }

        return new BpmnXmlDto(template.getBpmnXml());
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

    // --- 【核心新增】获取设计器可用的 Spring Bean ---
    @Transactional(readOnly = true)
    public List<DelegateInfoDTO> getAvailableBeans(String type) {
        List<DelegateInfoDTO> beans = new ArrayList<>();

        if (type == null || "delegate".equalsIgnoreCase(type)) {
            Map<String, JavaDelegate> delegateBeans = applicationContext.getBeansOfType(JavaDelegate.class);
            delegateBeans.forEach((name, bean) ->
                    beans.add(new DelegateInfoDTO(name, "JavaDelegate", "这是一个Java Delegate Bean"))
            );
        }

        if (type == null || "listener".equalsIgnoreCase(type)) {
            Map<String, TaskListener> listenerBeans = applicationContext.getBeansOfType(TaskListener.class);
            listenerBeans.forEach((name, bean) ->
                    beans.add(new DelegateInfoDTO(name, "TaskListener", "这是一个任务监听器 Bean"))
            );
        }

        return beans.stream()
                .sorted(Comparator.comparing(DelegateInfoDTO::getName))
                .collect(Collectors.toList());
    }
}