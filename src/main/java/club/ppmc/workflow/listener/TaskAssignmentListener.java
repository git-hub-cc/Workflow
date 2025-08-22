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

        String taskName = delegateTask.getName();
        String processInstanceId = delegateTask.getProcessInstanceId();

        Optional<WorkflowInstance> instanceOpt = instanceRepository.findByProcessInstanceId(processInstanceId);
        if (instanceOpt.isPresent()) {
            WorkflowInstance instance = instanceOpt.get();
            String formName = instance.getTemplate().getFormDefinition().getName();
            String submitterId = instance.getFormSubmission().getSubmitterId();
            Long submissionId = instance.getFormSubmission().getId();

            String submitterName = userRepository.findById(submitterId)
                    .map(User::getName)
                    .orElse("未知");

            // --- 【核心修改】同时发送邮件和应用内通知 ---
            try {
                // 1. 发送邮件通知
                // 在真实项目中，您应该从用户实体中获取邮箱。这里为了演示，我们假设用户ID就是邮箱前缀。
                String toEmail = assigneeId + "@example.com";
                notificationService.sendNewTaskNotification(toEmail, taskName, formName, submitterName);

                // 2. 创建应用内通知
                String title = "您有新的待办任务";
                String content = String.format("来自 %s 的 %s 申请需要您处理。", submitterName, formName);
                String link = "/tasks/" + delegateTask.getId(); // 指向任务详情页
                notificationService.createInAppNotification(assigneeId, title, content, "task", link);

            } catch (Exception e) {
                log.error("为任务 {} 发送通知失败: ", delegateTask.getId(), e);
            }

        } else {
            log.error("无法找到流程实例 {} 对应的本地工作流实例，通知发送失败。", processInstanceId);
        }
    }
}