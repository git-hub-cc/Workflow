package club.ppmc.workflow.service;

import club.ppmc.workflow.domain.User;
import club.ppmc.workflow.domain.UserStatus;
import club.ppmc.workflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Spring Security 用于加载用户信息的服务实现
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * 根据用户名（在我们的应用中是 userId）加载用户信息
     * @param username 用户的唯一标识 (userId)
     * @return UserDetails 对象
     * @throws UsernameNotFoundException 如果用户未找到
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findById(username)
                .orElseThrow(() -> new UsernameNotFoundException("未找到用户: " + username));

        // --- 【新增】检查用户状态 ---
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new DisabledException("用户账号已被禁用");
        }
        if (user.getStatus() == UserStatus.LOCKED) {
            throw new LockedException("用户账号已被锁定");
        }
        // User 实体类中 isEnabled() 和 isAccountNonLocked() 已经处理了这些逻辑，
        // 但在这里提前抛出更明确的异常，可以提供更好的错误信息。

        return user;
    }
}