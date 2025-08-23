package club.ppmc.workflow.repository;

import club.ppmc.workflow.domain.Menu;
import club.ppmc.workflow.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author cc
 * @description Menu 实体的 JPA Repository 接口
 */
@Repository
public interface MenuRepository extends JpaRepository<Menu, Long> {

    /**
     * 根据路由路径查找菜单
     * @param path 路由路径
     * @return Optional<Menu>
     */
    Optional<Menu> findByPath(String path);

    /**
     * 查询指定角色集合可以访问的所有可见菜单，并按排序号升序
     * @param roles 角色集合
     * @return 菜单列表
     */
    @Query("SELECT DISTINCT m FROM Menu m JOIN m.roles r WHERE r IN :roles AND m.visible = true ORDER BY m.orderNum ASC")
    List<Menu> findVisibleMenusByRoles(@Param("roles") Set<Role> roles);

    /**
     * 检查是否存在子菜单
     * @param parentId 父菜单ID
     * @return 如果存在子菜单则返回 true
     */
    boolean existsByParentId(Long parentId);

    /**
     * 【新增】检查是否有菜单关联了指定的表单定义ID
     * @param formDefinitionId 表单定义ID
     * @return 如果存在关联则返回 true
     */
    boolean existsByFormDefinitionId(Long formDefinitionId);

    // --- 【核心新增】 ---
    /**
     * 查找所有关联到指定表单定义的菜单列表。
     * @param formDefinitionId 表单定义ID
     * @return 关联的菜单列表
     */
    List<Menu> findByFormDefinitionId(Long formDefinitionId);
}