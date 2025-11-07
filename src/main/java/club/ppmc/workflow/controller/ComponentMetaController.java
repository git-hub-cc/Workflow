package club.ppmc.workflow.controller;

import club.ppmc.workflow.dto.ComponentMetaDto;
import club.ppmc.workflow.service.ComponentMetaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author cc
 * @description 【新增】提供组件元数据的 RESTful API 控制器
 * 这个控制器为前端设计器提供可拖拽的组件列表。
 */
@RestController
@RequestMapping("/api/components")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ComponentMetaController {

    private final ComponentMetaService componentMetaService;

    /**
     * (管理员) 获取所有可用的低代码组件元数据。
     * 设计器通过调用此接口来动态构建其组件面板。
     * @return 组件元数据列表
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ComponentMetaDto>> getComponentMetas() {
        return ResponseEntity.ok(componentMetaService.getComponentMetas());
    }
}