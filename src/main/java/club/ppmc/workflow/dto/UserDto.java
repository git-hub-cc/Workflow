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

    // --- 【核心修改】将 department 字符串改为 departmentId 和 departmentName ---
    private Long departmentId;
    private String departmentName;
    // --- 【修改结束】 ---

    // --- 【新增】可编辑字段 ---
    private String email;
    private String phoneNumber;
    // --- 【新增结束】 ---

    private String status;
    private List<String> roleNames;
    private List<String> groupNames;
}