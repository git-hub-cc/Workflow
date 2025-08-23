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
    private List<Long> attachmentIds; // 重新提交时更新的附件ID列表

    // --- 【核心新增】用于准备阶段的决策 ---
    /**
     * 【新增】准备阶段的决策结果 (例如: 'proceed', 'staging', 'terminate')
     * 此字段独立于通用的 'decision' 字段，专门用于处理第一个任务节点后的分支。
     */
    private String preparationOutcome;


    public enum Decision {
        APPROVED,
        REJECTED,
        /**
         * 打回至发起人
         */
        RETURN_TO_INITIATOR,
        /**
         * 打回至上一节点
         */
        RETURN_TO_PREVIOUS
    }
}