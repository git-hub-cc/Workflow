package club.ppmc.workflow.controller;

import club.ppmc.workflow.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @author cc
 * @description 【新增】提供公开访问的 API 控制器，无需认证
 */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PublicController {

    private final SystemSettingService systemSettingService;

    /**
     * API: 获取公开的系统设置 (如系统名称、图标、登录背景等)
     * @return 包含公开设置项的Map
     */
    @GetMapping("/settings")
    public ResponseEntity<Map<String, String>> getPublicSettings() {
        return ResponseEntity.ok(systemSettingService.getPublicSettings());
    }
}