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

    // --- 【核心修改】将 department 字段从 String 修改为与 Department 实体的关联 ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;
    // --- 【修改结束】 ---


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private User manager;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(nullable = false)
    private boolean passwordChangeRequired = false;

    @ManyToMany(fetch = FetchType.EAGER)
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


    // --- UserDetails 接口实现 ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public String getUsername() {
        return this.id;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return this.status != UserStatus.LOCKED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return !this.passwordChangeRequired;
    }

    @Override
    public boolean isEnabled() {
        return this.status == UserStatus.ACTIVE;
    }

    /**
     * 【新增】用于数据迁移的辅助方法，迁移完成后可删除
     * @return 旧的部门字符串名称
     */
    @Transient
    public String getDepartmentName() {
        // 这里的实现取决于您如何存储旧的 department 字符串。
        // 如果您在迁移期间临时保留了旧字段，可以直接返回。
        // 如果已经删除了，此方法将无法工作。
        // 这是一个占位符，您需要根据实际情况调整。
        // 例如: return this.oldDepartmentStringField;
        return this.department != null ? this.department.getName() : null;
    }
}