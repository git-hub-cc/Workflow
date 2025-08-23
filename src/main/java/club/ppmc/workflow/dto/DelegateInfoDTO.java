package club.ppmc.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author cc
 * @description 【新增】用于向前端返回可用 Delegate 或 Listener 信息的 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DelegateInfoDTO {
    /**
     * Spring Bean 的名称, 例如 "findManagerDelegate"
     */
    private String name;

    /**
     * Bean 的类型, 例如 "JavaDelegate" 或 "TaskListener"
     */
    private String type;

    /**
     * Bean 的描述 (可选)
     */
    private String description;
}