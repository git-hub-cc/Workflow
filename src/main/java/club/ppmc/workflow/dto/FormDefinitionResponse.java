package club.ppmc.workflow.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List; // 【新增】

/**
 * @author cc
 * @description 表单定义的响应 DTO
 */
@Data
public class FormDefinitionResponse {
    private Long id;
    private String name;
    private String schemaJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 【新增】用于前端动态生成列表页的元数据
    private List<FormFieldConfigDto> filterableFields;
    private List<FormFieldConfigDto> listDisplayFields;
}