package club.ppmc.workflow.dto;

import lombok.Data;
import java.time.LocalDateTime;

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
}