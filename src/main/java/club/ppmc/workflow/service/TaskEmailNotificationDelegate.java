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
 * 【新增】一个专门用于发送任务超时邮件通知的 Delegate。
 * 这个 Delegate 不会升级或重新分配任务，仅发送邮件。
 */
@Component("taskEmailNotificationDelegate")
@RequiredArgsConstructor
@Slf4j
public class TaskEmailNotificationDelegate implements JavaDelegate {

    private final TaskService taskService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
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
                .active()
                .singleResult();
        // --- 【核心修复】结束 ---

        if (task == null) {
            log.warn("任务超时邮件：无法找到与边界事件关联的待办任务（可能已被处理）。流程实例ID: {}, 用户任务定义Key: {}", execution.getProcessInstanceId(), userTaskDefinitionKey);
            return;
        }

        String assigneeId = task.getAssignee();
        User assignee = userRepository.findById(assigneeId).orElse(null);

        if (assignee == null) {
            log.warn("任务超时邮件：任务 {} 的办理人 {} 在数据库中不存在，无法发送邮件。", task.getId(), assigneeId);
            return;
        }

        String formName = (String) execution.getVariable("formName");
        String submitterName = (String) execution.getVariable("submitterName");
        // 模拟邮箱地址，真实项目中应从 assignee.getEmail() 获取
        String assigneeEmail = assignee.getId() + "@example.com";

        notificationService.sendTaskOverdueNotification(
                assigneeEmail,
                assignee.getName(),
                task.getName(),
                formName,
                // 对于非升级邮件，原办理人就是接收人自己
                assignee.getName()
        );

        log.info("成功为任务 {} (办理人: {}) 发送了超时邮件通知。", task.getId(), assigneeId);
    }
}