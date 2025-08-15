package club.ppmc.workflow.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * @author 你的名字
 * @description 表单提交数据的响应 DTO
 */
@Data
public class FormSubmissionResponse {
    private Long id;
    private Long formDefinitionId;
    private String formName; // 表单名称
    private String dataJson;
    private LocalDateTime createdAt;
    private String workflowStatus;
    private String submitterName; // 提交人姓名
}