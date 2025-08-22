package club.ppmc.workflow.repository;

import club.ppmc.workflow.domain.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author cc
 * @description 【新增】Notification 实体的 JPA Repository 接口
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 分页查询指定用户的通知
     * @param userId   用户ID
     * @param pageable 分页参数
     * @return 通知分页对象
     */
    Page<Notification> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    /**
     * 统计指定用户的未读通知数量
     * @param userId 用户ID
     * @param isRead 是否已读 (应传入 false)
     * @return 未读通知数量
     */
    long countByUserIdAndIsRead(String userId, boolean isRead);

    /**
     * 将指定用户的所有未读通知标记为已读
     * @param userId 用户ID
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
    int markAllAsReadByUserId(String userId);

    /**
     * 将指定的一批通知标记为已读
     * @param ids 通知ID列表
     * @param userId 用户ID (用于权限校验)
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id IN :ids AND n.user.id = :userId")
    int markAsReadByIds(List<Long> ids, String userId);
}