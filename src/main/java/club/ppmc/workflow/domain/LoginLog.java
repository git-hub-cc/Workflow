package club.ppmc.workflow.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * @author cc
 * @description 登录日志实体
 */
@Entity
@Getter
@Setter
@Table(name = "login_log")
public class LoginLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime loginTime;

    private String ipAddress;

    @Column(length = 512)
    private String userAgent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoginStatus status;

    @Column(length = 512)
    private String failureReason;

    public enum LoginStatus {
        SUCCESS,
        FAILURE
    }

    @PrePersist
    protected void onCreate() {
        loginTime = LocalDateTime.now();
    }
}