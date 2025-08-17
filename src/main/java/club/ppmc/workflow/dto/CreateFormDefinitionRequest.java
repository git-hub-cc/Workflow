package club.ppmc.workflow.dto;

import lombok.Data;

/**
 * @author cc
 * @description 创建新表单定义的请求 DTO
 */
@Data
public class CreateFormDefinitionRequest {
    private String name;
    private String schemaJson;
}