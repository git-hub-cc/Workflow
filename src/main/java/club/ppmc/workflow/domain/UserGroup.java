package club.ppmc.workflow.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * @author 你的名字
 * @description 用户组实体 (例如：部门、项目组)
 */
@Entity
@Getter
@Setter
@Table(name = "app_user_group")
public class UserGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name; // e.g., "研发部", "项目A核心团队"

    private String description;

    @ManyToMany(mappedBy = "userGroups")
    private Set<User> users = new HashSet<>();
}