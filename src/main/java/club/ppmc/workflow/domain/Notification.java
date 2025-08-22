package club.ppmc.workflow.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * @author cc
 * @description 【新增】应用内通知实体
 */
@Entity
@Getter
@Setter
@Table(name = "app_notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 接收通知的用户
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 通知标题
     */
    @Column(nullable = false)
    private String title;

    /**
     * 通知内容
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String content;

    /**
     * 通知类型 (可选，用于前端显示不同图标等)
     */
    private String type;

    /**
     * 点击通知后跳转的链接
     */
    private String link;

    /**
     * 是否已读
     */
    @Column(nullable = false)
    private boolean isRead = false;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}