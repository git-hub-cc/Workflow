package club.ppmc.workflow.controller;

import club.ppmc.workflow.dto.AuthRequest;
import club.ppmc.workflow.dto.AuthResponse;
import club.ppmc.workflow.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * @author 你的名字
 * @description 处理用户认证的控制器
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    /**
     * API: 用户登录
     * @param request 包含用户ID和密码
     * @return 包含JWT和用户信息的响应
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        try {
            AuthResponse response = authService.authenticate(request);
            return ResponseEntity.ok(response);
        } catch (CredentialsExpiredException e) {
            // 【新增】捕获凭证过期异常，向前段返回特定状态码或消息
            return ResponseEntity.status(499) // 自定义状态码
                    .body(Map.of("error", "PASSWORD_CHANGE_REQUIRED", "message", e.getMessage()));
        }
        // 其他认证异常（如密码错误、用户不存在）会由全局异常处理器或Spring Security默认处理为401/403
    }
}