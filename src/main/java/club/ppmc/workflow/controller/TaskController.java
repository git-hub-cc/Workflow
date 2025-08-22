package club.ppmc.workflow.controller;

import club.ppmc.workflow.dto.CompleteTaskRequest;
import club.ppmc.workflow.dto.CompletedTaskDto;
import club.ppmc.workflow.dto.TaskDto;
import club.ppmc.workflow.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

/**
 * @author cc
 * @description 任务相关的 RESTful API 控制器
 */
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TaskController {

    private final WorkflowService workflowService;

    /**
     * API: 获取当前登录用户的待办任务列表，支持分页和搜索
     * 权限: 已认证用户
     */
    @GetMapping("/pending")
    public ResponseEntity<Page<TaskDto>> getPendingTasks(
            Principal principal,
            @RequestParam(required = false) String keyword,
            Pageable pageable) {
        String assigneeId = principal.getName();
        Page<TaskDto> tasks = workflowService.getPendingTasksForUser(assigneeId, keyword, pageable);
        return ResponseEntity.ok(tasks);
    }

    /**
     * 【新增】API: 获取当前登录用户的已办任务列表
     * 权限: 已认证用户
     */
    @GetMapping("/completed")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<CompletedTaskDto>> getCompletedTasks(
            Principal principal,
            @RequestParam(required = false) String keyword,
            Pageable pageable) {
        String assigneeId = principal.getName();
        Page<CompletedTaskDto> tasks = workflowService.getCompletedTasksForUser(assigneeId, keyword, pageable);
        return ResponseEntity.ok(tasks);
    }


    /**
     * API: 获取单个任务的详情
     * 权限: 仅限任务办理人或管理员
     * SpEL: 调用 workflowService 的 isTaskOwner 方法进行校验
     */
    @GetMapping("/{camundaTaskId}")
    @PreAuthorize("@workflowService.isTaskOwner(#camundaTaskId, principal.username) or hasRole('ADMIN')")
    public ResponseEntity<TaskDto> getTaskById(@PathVariable String camundaTaskId) {
        TaskDto task = workflowService.getTaskDetails(camundaTaskId);
        return ResponseEntity.ok(task);
    }

    /**
     * API: 完成一个任务
     * 权限: 仅限任务办理人
     */
    @PostMapping("/{camundaTaskId}/complete")
    @PreAuthorize("@workflowService.isTaskOwner(#camundaTaskId, principal.username)")
    public ResponseEntity<Void> completeTask(@PathVariable String camundaTaskId,
                                             @RequestBody CompleteTaskRequest request) {
        workflowService.completeUserTask(camundaTaskId, request);
        return ResponseEntity.ok().build();
    }
}