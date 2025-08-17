package club.ppmc.workflow.repository;

import club.ppmc.workflow.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author 你的名字
 * @description Role 实体的 JPA Repository 接口
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * 根据角色名称查找角色
     * @param name 角色名称
     * @return Optional<Role>
     */
    Optional<Role> findByName(String name);

    /**
     * 根据一组角色名称查找所有角色
     * @param names 角色名称列表
     * @return 角色集合
     */
    Set<Role> findByNameIn(List<String> names);
}