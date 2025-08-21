package club.ppmc.workflow.repository;

import club.ppmc.workflow.domain.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author cc
 * @description User 实体的 JPA Repository 接口
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

    /**
     * 【新增】根据用户ID或姓名模糊查询用户，并限制返回结果数量。
     * 用于前端的人员选择器远程搜索功能。
     *
     * @param id       用户ID关键词
     * @param name     用户姓名关键词
     * @param pageable 分页参数，用于限制返回结果的数量 (e.g., PageRequest.of(0, 10))
     * @return 用户列表
     */
    List<User> findByIdContainingIgnoreCaseOrNameContainingIgnoreCaseOrderByNameAsc(String id, String name, Pageable pageable);
}