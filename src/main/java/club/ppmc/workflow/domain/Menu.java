package club.ppmc.workflow.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author cc
 * @description 菜单实体，用于构建前端动态侧边栏和路由
 */
@Entity
@Getter
@Setter
@Table(name = "app_menu")
public class Menu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 父菜单ID
    private Long parentId;

    @Column(nullable = false)
    private String name; // 菜单名称, e.g., "入库管理"

    private String path; // 前端路由路径, e.g., "/wms/inbound"

    private String icon; // 菜单图标

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private MenuType type; // 菜单类型

    // 如果是内嵌页面，指向前端组件的路径, e.g., "wms/inbound/DataListView"
    private String componentPath;

    // 关联的表单定义ID (当 type 为 FORM_ENTRY 或 DATA_LIST 时使用)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_definition_id")
    private FormDefinition formDefinition;

    // 排序号
    private Integer orderNum;

    // 是否可见
    private boolean visible = true;

    // 控制菜单访问权限的角色
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "menu_roles",
            joinColumns = @JoinColumn(name = "menu_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    // 瞬态字段，用于构建树形结构
    @Transient
    private List<Menu> children = new ArrayList<>();

    public enum MenuType {
        DIRECTORY,      // 目录 (仅用于分组，不可点击)
        FORM_ENTRY,     // 表单入口 (点击后跳转到表单填写页)
        DATA_LIST,      // 数据列表 (点击后跳转到通用数据列表页)
        REPORT,         // 报表页面 (预留)
        EXTERNAL_LINK   // 外部链接 (预留)
    }
}