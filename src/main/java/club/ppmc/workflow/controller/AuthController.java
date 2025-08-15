package club.ppmc.workflow.controller;

import club.ppmc.workflow.dto.AuthRequest;
import club.ppmc.workflow.dto.AuthResponse;
import club.ppmc.workflow.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.authenticate(request));
    }
}