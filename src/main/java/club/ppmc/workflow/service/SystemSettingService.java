package club.ppmc.workflow.service;

import club.ppmc.workflow.domain.SystemSetting;
import club.ppmc.workflow.repository.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author cc
 * @description 【新增】系统设置相关的业务逻辑服务
 */
@Service
@RequiredArgsConstructor
@Transactional
public class SystemSettingService {

    private final SystemSettingRepository systemSettingRepository;

    /**
     * 定义所有公开的设置项键
     */
    public static final class PublicSettingKeys {
        public static final List<String> ALL = List.of(
                SettingKeys.SYSTEM_NAME,
                SettingKeys.SYSTEM_ICON_ID,
                SettingKeys.THEME_COLOR,
                SettingKeys.FOOTER_INFO,
                SettingKeys.LOGIN_BACKGROUND_ID
        );
    }

    /**
     * 定义所有系统设置项的键常量
     */
    public static final class SettingKeys {
        public static final String SYSTEM_NAME = "SYSTEM_NAME";
        public static final String SYSTEM_ICON_ID = "SYSTEM_ICON_ID";
        public static final String THEME_COLOR = "THEME_COLOR";
        public static final String FOOTER_INFO = "FOOTER_INFO";
        public static final String LOGIN_BACKGROUND_ID = "LOGIN_BACKGROUND_ID";
    }

    /**
     * 获取所有系统设置，以Map形式返回 (供管理端使用)
     * @return Map<String, String>
     */
    @Transactional(readOnly = true)
    public Map<String, String> getSettingsMap() {
        return systemSettingRepository.findAll().stream()
                // [FIX] Handle potential null values for settingValue. Collectors.toMap does not allow null values.
                // We replace any null value with an empty string, which is a safe default.
                .collect(Collectors.toMap(
                        SystemSetting::getSettingKey,
                        setting -> setting.getSettingValue() != null ? setting.getSettingValue() : ""
                ));
    }

    /**
     * 获取所有公开的系统设置 (供登录页等公共页面使用)
     * @return Map<String, String>
     */
    @Transactional(readOnly = true)
    public Map<String, String> getPublicSettings() {
        return systemSettingRepository.findAll().stream()
                .filter(setting -> PublicSettingKeys.ALL.contains(setting.getSettingKey()))
                // [FIX] Handle potential null values for settingValue, same as in getSettingsMap().
                .collect(Collectors.toMap(
                        SystemSetting::getSettingKey,
                        setting -> setting.getSettingValue() != null ? setting.getSettingValue() : ""
                ));
    }


    /**
     * 批量更新系统设置
     * @param settingsMap 包含要更新的设置项的Map
     */
    public void updateSettings(Map<String, String> settingsMap) {
        settingsMap.forEach((key, value) -> {
            SystemSetting setting = systemSettingRepository.findBySettingKey(key)
                    .orElse(new SystemSetting()); // 如果不存在则创建新的

            setting.setSettingKey(key);
            setting.setSettingValue(value);
            // 注意：description 在此方法中不更新，仅在初始化时设置
            systemSettingRepository.save(setting);
        });
    }
}