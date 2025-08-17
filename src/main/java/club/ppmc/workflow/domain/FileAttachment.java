package club.ppmc.workflow.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * @author cc
 * @description 文件附件实体
 */
@Entity
@Getter
@Setter
@Table(name = "file_attachment")
public class FileAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 关联的表单提交，可以为null，表示文件已上传但尚未关联到任何提交
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id")
    private FormSubmission formSubmission;

    @Column(nullable = false)
    private String originalFilename;

    @Column(nullable = false)
    private String storedFilename; // 存储在磁盘上的唯一文件名 (e.g., UUID)

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private Long size;

    @Column(nullable = false, updatable = false)
    private String uploaderId;

    @Column(updatable = false)
    private LocalDateTime uploadedAt;

    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
    }
}