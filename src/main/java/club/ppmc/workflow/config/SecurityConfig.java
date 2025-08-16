package club.ppmc.workflow.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // 启用方法级别的安全注解, 如 @PreAuthorize
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // 禁用 CSRF，因为我们使用无状态的 JWT
                .authorizeHttpRequests(auth -> auth
                        // 公开访问端点：登录接口和 H2 数据库控制台
                        // todo camunda为临时添加
                        .requestMatchers("/api/auth/**", "/h2-console/**",  "/camunda/**").permitAll()
                        // 【新增】允许已认证用户访问自己的密码修改接口
                        .requestMatchers(HttpMethod.POST, "/api/users/me/change-password").authenticated()
                        // 其他所有请求都需要经过认证
                        .anyRequest().authenticated()
                )
                // 配置会话管理为无状态 (STATELESS)，因为我们不使用 session
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                // 在标准的 UsernamePasswordAuthenticationFilter 之前添加我们的 JWT 过滤器
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        // 为了 H2 控制台能在 iframe 中正常显示
        http.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()));

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        // 设置自定义的 UserDetailsService，用于从数据库加载用户信息
        authProvider.setUserDetailsService(userDetailsService);
        // 设置密码编码器，用于密码校验
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // 使用 BCrypt 算法进行密码加密
        return new BCryptPasswordEncoder();
    }
}