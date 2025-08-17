package club.ppmc.workflow.repository;

import club.ppmc.workflow.domain.FileAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}