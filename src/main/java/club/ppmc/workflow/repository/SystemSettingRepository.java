package club.ppmc.workflow.repository;

import club.ppmc.workflow.domain.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * @author cc
 * @description 【新增】SystemSetting 实体的 JPA Repository 接口
 */
@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSetting, Long> {

    /**
     * 根据设置项的键查找设置
     * @param settingKey 设置项的唯一键
     * @return Optional<SystemSetting>
     */
    Optional<SystemSetting> findBySettingKey(String settingKey);
}