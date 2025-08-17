package club.ppmc.workflow.repository;

import club.ppmc.workflow.domain.UserGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author 你的名字
 * @description UserGroup 实体的 JPA Repository 接口
 */
@Repository
public interface UserGroupRepository extends JpaRepository<UserGroup, Long> {
}