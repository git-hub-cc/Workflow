package club.ppmc.workflow.repository;

import club.ppmc.workflow.domain.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author cc
 * @description Task 实体的 JPA Repository 接口 (在第三阶段主要用于历史记录)
 */
@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    /**
     * 根据办理人ID和任务状态查找任务列表
     * @param assigneeId 办理人ID
     * @param status 任务状态
     * @return 任务列表
     */
    List<Task> findByAssigneeIdAndStatus(String assigneeId, Task.Status status);
}