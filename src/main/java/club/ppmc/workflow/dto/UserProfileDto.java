package club.ppmc.workflow.dto;

import lombok.Data;

/**
 * @author cc
 * @description 【新增】用于个人中心信息更新的 DTO
 */
@Data
public class UserProfileDto {
    private String name;
    private String email;
    private String phoneNumber;
}