package club.ppmc.workflow.repository;

import club.ppmc.workflow.domain.PageSchema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * @author cc
 * @description 【新增】PageSchema 实体的 JPA Repository 接口
 */
@Repository
public interface PageSchemaRepository extends JpaRepository<PageSchema, Long>, JpaSpecificationExecutor<PageSchema> {

    /**
     * 根据页面的唯一标识 (pageKey) 查找页面结构。
     * 这是前端渲染端获取页面结构的核心方法。
     *
     * @param pageKey 页面的唯一标识
     * @return Optional<PageSchema>
     */
    Optional<PageSchema> findByPageKey(String pageKey);

    /**
     * 检查是否存在具有给定 pageKey 的页面。
     * @param pageKey 页面的唯一标识
     * @return boolean
     */
    boolean existsByPageKey(String pageKey);
}