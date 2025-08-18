package club.ppmc.workflow.controller;

import club.ppmc.workflow.dto.AuthRequest;
import club.ppmc.workflow.dto.AuthResponse;
import club.ppmc.workflow.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


/**
 * @author cc
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
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        // 【核心修改】移除了 try-catch 块。
        // 所有认证相关的异常（密码错误、用户不存在、凭证过期等）
        // 现在将由 GlobalExceptionHandler 统一处理，以确保返回结构化的错误信息。
        AuthResponse response = authService.authenticate(request);
        return ResponseEntity.ok(response);
    }
}