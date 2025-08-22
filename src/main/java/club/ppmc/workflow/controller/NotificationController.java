package club.ppmc.workflow.controller;

import club.ppmc.workflow.dto.NotificationDto;
import club.ppmc.workflow.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * @author cc
 * @description 【新增】应用内通知相关的 RESTful API 控制器
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * API: 获取当前用户的通知列表 (分页)
     */
    @GetMapping
    public ResponseEntity<Page<NotificationDto>> getMyNotifications(Principal principal, Pageable pageable) {
        String userId = principal.getName();
        return ResponseEntity.ok(notificationService.getMyNotifications(userId, pageable));
    }

    /**
     * API: 获取当前用户的未读通知数量
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(Principal principal) {
        String userId = principal.getName();
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * API: 将所有通知标记为已读
     */
    @PostMapping("/mark-all-as-read")
    public ResponseEntity<Void> markAllAsRead(Principal principal) {
        String userId = principal.getName();
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }

    /**
     * API: 将指定的通知标记为已读
     */
    @PostMapping("/mark-as-read")
    public ResponseEntity<Void> markAsRead(Principal principal, @RequestBody List<Long> ids) {
        String userId = principal.getName();
        notificationService.markAsRead(userId, ids);
        return ResponseEntity.ok().build();
    }
}