package club.ppmc.workflow.dto;

import lombok.Data;
import java.util.List;

/**
 * @author cc
 * @description 完成任务的请求 DTO
 */
@Data
public class CompleteTaskRequest {
    private Decision decision;
    private String approvalComment; // 审批意见
    private String updatedFormData; // 用于接收被驳回后修改的表单数据
    // --- 【新增】 ---
    private List<Long> attachmentIds; // 重新提交时更新的附件ID列表

    public enum Decision {
        APPROVED,
        REJECTED
    }
}