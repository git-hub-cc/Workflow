package club.ppmc.workflow.controller;

import club.ppmc.workflow.dto.ChangePasswordRequest;
import club.ppmc.workflow.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

/**
 * @author 你的名字
 * @description 处理用户个人信息相关的控制器
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;

    /**
     * API: 当前登录用户修改自己的密码
     * @param request 包含旧密码和新密码
     * @param principal Spring Security 自动注入的当前用户信息
     * @return 成功或失败的响应
     */
    @PostMapping("/me/change-password")
    public ResponseEntity<Void> changePassword(@RequestBody ChangePasswordRequest request, Principal principal) {
        String userId = principal.getName();
        userService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
        return ResponseEntity.ok().build();
    }
}