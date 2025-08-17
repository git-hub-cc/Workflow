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
    // --- 【新增】 ---
    private boolean suspended; // 是否挂起
}