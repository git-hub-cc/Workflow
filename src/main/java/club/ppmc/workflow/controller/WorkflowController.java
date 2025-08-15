package club.ppmc.workflow.controller;

import club.ppmc.workflow.domain.FormSubmission;
import club.ppmc.workflow.domain.User;
import club.ppmc.workflow.dto.*;
import club.ppmc.workflow.repository.FormSubmissionRepository;
import club.ppmc.workflow.repository.UserRepository;
import club.ppmc.workflow.repository.WorkflowTemplateRepository;
import club.ppmc.workflow.service.FormService;
import club.ppmc.workflow.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author 你的名字
 * @description 工作流相关的 RESTful API 控制器
 */
@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WorkflowController {

    private final WorkflowService workflowService;
    private final UserRepository userRepository;
    private final WorkflowTemplateRepository templateRepository;
    private final FormSubmissionRepository formSubmissionRepository;
    private final FormService formService;

    /**
     * API: 部署一个新的工作流模板
     * 权限: 仅限管理员
     */
    @PostMapping("/deploy")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deployWorkflow(@RequestBody DeployWorkflowRequest request) {
        workflowService.deployWorkflow(request);
        return ResponseEntity.ok().build();
    }

    /**
     * API: 根据表单ID获取其工作流模板
     * 权限: 已认证用户
     */
    @GetMapping("/templates")
    public ResponseEntity<WorkflowTemplateResponse> getTemplateByFormId(@RequestParam Long formId) {
        return templateRepository.findByFormDefinitionId(formId)
                .map(template -> {
                    WorkflowTemplateResponse dto = new WorkflowTemplateResponse();
                    dto.setFormDefinitionId(template.getFormDefinition().getId());
                    dto.setBpmnXml(template.getBpmnXml());
                    dto.setProcessDefinitionKey(template.getProcessDefinitionKey());
                    return ResponseEntity.ok(dto);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * API: 获取所有用户列表 (用于在前端下拉选择审批人)
     * 权限: 已认证用户
     */
    @GetMapping("/users")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<User> users = userRepository.findAll();
        List<UserDto> userDtos = users.stream().map(user -> {
            UserDto dto = new UserDto();
            dto.setId(user.getId());
            dto.setName(user.getName());
            dto.setRole(user.getRole());
            return dto;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(userDtos);
    }

    /**
     * API: 获取当前用户提交的所有申请
     * 权限: 已认证用户
     */
    @GetMapping("/my-submissions")
    public ResponseEntity<List<FormSubmissionResponse>> getMySubmissions(Principal principal) {
        String userId = principal.getName();
        List<FormSubmission> submissions = formSubmissionRepository.findBySubmitterIdOrderByCreatedAtDesc(userId);
        List<FormSubmissionResponse> responses = submissions.stream()
                .map(formService::convertToSubmissionResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    /**
     * API: 根据业务ID（FormSubmission ID）获取工作流实例的历史记录
     * 权限: 仅限提交人或管理员
     */
    @GetMapping("/history/{submissionId}")
    @PreAuthorize("@workflowService.isSubmissionOwner(#submissionId, principal.username) or hasRole('ADMIN')")
    public ResponseEntity<List<HistoryActivityDto>> getWorkflowHistory(@PathVariable Long submissionId) {
        List<HistoryActivityDto> history = workflowService.getWorkflowHistoryBySubmissionId(submissionId);
        return ResponseEntity.ok(history);
    }
}