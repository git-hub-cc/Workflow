package club.ppmc.workflow.domain;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * @author cc
 * @description 角色实体
 */
@Entity
@Getter
@Setter
@EqualsAndHashCode(of = "id") // 2. 添加注解，指定基于 id 字段进行比较
@Table(name = "app_role")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name; // e.g., "ADMIN", "FINANCE_APPROVER"

    private String description;

    @ManyToMany(mappedBy = "roles")
    private Set<User> users = new HashSet<>();

    // --- 【新增】 ---
    // 反向关联，表示哪些菜单被这个角色所拥有。由 Menu 实体的 roles 字段维护关系。
    @ManyToMany(mappedBy = "roles")
    private Set<Menu> menus = new HashSet<>();
}