package club.ppmc.workflow.repository;

import club.ppmc.workflow.domain.UserGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author cc
 * @description UserGroup 实体的 JPA Repository 接口
 */
@Repository
public interface UserGroupRepository extends JpaRepository<UserGroup, Long>, JpaSpecificationExecutor<UserGroup> {

    // 【新增】根据名称查找用户组
    Optional<UserGroup> findByName(String name);

    // 【新增】根据一组名称查找所有用户组
    Set<UserGroup> findByNameIn(List<String> names);
}