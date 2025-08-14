package club.ppmc.workflow.repository;

import club.ppmc.workflow.domain.WorkflowTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * @author 你的名字
 * @description WorkflowTemplate 实体的 JPA Repository 接口
 */
@Repository
public interface WorkflowTemplateRepository extends JpaRepository<WorkflowTemplate, Long> {

    /**
     * 根据表单定义ID查找其关联的工作流模板
     * @param formDefinitionId 表单定义 ID
     * @return Optional<WorkflowTemplate>
     */
    Optional<WorkflowTemplate> findByFormDefinitionId(Long formDefinitionId);
}