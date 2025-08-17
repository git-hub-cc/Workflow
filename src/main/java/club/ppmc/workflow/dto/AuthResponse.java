package club.ppmc.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String token;
    private UserAuthInfo user;

    /**
     * 专门用于认证响应的用户信息 DTO
     * 保持与旧版 UserDto 兼容，同时可以扩展
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserAuthInfo {
        private String id;
        private String name;
        private String role; // 仍然保留一个主角色，用于前端快速判断权限
    }
}