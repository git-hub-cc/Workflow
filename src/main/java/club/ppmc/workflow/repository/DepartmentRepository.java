package club.ppmc.workflow.repository;

import club.ppmc.workflow.domain.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author cc
 * @description 新增的 Department 实体的 JPA Repository 接口
 */
@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    /**
     * 查找所有顶级部门（没有父部门的部门）
     * @return 顶级部门列表
     */
    List<Department> findByParentIsNull();

}