package club.ppmc.workflow.dto;

import lombok.Data;

/**
 * @author 你的名字
 * @description 完成任务的请求 DTO
 */
@Data
public class CompleteTaskRequest {
    private Decision decision;
    private String comment; // 审批意见（在第三阶段中，此字段未直接使用，但可保留用于扩展）

    public enum Decision {
        APPROVED,
        REJECTED
    }
}