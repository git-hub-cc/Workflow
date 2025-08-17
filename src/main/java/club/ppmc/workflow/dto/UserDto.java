package club.ppmc.workflow.dto;

import lombok.Data;

import java.util.List;

/**
 * @author cc
 * @description 用户信息的数据传输对象
 */
@Data
public class UserDto {
    private String id;
    private String name;
    private String managerId;
    private String department;

    // --- 【新增与修改】 ---
    private String status;
    private List<String> roleNames; // 角色名称列表
    private List<String> groupNames; // 用户组名称列表
}