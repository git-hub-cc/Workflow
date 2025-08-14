package club.ppmc.workflow.repository;

import club.ppmc.workflow.domain.WorkflowInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * @author 你的名字
 * @description WorkflowInstance 实体的 JPA Repository 接口
 */
@Repository
public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstance, Long> {

    /**
     * 根据 Camunda 的流程实例 ID 查找本地的流程实例记录
     * @param processInstanceId Camunda 流程实例 ID
     * @return Optional<WorkflowInstance>
     */
    Optional<WorkflowInstance> findByProcessInstanceId(String processInstanceId);
}