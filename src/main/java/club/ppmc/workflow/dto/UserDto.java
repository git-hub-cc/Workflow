package club.ppmc.workflow.dto;

import lombok.Data;

/**
 * @author 你的名字
 * @description 用户信息的数据传输对象
 */
@Data
public class UserDto {
    private String id;
    private String name;
    private String role; // 角色字段
    private String managerId; // 新增：直属上级ID
}