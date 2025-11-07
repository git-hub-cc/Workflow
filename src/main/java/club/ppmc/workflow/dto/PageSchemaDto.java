package club.ppmc.workflow.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * @author cc
 * @description 【新增】PageSchema 的数据传输对象 (DTO)
 */
@Data
public class PageSchemaDto {
    private Long id;
    private String pageKey;
    private String name;
    private String schemaJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}