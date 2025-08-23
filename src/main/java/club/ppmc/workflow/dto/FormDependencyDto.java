package club.ppmc.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author cc
 * @description 【新增】用于返回表单依赖关系检查结果的 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormDependencyDto {
    private boolean canDelete;
    private List<DependencyItemDto> dependencies;
}