package club.ppmc.workflow.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author cc
 * @description 新增的部门实体，用于构建组织架构
 */
@Entity
@Getter
@Setter
@Table(name = "app_department")
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    // 自关联：父部门
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @JsonBackReference // 避免在序列化时产生无限递归
    private Department parent;

    // 自关联：子部门列表
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference // 标记这是关系的主控方
    private List<Department> children = new ArrayList<>();

    // 部门负责人
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private User manager;

    // 同级部门排序
    private Integer orderNum;

    // 部门下的员工列表
    @OneToMany(mappedBy = "department", fetch = FetchType.LAZY)
    private List<User> users = new ArrayList<>();
}