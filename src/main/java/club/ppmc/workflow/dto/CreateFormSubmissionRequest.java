package club.ppmc.workflow.dto;

import lombok.Data;

/**
 * @author 你的名字
 * @description 创建表单提交数据的请求 DTO
 */
@Data
public class CreateFormSubmissionRequest {
    private String dataJson;
}