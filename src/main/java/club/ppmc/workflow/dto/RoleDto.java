package club.ppmc.workflow.dto;

import lombok.Data;

/**
 * @author 你的名字
 * @description 角色数据传输对象
 */
@Data
public class RoleDto {
    private Long id;
    private String name;
    private String description;
}