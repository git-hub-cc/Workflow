package club.ppmc.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author 你的名字
 * @description 工作流历史活动节点的 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoryActivityDto {
    private String activityId;
    private String activityName;
    private String activityType; // 例如: userTask, serviceTask, startEvent
    private String assigneeId;
    private String assigneeName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationInMillis;
    private String decision; // APPROVED, REJECTED
    private String comment;
}