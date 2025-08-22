package club.ppmc.workflow.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * @author cc
 * @description 【新增】已办任务的数据传输对象
 */
@Data
@Builder
public class CompletedTaskDto {
    // 任务基本信息
    private String camundaTaskId;
    private String stepName;
    private Date startTime;
    private Date endTime;
    private Long durationInMillis;

    // 关联的业务信息
    private Long formSubmissionId;
    private String formName;
    private String submitterName;

    // 审批信息
    private String decision; // "APPROVED" or "REJECTED"
    private String comment;
}