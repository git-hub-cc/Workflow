package club.ppmc.workflow.repository;

import club.ppmc.workflow.domain.WorkflowTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * @author cc
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

    /**
     * 【新增】统计有多少工作流模板的BPMN XML内容包含了指定的文本。
     * 这对于检查角色或用户组是否在任何流程定义中被使用非常有用。
     * @param searchText 要在 bpmnXml 字段中搜索的文本
     * @return 包含该文本的模板数量
     */
    long countByBpmnXmlContaining(String searchText);
}