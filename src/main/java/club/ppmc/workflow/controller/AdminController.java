package club.ppmc.workflow.controller;

import club.ppmc.workflow.dto.*;
import club.ppmc.workflow.service.AdminService;
import club.ppmc.workflow.service.DepartmentService;
import club.ppmc.workflow.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * @author cc
 * @description 管理员相关的 RESTful API 控制器
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final UserService userService;
    // --- 【核心新增】 ---
    private final DepartmentService departmentService;


    // --- 流程实例管理 ---
    @GetMapping("/instances")
    public ResponseEntity<List<ProcessInstanceDto>> getActiveProcessInstances() {
        return ResponseEntity.ok(adminService.getActiveProcessInstances());
    }

    @DeleteMapping("/instances/{processInstanceId}")
    public ResponseEntity<Void> terminateProcessInstance(@PathVariable String processInstanceId, @RequestParam String reason) {
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


    // --- 用户管理 ---
    @GetMapping("/users")
    public ResponseEntity<List<UserDto>> getAllUsersForAdmin() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

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

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        adminService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/users/{id}/reset-password")
    public ResponseEntity<Void> resetPassword(@PathVariable String id) {
        userService.resetPassword(id);
        return ResponseEntity.ok().build();
    }

    // --- 角色管理 ---
    @PostMapping("/roles")
    public ResponseEntity<RoleDto> createRole(@RequestBody RoleDto roleDto) {
        RoleDto createdRole = adminService.createRole(roleDto);
        return new ResponseEntity<>(createdRole, HttpStatus.CREATED);
    }

    @GetMapping("/roles")
    public ResponseEntity<List<RoleDto>> getAllRoles() {
        return ResponseEntity.ok(adminService.getAllRoles());
    }

    @PutMapping("/roles/{id}")
    public ResponseEntity<RoleDto> updateRole(@PathVariable Long id, @RequestBody RoleDto roleDto) {
        return ResponseEntity.ok(adminService.updateRole(id, roleDto));
    }

    @DeleteMapping("/roles/{id}")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        adminService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }

    // --- 用户组管理 ---
    @PostMapping("/groups")
    public ResponseEntity<UserGroupDto> createGroup(@RequestBody UserGroupDto groupDto) {
        UserGroupDto createdGroup = adminService.createGroup(groupDto);
        return new ResponseEntity<>(createdGroup, HttpStatus.CREATED);
    }

    @GetMapping("/groups")
    public ResponseEntity<List<UserGroupDto>> getAllGroups() {
        return ResponseEntity.ok(adminService.getAllGroups());
    }

    @PutMapping("/groups/{id}")
    public ResponseEntity<UserGroupDto> updateGroup(@PathVariable Long id, @RequestBody UserGroupDto groupDto) {
        return ResponseEntity.ok(adminService.updateGroup(id, groupDto));
    }

    @DeleteMapping("/groups/{id}")
    public ResponseEntity<Void> deleteGroup(@PathVariable Long id) {
        adminService.deleteGroup(id);
        return ResponseEntity.noContent().build();
    }


    // --- 【核心新增】部门管理 API ---
    @PostMapping("/departments")
    public ResponseEntity<DepartmentDto> createDepartment(@RequestBody DepartmentDto departmentDto) {
        DepartmentDto createdDept = departmentService.createDepartment(departmentDto);
        return new ResponseEntity<>(createdDept, HttpStatus.CREATED);
    }

    @PutMapping("/departments/{id}")
    public ResponseEntity<DepartmentDto> updateDepartment(@PathVariable Long id, @RequestBody DepartmentDto departmentDto) {
        DepartmentDto updatedDept = departmentService.updateDepartment(id, departmentDto);
        return ResponseEntity.ok(updatedDept);
    }

    @DeleteMapping("/departments/{id}")
    public ResponseEntity<Void> deleteDepartment(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/departments/tree")
    public ResponseEntity<List<DepartmentDto>> getDepartmentTree() {
        return ResponseEntity.ok(departmentService.getDepartmentTree());
    }
    // --- 【新增结束】 ---


    // --- 组织架构与数据源 ---
    @GetMapping("/organization-tree")
    @PreAuthorize("isAuthenticated()")
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

    // --- 日志管理 ---
    @GetMapping("/logs/login")
    public ResponseEntity<Page<LoginLogDto>> getLoginLogs(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            Pageable pageable) {
        return ResponseEntity.ok(adminService.getLoginLogs(userId, status, startTime, endTime, pageable));
    }

    @GetMapping("/logs/operation")
    public ResponseEntity<Page<OperationLogDto>> getOperationLogs(
            @RequestParam(required = false) String operatorId,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            Pageable pageable) {
        return ResponseEntity.ok(adminService.getOperationLogs(operatorId, module, startTime, endTime, pageable));
    }
}