package club.ppmc.workflow.controller;

import club.ppmc.workflow.dto.MenuDto;
import club.ppmc.workflow.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * @author cc
 * @description 菜单相关的 RESTful API 控制器
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    /**
     * API: 获取当前登录用户的可访问菜单树
     * @param principal Spring Security 自动注入的当前用户信息
     * @return 菜单树结构的 DTO 列表
     */
    @GetMapping("/menus/my-menus")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MenuDto>> getMyMenus(Principal principal) {
        String userId = principal.getName();
        List<MenuDto> menuTree = menuService.getMenuTreeForUser(userId);
        return ResponseEntity.ok(menuTree);
    }

    // --- 以下为管理员操作 ---

    /**
     * API: (管理员)获取所有菜单的完整树形结构
     */
    @GetMapping("/admin/menus/tree")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<MenuDto>> getAllMenusAsTree() {
        return ResponseEntity.ok(menuService.getAllMenusAsTree());
    }

    /**
     * API: (管理员)创建新菜单
     */
    @PostMapping("/admin/menus")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MenuDto> createMenu(@RequestBody MenuDto menuDto) {
        MenuDto createdMenu = menuService.createMenu(menuDto);
        return new ResponseEntity<>(createdMenu, HttpStatus.CREATED);
    }

    /**
     * API: (管理员)更新菜单
     */
    @PutMapping("/admin/menus/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MenuDto> updateMenu(@PathVariable Long id, @RequestBody MenuDto menuDto) {
        MenuDto updatedMenu = menuService.updateMenu(id, menuDto);
        return ResponseEntity.ok(updatedMenu);
    }

    /**
     * 【新增】API: (管理员)通过拖拽更新整个菜单树的结构和顺序
     */
    @PutMapping("/admin/menus/update-tree")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateMenuTree(@RequestBody List<MenuDto> menuTree) {
        menuService.updateMenuTree(menuTree);
        return ResponseEntity.ok().build();
    }


    /**
     * API: (管理员)删除菜单
     */
    @DeleteMapping("/admin/menus/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteMenu(@PathVariable Long id) {
        menuService.deleteMenu(id);
        return ResponseEntity.noContent().build();
    }
}