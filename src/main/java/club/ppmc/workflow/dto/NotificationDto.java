package club.ppmc.workflow.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author cc
 * @description 【新增】应用内通知的数据传输对象
 */
@Data
@Builder
public class NotificationDto {
    private Long id;
    private String title;
    private String content;
    private String type;
    private String link;
    private boolean isRead;
    private LocalDateTime createdAt;
}