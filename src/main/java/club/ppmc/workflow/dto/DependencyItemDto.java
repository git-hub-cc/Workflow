package club.ppmc.workflow.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author cc
 * @description 【新增】用于描述单个依赖项的 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // 仅序列化非空字段
public class DependencyItemDto {

    private DependencyType type;
    private String name;
    private Long id;
    private Long count; // 专用于 SUBMISSION 类型

    public enum DependencyType {
        WORKFLOW,
        MENU,
        SUBMISSION
    }
}