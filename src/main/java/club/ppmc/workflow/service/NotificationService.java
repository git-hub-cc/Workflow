package club.ppmc.workflow.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    /**
     * 异步发送新任务通知邮件
     * @param toEmail 收件人邮箱
     * @param taskName 任务名称
     * @param formName 关联的表单名称
     * @param submitterName 提交人
     */
    @Async // 异步执行，避免阻塞主流程（如启动工作流）
    public void sendNewTaskNotification(String toEmail, String taskName, String formName, String submitterName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("【工作流通知】您有一个新的待办任务");

            String text = String.format(
                    "您好，\n\n您有一个新的待办任务需要处理。\n\n" +
                            "任务名称: %s\n" +
                            "相关申请: %s\n" +
                            "发起人: %s\n\n" +
                            "请及时登录系统进行处理。\n\n" +
                            "此邮件为系统自动发送，请勿回复。",
                    taskName, formName, submitterName
            );
            message.setText(text);

            mailSender.send(message);
            log.info("成功发送新任务通知邮件至: {}", toEmail);
        } catch (Exception e) {
            // 捕获所有异常，防止异步线程因异常而终止
            log.error("发送邮件至 {} 失败: ", toEmail, e);
        }
    }
}