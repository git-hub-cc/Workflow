package club.ppmc.workflow.controller;

import club.ppmc.workflow.aop.LogOperation;
import club.ppmc.workflow.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * @author cc
 * @description 【新增】系统设置相关的 RESTful API 控制器 (需要管理员权限)
 */
@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')")
public class SystemSettingController {

    private final SystemSettingService systemSettingService;

    /**
     * API: (管理员)获取所有系统设置
     * @return 包含所有设置项的Map
     */
    @GetMapping
    public ResponseEntity<Map<String, String>> getAllSettings() {
        return ResponseEntity.ok(systemSettingService.getSettingsMap());
    }

    /**
     * API: (管理员)更新系统设置
     * @param settingsMap 包含要更新的设置项的Map
     * @return 成功响应
     */
    @PutMapping
    @LogOperation(module = "系统管理", action = "更新系统设置")
    public ResponseEntity<Void> updateSettings(@RequestBody Map<String, String> settingsMap) {
        systemSettingService.updateSettings(settingsMap);
        return ResponseEntity.ok().build();
    }
}