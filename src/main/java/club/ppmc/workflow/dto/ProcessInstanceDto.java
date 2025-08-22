package club.ppmc.workflow.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * @author cc
 * @description 流程实例的数据传输对象（管理端使用）
 */
@Data
@Builder
public class ProcessInstanceDto {
    private String processInstanceId;
    private String businessKey; // 对应 FormSubmission ID
    private String processDefinitionName;
    private int version;
    private LocalDateTime startTime;
    private String startUserId;
    private String startUserName;
    private String currentActivityName;
    private boolean suspended; // 是否挂起

    // --- 【核心新增】 ---

    /**
     * 流程持续时间（毫秒），仅对已结束的实例有效
     */
    private Long durationInMillis;

    /**
     * 流程实例当前是否存在技术异常 (Incident)
     */
    private boolean hasIncident;

    /**
     * 实例的最终状态 (RUNNING, COMPLETED, TERMINATED)
     */
    private String state;

}