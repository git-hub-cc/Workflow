package club.ppmc.workflow.dto;

import lombok.Data;

/**
 * @author cc
 * @description 修改密码的请求 DTO
 */
@Data
public class ChangePasswordRequest {
    private String oldPassword;
    private String newPassword;
}