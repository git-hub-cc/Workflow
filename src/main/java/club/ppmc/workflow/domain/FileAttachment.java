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

    // --- 【数据不一致修复】新增文件状态枚举 ---
    public enum FileStatus {
        /**
         * 临时状态：文件已上传，但尚未关联到任何最终提交记录。此状态下的文件可被定时任务清理。
         */
        TEMPORARY,
        /**
         * 已关联状态：文件已成功关联到一份表单提交记录。
         */
        LINKED
    }


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

    // --- 【数据不一致修复】新增状态字段 ---
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FileStatus status;

    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
        // --- 【数据不一致修复】新上传的文件默认为临时状态 ---
        status = FileStatus.TEMPORARY;
    }
}