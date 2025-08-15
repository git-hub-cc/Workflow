package club.ppmc.workflow.service;

import club.ppmc.workflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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
        // 直接从 UserRepository 查找 User 实体，因为它已经实现了 UserDetails 接口
        return userRepository.findById(username)
                .orElseThrow(() -> new UsernameNotFoundException("未找到用户: " + username));
    }
}