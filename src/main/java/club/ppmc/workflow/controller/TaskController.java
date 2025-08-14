package club.ppmc.workflow.controller;

import club.ppmc.workflow.dto.CompleteTaskRequest;
import club.ppmc.workflow.dto.TaskDto;
import club.ppmc.workflow.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author 你的名字
 * @description 任务相关的 RESTful API 控制器
 */
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TaskController {

    private final WorkflowService workflowService;

    /**
     * API: 获取指定用户的待办任务列表
     * METHOD: GET
     * PATH: /api/tasks/pending?assigneeId={userId}
     */
    @GetMapping("/pending")
    public ResponseEntity<List<TaskDto>> getPendingTasks(@RequestParam String assigneeId) {
        List<TaskDto> tasks = workflowService.getPendingTasksForUser(assigneeId);
        return ResponseEntity.ok(tasks);
    }

    /**
     * API: 获取单个任务的详情
     * METHOD: GET
     * PATH: /api/tasks/{camundaTaskId}
     */
    @GetMapping("/{camundaTaskId}")
    public ResponseEntity<TaskDto> getTaskById(@PathVariable String camundaTaskId) {
        TaskDto task = workflowService.getTaskDetails(camundaTaskId);
        return ResponseEntity.ok(task);
    }

    /**
     * API: 完成一个任务
     * METHOD: POST
     * PATH: /api/tasks/{camundaTaskId}/complete
     */
    @PostMapping("/{camundaTaskId}/complete")
    public ResponseEntity<Void> completeTask(@PathVariable String camundaTaskId,
                                             @RequestBody CompleteTaskRequest request) {
        // 在真实项目中, 这里需要进行权限校验，确保当前用户是该任务的办理人
        workflowService.completeUserTask(camundaTaskId, request);
        return ResponseEntity.ok().build();
    }
}