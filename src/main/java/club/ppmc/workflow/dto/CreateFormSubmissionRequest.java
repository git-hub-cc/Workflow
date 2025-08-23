package club.ppmc.workflow.dto;

import lombok.Data;
import java.util.List;

/**
 * @author cc
 * @description 创建表单提交数据的请求 DTO
 */
@Data
public class CreateFormSubmissionRequest {
    private String dataJson;
    // --- 【新增】 ---
    private List<Long> attachmentIds; // 关联的文件附件ID列表

    /**
     * 【新增】初始动作标识, 用于表单首次提交时决定流程的第一个走向。
     * 可选值为: 'proceed' (继续), 'stage' (暂存), 'terminate' (终止)。
     */
    private String initialAction;
}