package club.ppmc.workflow.repository;

import club.ppmc.workflow.domain.FormDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author cc
 * @description FormDefinition 实体的 JPA Repository 接口
 */
@Repository
public interface FormDefinitionRepository extends JpaRepository<FormDefinition, Long> {
}