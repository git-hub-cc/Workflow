package club.ppmc.workflow.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "app_user")
public class User implements UserDetails {
    @Id
    private String id; // 将作为 username

    private String name;

    @Column(nullable = false)
    private String password; // 存储编码后的密码

    @Column(nullable = false)
    private String role; // 例如: "USER", "ADMIN"

    // --- 新增字段: 用户的直属上级 ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id") // 在数据库中创建外键列 manager_id
    private User manager;

    // --- UserDetails 接口实现 ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 角色需要以 "ROLE_" 为前缀，这是 Spring Security 的约定
        return List.of(new SimpleGrantedAuthority("ROLE_" + this.role));
    }

    @Override
    public String getUsername() {
        return this.id; // 使用 id 作为 Spring Security 的 username
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // 账户永不过期
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // 账户永不锁定
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // 凭证永不过期
    }

    @Override
    public boolean isEnabled() {
        return true; // 账户启用
    }
}