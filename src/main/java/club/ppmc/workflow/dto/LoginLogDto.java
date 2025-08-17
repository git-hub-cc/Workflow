package club.ppmc.workflow.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author cc
 * @description 登录日志数据传输对象
 */
@Data
public class LoginLogDto {
    private Long id;
    private String userId;
    private LocalDateTime loginTime;
    private String ipAddress;
    private String userAgent;
    private String status;
    private String failureReason;
}