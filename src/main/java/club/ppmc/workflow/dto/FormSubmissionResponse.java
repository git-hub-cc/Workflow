package club.ppmc.workflow.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author cc
 * @description 表单提交数据的响应 DTO
 */
@Data
public class FormSubmissionResponse {
    private Long id;
    private Long formDefinitionId;
    private String formName; // 表单名称
    private String dataJson;
    private LocalDateTime createdAt;
    private String workflowStatus; // 流程实例状态 (e.g., 审批中, 已通过)
    private String submitterName; // 提交人姓名
    // --- 【新增】 ---
    private List<FileAttachmentDto> attachments;
    // --- 【核心新增】用于表示申请业务状态的字段 ---
    private String submissionStatus; // 申请的业务状态 (e.g., DRAFT, PROCESSING)
}