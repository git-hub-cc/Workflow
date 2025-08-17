package club.ppmc.workflow.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author cc
 * @description 操作日志数据传输对象
 */
@Data
public class OperationLogDto {
    private Long id;
    private String operatorId;
    private String operatorName;
    private LocalDateTime operationTime;
    private String module;
    private String action;
    private String targetId;
    private String details;
    private String ipAddress;
}