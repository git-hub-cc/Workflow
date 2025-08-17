package club.ppmc.workflow.controller;

import club.ppmc.workflow.dto.*;
import club.ppmc.workflow.service.AdminService;
import club.ppmc.workflow.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * @author 你的名字
 * @description 管理员相关的 RESTful API 控制器
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')") // 在类级别上要求所有接口调用者都必须拥有 ADMIN 角色
public class AdminController {

    private final AdminService adminService;
    private final UserService userService; // 注入 UserService

    // --- 流程实例管理 (保持不变) ---

    @GetMapping("/instances")
    public ResponseEntity<List<ProcessInstanceDto>> getActiveProcessInstances() {
        return ResponseEntity.ok(adminService.getActiveProcessInstances());
    }

    @DeleteMapping("/instances/{processInstanceId}")
    public ResponseEntity<Void> terminateProcessInstance(@PathVariable String processInstanceId,
                                                         @RequestParam String reason) {
        adminService.terminateProcessInstance(processInstanceId, reason);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/instances/{processInstanceId}/suspend")
    public ResponseEntity<Void> suspendProcessInstance(@PathVariable String processInstanceId) {
        adminService.suspendProcessInstance(processInstanceId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/instances/{processInstanceId}/activate")
    public ResponseEntity<Void> activateProcessInstance(@PathVariable String processInstanceId) {
        adminService.activateProcessInstance(processInstanceId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/tasks/{taskId}/reassign")
    public ResponseEntity<Void> reassignTask(@PathVariable String taskId, @RequestBody Map<String, String> payload) {
        String newAssigneeId = payload.get("newAssigneeId");
        if (newAssigneeId == null || newAssigneeId.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        adminService.reassignTask(taskId, newAssigneeId);
        return ResponseEntity.ok().build();
    }


    // --- 用户管理 (已更新) ---

    @PostMapping("/users")
    public ResponseEntity<UserDto> createUser(@RequestBody UserDto userDto) {
        UserDto createdUser = adminService.createUser(userDto);
        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<UserDto> updateUser(@PathVariable String id, @RequestBody UserDto userDto) {
        UserDto updatedUser = adminService.updateUser(id, userDto);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * API: 删除一个用户 (逻辑删除)
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        adminService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * API: (管理员)重置用户密码
     */
    @PostMapping("/users/{id}/reset-password")
    public ResponseEntity<Void> resetPassword(@PathVariable String id) {
        userService.resetPassword(id);
        return ResponseEntity.ok().build();
    }

    // --- 角色管理 (新增) ---

    /**
     * API: 创建一个新角色
     */
    @PostMapping("/roles")
    public ResponseEntity<RoleDto> createRole(@RequestBody RoleDto roleDto) {
        RoleDto createdRole = adminService.createRole(roleDto);
        return new ResponseEntity<>(createdRole, HttpStatus.CREATED);
    }

    /**
     * API: 获取所有角色列表
     */
    @GetMapping("/roles")
    public ResponseEntity<List<RoleDto>> getAllRoles() {
        return ResponseEntity.ok(adminService.getAllRoles());
    }
    // ... 可以添加更新和删除角色的端点 ...


    // --- 组织架构与数据源 (保持不变) ---

    @GetMapping("/organization-tree")
    @PreAuthorize("isAuthenticated()") // 覆盖类级别的 'ADMIN' 限制
    public ResponseEntity<List<DepartmentTreeNode>> getOrganizationTree() {
        return ResponseEntity.ok(adminService.getOrganizationTree());
    }

    @GetMapping("/tree-data-source")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TreeNodeDto>> getTreeDataSource(@RequestParam String source) {
        if ("departments".equalsIgnoreCase(source)) {
            return ResponseEntity.ok(adminService.getDepartmentTree());
        }
        return ResponseEntity.badRequest().build();
    }
}