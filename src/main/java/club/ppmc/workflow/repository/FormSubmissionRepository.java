package club.ppmc.workflow.repository;

import club.ppmc.workflow.domain.FormSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author cc
 * @description FormSubmission 实体的 JPA Repository 接口
 */
@Repository
public interface FormSubmissionRepository extends JpaRepository<FormSubmission, Long>, JpaSpecificationExecutor<FormSubmission> {

    /**
     * 根据表单定义ID查找所有提交记录
     * @param formDefinitionId 表单定义的ID
     * @return 提交记录列表
     */
    List<FormSubmission> findByFormDefinitionId(Long formDefinitionId);

    // 【核心修改】原 findBySubmitterIdOrderByCreatedAtDesc 方法将被更灵活的 Specification 查询替代，故移除。

    // --- 【核心新增】 ---
    /**
     * 根据表单定义ID统计提交记录的数量。
     * @param formDefinitionId 表单定义ID
     * @return 提交记录数
     */
    long countByFormDefinitionId(Long formDefinitionId);

    /**
     * 根据表单定义ID批量删除所有提交记录。
     * @param formDefinitionId 表单定义ID
     */
    void deleteByFormDefinitionId(Long formDefinitionId);
}