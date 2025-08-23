package club.ppmc.workflow.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "form_submission")
public class FormSubmission {

    // --- 【核心新增】定义申请的业务状态枚举 ---
    public enum SubmissionStatus {
        /**
         * 草稿：仅保存，未启动工作流
         */
        DRAFT,
        /**
         * 处理中：已启动工作流
         */
        PROCESSING,
        /**
         * 已通过：工作流正常结束
         */
        APPROVED,
        /**
         * 已拒绝：工作流被拒绝
         */
        REJECTED,
        /**
         * 已终止：工作流被手动终止
         */
        TERMINATED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_definition_id", nullable = false)
    private FormDefinition formDefinition;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String dataJson;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @OneToOne(mappedBy = "formSubmission", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private WorkflowInstance workflowInstance;

    @Column
    private String submitterId;

    // --- 【核心新增】增加业务状态字段 ---
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubmissionStatus status;

    // --- 【新增：与文件附件的一对多关系】 ---
    @OneToMany(
            mappedBy = "formSubmission",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<FileAttachment> attachments = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        // --- 【核心修改】新创建的申请默认为草稿状态 ---
        if (status == null) {
            status = SubmissionStatus.DRAFT;
        }
    }
}