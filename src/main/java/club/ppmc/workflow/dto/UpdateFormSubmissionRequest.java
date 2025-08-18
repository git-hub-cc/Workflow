package club.ppmc.workflow.dto;

import lombok.Data;

import java.util.List;

/**
 * @author cc
 * @description 【新增】用于更新表单提交记录的请求 DTO
 */
@Data
public class UpdateFormSubmissionRequest {
    private String dataJson;
    private List<Long> attachmentIds;
}