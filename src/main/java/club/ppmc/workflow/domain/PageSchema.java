package club.ppmc.workflow.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * @author cc
 * @description 【新增】用于存储低代码页面结构的核心实体
 */
@Entity
@Getter
@Setter
@Table(name = "page_schema")
public class PageSchema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 页面的唯一标识, 用于URL路由。例如: "homepage", "about-us", "products/detail-template"
     */
    @Column(nullable = false, unique = true)
    private String pageKey;

    /**
     * 页面的可读名称, 用于在管理后台显示。例如: "商城首页", "关于我们"
     */
    @Column(nullable = false)
    private String name;

    /**
     * 存储页面结构的JSON字符串。由前端可视化设计器生成。
     */
    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String schemaJson;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /**
     * 最后更新时间
     */
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}