package club.ppmc.workflow.service;

import club.ppmc.workflow.domain.User;
import club.ppmc.workflow.dto.AuthRequest;
import club.ppmc.workflow.dto.AuthResponse;
import club.ppmc.workflow.dto.UserDto;
import club.ppmc.workflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    /**
     * 对用户进行认证，如果成功，则生成并返回 JWT
     * @param request 包含用户ID和密码的认证请求
     * @return 包含 Token 和用户信息的响应
     */
    public AuthResponse authenticate(AuthRequest request) {
        // 使用 Spring Security 的 AuthenticationManager 进行认证
        // 它会自动调用 UserDetailsServiceImpl 和 PasswordEncoder
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUserId(),
                        request.getPassword()
                )
        );

        // 如果上面的代码没有抛出异常，说明认证成功
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new UsernameNotFoundException("认证成功但无法在数据库中找到用户，数据不一致"));

        // 生成 JWT
        String jwtToken = jwtService.generateToken(user);

        // 构建响应 DTO
        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setName(user.getName());
        userDto.setRole(user.getRole());

        return AuthResponse.builder()
                .token(jwtToken)
                .user(userDto)
                .build();
    }
}