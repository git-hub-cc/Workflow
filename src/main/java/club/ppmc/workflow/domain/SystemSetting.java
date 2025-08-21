package club.ppmc.workflow.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * @author cc
 * @description 【新增】系统设置实体，用于存储全局配置项
 */
@Entity
@Getter
@Setter
@Table(name = "app_system_setting")
public class SystemSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 设置项的唯一键 (e.g., "SYSTEM_NAME")
     */
    @Column(nullable = false, unique = true)
    private String settingKey;

    /**
     * 设置项的值
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String settingValue;

    /**
     * 设置项的描述
     */
    private String description;
}