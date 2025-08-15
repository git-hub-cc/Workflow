package club.ppmc.workflow.listener;

import club.ppmc.workflow.domain.User;
import club.ppmc.workflow.domain.WorkflowInstance;
import club.ppmc.workflow.repository.UserRepository;
import club.ppmc.workflow.repository.WorkflowInstanceRepository;
import club.ppmc.workflow.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("taskAssignmentListener") // 为 Bean 命名，以便在 BPMN 中通过 Delegate Expression 使用
@RequiredArgsConstructor
@Slf4j
public class TaskAssignmentListener implements TaskListener {

    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final WorkflowInstanceRepository instanceRepository;

    @Override
    public void notify(DelegateTask delegateTask) {
        // 只在任务创建 (create) 事件时发送通知
        if (!EVENTNAME_CREATE.equals(delegateTask.getEventName())) {
            return;
        }

        String assigneeId = delegateTask.getAssignee();
        if (assigneeId == null || assigneeId.isEmpty()) {
            log.warn("任务 {} 没有分配办理人，无法发送通知。", delegateTask.getId());
            return;
        }

        // 在真实项目中，您应该从用户实体中获取邮箱。这里为了演示，我们假设用户ID就是邮箱前缀。
        // 例如，从 userRepository.findById(assigneeId) 中获取 user.getEmail()
        String toEmail = assigneeId + "@example.com"; // 【注意】请根据您的用户模型修改此处的邮箱获取逻辑
        log.info("准备为任务 {} 向 {} ({}) 发送通知邮件。", delegateTask.getId(), assigneeId, toEmail);

        String taskName = delegateTask.getName();
        String processInstanceId = delegateTask.getProcessInstanceId();

        Optional<WorkflowInstance> instanceOpt = instanceRepository.findByProcessInstanceId(processInstanceId);
        if (instanceOpt.isPresent()) {
            WorkflowInstance instance = instanceOpt.get();
            String formName = instance.getTemplate().getFormDefinition().getName();
            String submitterId = instance.getFormSubmission().getSubmitterId();

            String submitterName = userRepository.findById(submitterId)
                    .map(User::getName)
                    .orElse("未知");

            notificationService.sendNewTaskNotification(toEmail, taskName, formName, submitterName);
        } else {
            log.error("无法找到流程实例 {} 对应的本地工作流实例，邮件通知发送失败。", processInstanceId);
        }
    }
}