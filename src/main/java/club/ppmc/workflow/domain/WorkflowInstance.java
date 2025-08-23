package club.ppmc.workflow.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "workflow_instance")
public class WorkflowInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private WorkflowTemplate template;

    @OneToOne
    @JoinColumn(name = "submission_id", referencedColumnName = "id", nullable = false)
    private FormSubmission formSubmission;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(nullable = false, unique = true)
    private String processInstanceId;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;

    public enum Status {
        PROCESSING,
        APPROVED,
        REJECTED,
        // --- 【核心新增】增加终止状态，与 Camunda 保持一致 ---
        TERMINATED
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        status = Status.PROCESSING;
    }
}