package club.ppmc.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author cc
 * @description 【新增】用于在表单定义中描述字段配置的 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FormFieldConfigDto {
    private String id;
    private String label;
    private String type;
}