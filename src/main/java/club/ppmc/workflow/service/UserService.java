package club.ppmc.workflow.service;

import club.ppmc.workflow.aop.LogOperation;
import club.ppmc.workflow.domain.User;
import club.ppmc.workflow.dto.UserPickerDto;
import club.ppmc.workflow.dto.UserProfileDto;
import club.ppmc.workflow.exception.ResourceNotFoundException;
import club.ppmc.workflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author cc
 * @description 处理用户相关业务逻辑的服务
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SystemSettingService systemSettingService; // 注入系统设置服务

    /**
     * 修改用户密码
     * @param userId 用户ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     */
    @LogOperation(module = "个人中心", action = "修改个人密码", targetIdExpression = "#userId")
    public void changePassword(String userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("未找到用户: " + userId));

        // 校验旧密码是否正确
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            // 【核心修改】抛出带有用户友好信息的异常，将由全局异常处理器捕获并返回 400 Bad Request
            throw new IllegalArgumentException("旧密码不正确");
        }

        // 校验新密码复杂度（示例，真实项目应更复杂）
        if (newPassword == null || newPassword.length() < 6) {
            // 【核心修改】抛出带有用户友好信息的异常
            throw new IllegalArgumentException("新密码长度不能少于6位");
        }

        // 更新密码，并清除“强制修改密码”标志
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangeRequired(false);
        userRepository.save(user);
    }

    /**
     * (管理员)重置用户密码
     * @param userId 用户ID
     */
    @LogOperation(module = "用户管理", action = "重置用户密码", targetIdExpression = "#userId")
    public void resetPassword(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("未找到用户: " + userId));

        // --- 【核心修改】从系统设置中读取默认密码，如果未设置则使用硬编码的 "password" ---
        String defaultPassword = systemSettingService.getSettingsMap()
                .getOrDefault("DEFAULT_PASSWORD", "password");

        user.setPassword(passwordEncoder.encode(defaultPassword));
        user.setPasswordChangeRequired(true);
        userRepository.save(user);
    }

    /**
     * 【新增】获取用户个人资料
     * @param userId 用户ID
     * @return UserProfileDto
     */
    @Transactional(readOnly = true)
    public UserProfileDto getUserProfile(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("未找到用户: " + userId));
        UserProfileDto dto = new UserProfileDto();
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setPhoneNumber(user.getPhoneNumber());
        return dto;
    }

    /**
     * 【新增】更新用户个人资料
     * @param userId 用户ID
     * @param profileDto 包含新资料的 DTO
     */
    @LogOperation(module = "个人中心", action = "更新个人资料", targetIdExpression = "#userId")
    public UserProfileDto updateUserProfile(String userId, UserProfileDto profileDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("未找到用户: " + userId));

        user.setName(profileDto.getName());
        user.setEmail(profileDto.getEmail());
        user.setPhoneNumber(profileDto.getPhoneNumber());

        User savedUser = userRepository.save(user);

        UserProfileDto responseDto = new UserProfileDto();
        responseDto.setName(savedUser.getName());
        responseDto.setEmail(savedUser.getEmail());
        responseDto.setPhoneNumber(savedUser.getPhoneNumber());
        return responseDto;
    }


    /**
     * 【新增】为前端选择器搜索用户
     *
     * @param keyword 搜索关键词
     * @return 匹配的用户列表 (UserPickerDto)
     */
    @Transactional(readOnly = true)
    public List<UserPickerDto> searchUsersForPicker(String keyword) {
        // 如果关键词为空，不返回任何结果，强制前端必须输入才能搜索
        if (!StringUtils.hasText(keyword)) {
            return Collections.emptyList();
        }

        // 限制最多返回20条结果，防止性能问题和信息过度暴露
        PageRequest pageRequest = PageRequest.of(0, 20);
        // 【FIX】从 Page<User> 对象中获取内容列表
        Page<User> userPage = userRepository.findByIdContainingIgnoreCaseOrNameContainingIgnoreCaseOrderByNameAsc(keyword, keyword, pageRequest);
        List<User> users = userPage.getContent();


        return users.stream()
                .map(this::toUserPickerDto)
                .collect(Collectors.toList());
    }

    /**
     * 将 User 实体转换为 UserPickerDto
     */
    private UserPickerDto toUserPickerDto(User user) {
        UserPickerDto dto = new UserPickerDto();
        dto.setId(user.getId());
        dto.setName(user.getName());
        return dto;
    }

}