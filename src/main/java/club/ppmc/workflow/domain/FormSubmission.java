package club.ppmc.workflow.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "form_submission")
public class FormSubmission {

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

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}