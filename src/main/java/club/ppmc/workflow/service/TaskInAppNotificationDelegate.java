package club.ppmc.workflow.service;

import club.ppmc.workflow.domain.User;
import club.ppmc.workflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.model.bpmn.instance.BoundaryEvent;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.bpmn.instance.UserTask;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 【新增】一个专门用于发送任务超时应用内通知的 Delegate。
 * 这个 Delegate 不会升级或重新分配任务，仅发送通知。
 */
@Component("taskInAppNotificationDelegate")
@RequiredArgsConstructor
@Slf4j
public class TaskInAppNotificationDelegate implements JavaDelegate {

    private final TaskService taskService;
    private final NotificationService notificationService;

    @Override
    @Transactional(readOnly = true) // 只读事务，因为它只查询和发送通知，不修改流程状态
    public void execute(DelegateExecution execution) throws Exception {
        // --- 【核心修复】开始 ---
        // 1. 获取当前执行的BPMN元素（即边界事件后面的ServiceTask），并强制转换为 FlowNode
        FlowNode serviceTaskNode = (FlowNode) execution.getBpmnModelElementInstance();

        // 2. 从 ServiceTask 获取唯一的输入流
        SequenceFlow incomingFlow = serviceTaskNode.getIncoming().iterator().next();

        // 3. 输入流的源头就是边界定时器事件
        BoundaryEvent boundaryEvent = (BoundaryEvent) incomingFlow.getSource();

        // 4. 从边界事件获取它所附加的用户任务
        UserTask userTask = (UserTask) boundaryEvent.getAttachedTo();
        String userTaskDefinitionKey = userTask.getId();

        // 5. 使用正确的流程实例ID和用户任务的定义Key来精确查找待办任务
        Task task = taskService.createTaskQuery()
                .processInstanceId(execution.getProcessInstanceId())
                .taskDefinitionKey(userTaskDefinitionKey)
                .active() // 确保任务是激活状态
                .singleResult();
        // --- 【核心修复】结束 ---

        if (task == null) {
            log.warn("任务超时通知：无法找到与边界事件关联的待办任务（可能已被处理）。流程实例ID: {}, 用户任务定义Key: {}", execution.getProcessInstanceId(), userTaskDefinitionKey);
            return;
        }

        String assigneeId = task.getAssignee();
        if (assigneeId == null) {
            log.warn("任务超时通知：任务 {} 没有办理人，无法发送通知。", task.getId());
            return;
        }

        String formName = (String) execution.getVariable("formName");
        String submitterName = (String) execution.getVariable("submitterName");

        String title = "您有一个待办任务即将超时";
        String content = String.format("来自 %s 的 %s 申请已等待您处理一段时间，请尽快处理。", submitterName, formName);
        String link = "/tasks/" + task.getId();

        notificationService.createInAppNotification(assigneeId, title, content, "task_reminder", link);

        log.info("成功为任务 {} (办理人: {}) 发送了超时应用内通知。", task.getId(), assigneeId);
    }
}