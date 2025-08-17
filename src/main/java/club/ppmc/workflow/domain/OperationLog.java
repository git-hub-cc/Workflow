package club.ppmc.workflow.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * @author cc
 * @description 操作日志实体
 */
@Entity
@Getter
@Setter
@Table(name = "operation_log")
public class OperationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String operatorId;

    private String operatorName;

    @Column(nullable = false, updatable = false)
    private LocalDateTime operationTime;

    @Column(nullable = false)
    private String module; // 操作模块

    @Column(nullable = false)
    private String action; // 具体操作

    private String targetId; // 操作对象的ID

    @Lob
    @Column(columnDefinition = "TEXT")
    private String details; // 操作详情 (如请求参数的JSON)

    private String ipAddress;

    @PrePersist
    protected void onCreate() {
        operationTime = LocalDateTime.now();
    }
}