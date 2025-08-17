package club.ppmc.workflow.controller;

import club.ppmc.workflow.dto.DepartmentTreeNode;
import club.ppmc.workflow.dto.ProcessInstanceDto;
import club.ppmc.workflow.dto.TreeNodeDto; // 【新增】导入
import club.ppmc.workflow.dto.UserDto;
import club.ppmc.workflow.service.AdminService;
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

    // --- 流程实例管理 ---

    /**
     * API: 获取所有正在运行的流程实例
     */
    @GetMapping("/instances")
    public ResponseEntity<List<ProcessInstanceDto>> getActiveProcessInstances() {
        return ResponseEntity.ok(adminService.getActiveProcessInstances());
    }

    /**
     * API: 终止一个正在运行的流程实例
     */
    @DeleteMapping("/instances/{processInstanceId}")
    public ResponseEntity<Void> terminateProcessInstance(@PathVariable String processInstanceId,
                                                         @RequestParam String reason) {
        adminService.terminateProcessInstance(processInstanceId, reason);
        return ResponseEntity.ok().build();
    }

    /**
     * API: 挂起一个流程实例
     */
    @PostMapping("/instances/{processInstanceId}/suspend")
    public ResponseEntity<Void> suspendProcessInstance(@PathVariable String processInstanceId) {
        adminService.suspendProcessInstance(processInstanceId);
        return ResponseEntity.ok().build();
    }

    /**
     * API: 激活一个流程实例
     */
    @PostMapping("/instances/{processInstanceId}/activate")
    public ResponseEntity<Void> activateProcessInstance(@PathVariable String processInstanceId) {
        adminService.activateProcessInstance(processInstanceId);
        return ResponseEntity.ok().build();
    }

    /**
     * API: 改派一个任务
     */
    @PostMapping("/tasks/{taskId}/reassign")
    public ResponseEntity<Void> reassignTask(@PathVariable String taskId, @RequestBody Map<String, String> payload) {
        String newAssigneeId = payload.get("newAssigneeId");
        if (newAssigneeId == null || newAssigneeId.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        adminService.reassignTask(taskId, newAssigneeId);
        return ResponseEntity.ok().build();
    }


    // --- 用户管理 ---

    /**
     * API: 创建一个新用户
     */
    @PostMapping("/users")
    public ResponseEntity<UserDto> createUser(@RequestBody UserDto userDto) {
        UserDto createdUser = adminService.createUser(userDto);
        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
    }

    /**
     * API: 更新一个用户信息
     */
    @PutMapping("/users/{id}")
    public ResponseEntity<UserDto> updateUser(@PathVariable String id, @RequestBody UserDto userDto) {
        UserDto updatedUser = adminService.updateUser(id, userDto);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * API: 删除一个用户
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        adminService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // --- 组织架构与数据源 ---
    /**
     * API: 获取组织架构树 (部门+用户, Ant Design Tree 使用)
     * 权限: 已认证用户 (因为用户选择器所有人都可能用到)
     */
    @GetMapping("/organization-tree")
    @PreAuthorize("isAuthenticated()") // 覆盖类级别的 'ADMIN' 限制
    public ResponseEntity<List<DepartmentTreeNode>> getOrganizationTree() {
        return ResponseEntity.ok(adminService.getOrganizationTree());
    }

    // --- 【新增API：用于树形选择器】 ---
    /**
     * API: 获取指定类型的树形结构数据
     * @param source 数据源标识, 例如 "departments"
     * @return 树形节点列表
     */
    @GetMapping("/tree-data-source")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TreeNodeDto>> getTreeDataSource(@RequestParam String source) {
        if ("departments".equalsIgnoreCase(source)) {
            return ResponseEntity.ok(adminService.getDepartmentTree());
        }
        // 未来可以扩展其他数据源, 如商品分类等
        // else if ("categories".equalsIgnoreCase(source)) { ... }
        return ResponseEntity.badRequest().build();
    }
}