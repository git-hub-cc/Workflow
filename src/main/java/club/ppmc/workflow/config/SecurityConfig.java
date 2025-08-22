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
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // 公开访问的端点
                        .requestMatchers("/api/auth/**", "/h2-console/**",  "/camunda/**", "/api/public/**").permitAll()
                        // 允许对文件进行公开的GET请求（如图标、背景图），而其他操作（如POST上传）仍需认证
                        .requestMatchers(HttpMethod.GET, "/api/files/**").permitAll()

                        // 【核心修复】新增：允许匿名访问静态资源。这对于加载前端页面（如HTML, CSS, JS, 图片）至关重要。
                        .requestMatchers("/", "/index.html", "/favicon.ico", "/*.css", "/*.js", "/*.svg", "/static/**", "/assets/**", "/images/**").permitAll()

                        // 需要认证的端点
                        .requestMatchers("/api/menus/my-menus").authenticated()
                        .requestMatchers("/api/files/**").authenticated() // 此规则现在主要对非GET请求（如上传）生效
                        .requestMatchers(HttpMethod.POST, "/api/users/me/change-password").authenticated()
                        // --- 【核心新增】为表单中的用户选择器提供一个安全的搜索接口，所有登录用户可用 ---
                        .requestMatchers("/api/users/search-for-picker").authenticated()
                        // 流程设计器中的用户选择器，现在仅限管理员可用，防止普通用户获取全量用户列表
                        .requestMatchers("/api/workflows/users").hasRole("ADMIN")
                        .requestMatchers("/api/workflows/groups").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/forms/import-word").hasRole("ADMIN")

                        // --- 【核心修改】将组织树接口权限放宽至所有登录用户，以便表单中的人员选择器使用 ---
                        // 这条规则必须放在下面的 "/api/admin/**" 规则之前才会生效
                        .requestMatchers("/api/admin/organization-tree").authenticated()

                        // 所有 /api/admin/ 下的其他请求仍需要ADMIN角色
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        http.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()));

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}