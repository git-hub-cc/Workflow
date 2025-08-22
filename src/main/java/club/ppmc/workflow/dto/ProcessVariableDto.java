package club.ppmc.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author cc
 * @description 【新增】用于流程变量管理的 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessVariableDto {

    /**
     * 变量名
     */
    private String name;

    /**
     * 变量类型 (e.g., String, Boolean, Integer, Json)
     */
    private String type;

    /**
     * 变量值 (通常为序列化后的字符串)
     */
    private Object value;
}