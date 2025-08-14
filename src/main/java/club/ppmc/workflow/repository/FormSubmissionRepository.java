package club.ppmc.workflow.repository;

import club.ppmc.workflow.domain.FormSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author 你的名字
 * @description FormSubmission 实体的 JPA Repository 接口
 */
@Repository
public interface FormSubmissionRepository extends JpaRepository<FormSubmission, Long> {

    /**
     * 根据表单定义ID查找所有提交记录
     * @param formDefinitionId 表单定义的ID
     * @return 提交记录列表
     */
    List<FormSubmission> findByFormDefinitionId(Long formDefinitionId);
}