package club.ppmc.workflow.dto;

import lombok.Data;
import java.util.Date; // Camunda 的时间是 java.util.Date
import java.util.List;

/**
 * @author cc
 * @description 任务的数据传输对象
 */
@Data
public class TaskDto {
    private String camundaTaskId; // 使用 Camunda 的任务 ID
    private String stepName;
    private Date createdAt; // 任务创建时间

    // 关联的业务信息
    private Long formSubmissionId;
    private String formName;
    private String submitterName;

    // --- 【阶段一新增】 ---
    /**
     * 关联的表单定义ID，用于在“我的待办”中点击修改任务时，能够正确跳转到FormViewer
     */
    private Long formDefinitionId;
    // --- 【新增结束】 ---

    /**
     * 当前任务可用的操作决策列表
     * e.g., ["APPROVED", "REJECTED", "RETURN_TO_INITIATOR", "RETURN_TO_PREVIOUS"]
     */
    private List<String> availableDecisions;
}