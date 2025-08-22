package club.ppmc.workflow.service;

import club.ppmc.workflow.domain.User;
import club.ppmc.workflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.engine.task.Task;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * @author cc
 * @description 【新增】处理任务超时的 Camunda JavaDelegate
 * <p>
 * 当一个任务上的边界定时器事件被触发时，此 Delegate 将被调用。
 * 它的核心逻辑是：
 * 1. 找到当前任务的办理人。
 * 2. 查找该办理人的上级经理。
 * 3. 如果找到上级经理，则将任务重新分配给该经理（任务升级）。
 * 4. 无论是否找到上级，都发送一封超时提醒邮件。
 */
@Component("taskEscalationDelegate")
@RequiredArgsConstructor
@Slf4j
public class TaskEscalationDelegate implements JavaDelegate {

    private final TaskService taskService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public void execute(DelegateExecution execution) throws Exception {
        String processInstanceId = execution.getProcessInstanceId();
        // 边界事件触发时，它附加的活动（UserTask）的ID存储在 activityInstanceId 中
        String activityInstanceId = execution.getActivityInstanceId();

        log.info("任务超时处理 [TaskEscalationDelegate] for process instance: {}, activity instance: {}", processInstanceId, activityInstanceId);

        // 根据流程实例ID和活动ID找到对应的待办任务
        Task task = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .activityInstanceIdIn(activityInstanceId)
                .singleResult();

        if (task == null) {
            log.warn("无法找到与活动实例 {} 关联的待办任务，可能已被处理。跳过升级。", activityInstanceId);
            return;
        }

        String currentAssigneeId = task.getAssignee();
        if (currentAssigneeId == null) {
            log.warn("任务 {} 没有办理人，无法执行升级。", task.getId());
            return;
        }

        Optional<User> userOpt = userRepository.findById(currentAssigneeId);
        if (userOpt.isEmpty()) {
            log.error("数据不一致：在数据库中找不到任务办理人 {}", currentAssigneeId);
            return;
        }

        User currentUser = userOpt.get();
        User manager = currentUser.getManager();

        if (manager != null) {
            String managerId = manager.getId();
            taskService.setAssignee(task.getId(), managerId);
            log.info("任务 {} 已从 {} 升级给其上级 {}", task.getId(), currentAssigneeId, managerId);

            // 【通知上级】
            // 在真实项目中，应从用户实体获取邮箱
            String managerEmail = managerId + "@example.com";
            notificationService.sendTaskOverdueNotification(
                    managerEmail,
                    manager.getName(),
                    task.getName(),
                    (String) execution.getVariable("formName"),
                    currentUser.getName()
            );

        } else {
            log.warn("用户 {} 没有上级经理，任务 {} 无法升级。", currentAssigneeId, task.getId());
            // 【通知原办理人】
            String assigneeEmail = currentAssigneeId + "@example.com";
            notificationService.sendTaskOverdueNotification(
                    assigneeEmail,
                    currentUser.getName(),
                    task.getName(),
                    (String) execution.getVariable("formName"),
                    (String) execution.getVariable("submitterName")
            );
        }
    }
}