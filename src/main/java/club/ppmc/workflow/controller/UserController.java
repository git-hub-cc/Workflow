package club.ppmc.workflow.controller;

import club.ppmc.workflow.dto.ChangePasswordRequest;
import club.ppmc.workflow.dto.UserPickerDto;
import club.ppmc.workflow.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * @author cc
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
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> changePassword(@RequestBody ChangePasswordRequest request, Principal principal) {
        String userId = principal.getName();
        userService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
        return ResponseEntity.ok().build();
    }


    /**
     * 【新增】API: 搜索用户以供前端选择器使用
     * <p>
     * 这是一个安全的、对所有登录用户开放的接口，用于在表单等场景中通过姓名或ID搜索用户。
     * 它不会返回全量用户列表，以防止信息泄露。
     *
     * @param keyword 搜索关键词（可匹配用户ID或姓名）
     * @return 匹配的用户列表 (简化版 UserPickerDto)
     */
    @GetMapping("/search-for-picker")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UserPickerDto>> searchUsersForPicker(@RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(userService.searchUsersForPicker(keyword));
    }

}