package club.ppmc.workflow.service;

import club.ppmc.workflow.domain.Notification;
import club.ppmc.workflow.domain.User;
import club.ppmc.workflow.dto.NotificationDto;
import club.ppmc.workflow.exception.ResourceNotFoundException;
import club.ppmc.workflow.repository.NotificationRepository;
import club.ppmc.workflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final JavaMailSender mailSender;
    // --- 【新增】注入 Repository ---
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

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

    /**
     * 【新增】异步发送任务超时/升级通知邮件
     * @param toEmail 收件人邮箱
     * @param recipientName 收件人姓名
     * @param taskName 任务名称
     * @param formName 关联的表单名称
     * @param originalAssigneeName 原办理人姓名 (如果发生升级)
     */
    @Async
    public void sendTaskOverdueNotification(String toEmail, String recipientName, String taskName, String formName, String originalAssigneeName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("【工作流提醒】一个待办任务已超时");

            boolean isEscalation = !recipientName.equals(originalAssigneeName);
            String escalationText = isEscalation
                    ? String.format("原办理人 %s 未及时处理，该任务现已转交由您处理。\n\n", originalAssigneeName)
                    : "";

            String text = String.format(
                    "您好，%s，\n\n以下任务已超过预定处理时间。\n\n%s" +
                            "任务名称: %s\n" +
                            "相关申请: %s\n\n" +
                            "请尽快登录系统处理，以免影响业务进度。\n\n" +
                            "此邮件为系统自动发送，请勿回复。",
                    recipientName, escalationText, taskName, formName
            );
            message.setText(text);

            mailSender.send(message);
            log.info("成功发送任务超时通知邮件至: {}", toEmail);
        } catch (Exception e) {
            log.error("发送任务超时邮件至 {} 失败: ", toEmail, e);
        }
    }

    // --- 【新增】应用内通知相关方法 ---

    @Async
    @Transactional
    public void createInAppNotification(String userId, String title, String content, String type, String link) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("创建通知失败：未找到用户 " + userId));

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setLink(link);
        notificationRepository.save(notification);
        log.info("已为用户 {} 创建应用内通知: {}", userId, title);
    }

    @Transactional(readOnly = true)
    public Page<NotificationDto> getMyNotifications(String userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String userId) {
        return notificationRepository.countByUserIdAndIsRead(userId, false);
    }

    @Transactional
    public void markAllAsRead(String userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }

    @Transactional
    public void markAsRead(String userId, List<Long> ids) {
        notificationRepository.markAsReadByIds(ids, userId);
    }

    private NotificationDto toDto(Notification entity) {
        return NotificationDto.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .content(entity.getContent())
                .type(entity.getType())
                .link(entity.getLink())
                .isRead(entity.isRead())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}