package club.ppmc.workflow.repository;

import club.ppmc.workflow.domain.FileAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author cc
 * @description FileAttachment 实体的 JPA Repository 接口
 */
@Repository
public interface FileAttachmentRepository extends JpaRepository<FileAttachment, Long> {

    /**
     * 根据表单提交ID查找所有附件
     * @param submissionId 表单提交ID
     * @return 附件列表
     */
    List<FileAttachment> findByFormSubmissionId(Long submissionId);

    // --- 【数据不一致修复】新增查询方法，用于定时清理任务 ---
    /**
     * 查找在指定时间之前创建的、处于特定状态的所有附件记录。
     * @param status 文件状态 (例如: TEMPORARY)
     * @param cutoffTime 截止时间
     * @return 附件记录列表
     */
    List<FileAttachment> findByStatusAndUploadedAtBefore(FileAttachment.FileStatus status, LocalDateTime cutoffTime);
}