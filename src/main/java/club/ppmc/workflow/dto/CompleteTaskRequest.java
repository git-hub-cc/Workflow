package club.ppmc.workflow.dto;

import lombok.Data;

/**
 * @author 你的名字
 * @description 完成任务的请求 DTO
 */
@Data
public class CompleteTaskRequest {
    private Decision decision;
    private String approvalComment; // 审批意见
    private String updatedFormData; // 【新增】用于接收被驳回后修改的表单数据

    public enum Decision {
        APPROVED,
        REJECTED
    }
}