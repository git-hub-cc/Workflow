package club.ppmc.workflow.controller;

// 【权限修复】导入 PageSchemaDto 和 PageSchemaService
import club.ppmc.workflow.dto.PageSchemaDto;
import club.ppmc.workflow.service.PageSchemaService;
import club.ppmc.workflow.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    // 【权限修复】注入 PageSchemaService
    private final PageSchemaService pageSchemaService;

    /**
     * API: 获取公开的系统设置 (如系统名称、图标、登录背景等)
     * @return 包含公开设置项的Map
     */
    @GetMapping("/settings")
    public ResponseEntity<Map<String, String>> getPublicSettings() {
        return ResponseEntity.ok(systemSettingService.getPublicSettings());
    }

    /**
     * 【权限修复】将此公开接口从 PageSchemaController 移至此处
     * (公开) 根据 pageKey 获取单个页面结构。
     * 这个接口供前端渲染端调用，必须是公开的。
     *
     * @param pageKey 页面的唯一标识
     * @return PageSchemaDto
     */
    @GetMapping("/pages/{pageKey}")
    public ResponseEntity<PageSchemaDto> getPageSchemaByKey(@PathVariable String pageKey) {
        return ResponseEntity.ok(pageSchemaService.getPageSchemaByKey(pageKey));
    }
}