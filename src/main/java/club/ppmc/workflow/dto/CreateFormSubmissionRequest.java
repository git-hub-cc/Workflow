package club.ppmc.workflow.dto;

import lombok.Data;
import java.util.List;

/**
 * @author 你的名字
 * @description 创建表单提交数据的请求 DTO
 */
@Data
public class CreateFormSubmissionRequest {
    private String dataJson;
    // --- 【新增】 ---
    private List<Long> attachmentIds; // 关联的文件附件ID列表
}