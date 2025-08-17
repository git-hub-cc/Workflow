package club.ppmc.workflow.service;

import club.ppmc.workflow.domain.User;
import club.ppmc.workflow.exception.ResourceNotFoundException;
import club.ppmc.workflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author 你的名字
 * @description 处理用户相关业务逻辑的服务
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 修改用户密码
     * @param userId 用户ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     */
    public void changePassword(String userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("未找到用户: " + userId));

        // 校验旧密码是否正确
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("旧密码不正确");
        }

        // 校验新密码复杂度（示例，真实项目应更复杂）
        if (newPassword == null || newPassword.length() < 6) {
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
    public void resetPassword(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("未找到用户: " + userId));

        // 重置为默认密码 'password'，并设置强制修改标志
        user.setPassword(passwordEncoder.encode("password"));
        user.setPasswordChangeRequired(true);
        userRepository.save(user);
    }
}