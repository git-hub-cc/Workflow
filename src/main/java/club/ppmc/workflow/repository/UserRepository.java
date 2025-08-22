package club.ppmc.workflow.repository;

import club.ppmc.workflow.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author cc
 * @description User 实体的 JPA Repository 接口
 */
@Repository
public interface UserRepository extends JpaRepository<User, String>, JpaSpecificationExecutor<User> {

    /**
     * 【核心修改】将此方法修改为支持分页的 Page 返回类型，以优化管理员侧的全量用户选择器。
     *
     * @param id       用户ID关键词
     * @param name     用户姓名关键词
     * @param pageable 分页参数
     * @return 用户分页对象
     */
    Page<User> findByIdContainingIgnoreCaseOrNameContainingIgnoreCaseOrderByNameAsc(String id, String name, Pageable pageable);
}