package club.ppmc.workflow.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

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

    private String department; // 用户所属部门

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private User manager;

    // --- 【新增字段】 ---
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE; // 默认状态为 ACTIVE

    @Column(nullable = false)
    private boolean passwordChangeRequired = false; // 是否需要强制修改密码

    // --- 【关系修改】 ---
    @ManyToMany(fetch = FetchType.EAGER) // EAGER a an EAGER fetch for roles is often practical for authorization
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_groups",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "group_id")
    )
    private Set<UserGroup> userGroups = new HashSet<>();


    // --- UserDetails 接口实现 (已更新) ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 从 roles 集合动态构建权限列表
        return this.roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .collect(Collectors.toList());
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
        // 根据 status 字段判断
        return this.status != UserStatus.LOCKED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        // 如果需要强制修改密码，则认为凭证已过期
        return !this.passwordChangeRequired;
    }

    @Override
    public boolean isEnabled() {
        // 根据 status 字段判断
        return this.status == UserStatus.ACTIVE;
    }
}