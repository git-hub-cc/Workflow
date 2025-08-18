package club.ppmc.workflow.controller;

import club.ppmc.workflow.domain.FormSubmission;
import club.ppmc.workflow.domain.User;
import club.ppmc.workflow.domain.UserGroup;
import club.ppmc.workflow.dto.*;
import club.ppmc.workflow.repository.FormSubmissionRepository;
import club.ppmc.workflow.repository.UserGroupRepository;
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
 * @author cc
 * @description 工作流相关的 RESTful API 控制器
 */
@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WorkflowController {

    private final WorkflowService workflowService;
    private final UserRepository userRepository;
    private final UserGroupRepository userGroupRepository;
    private final FormSubmissionRepository formSubmissionRepository;
    private final FormService formService;

    // --- 流程定义与部署 ---
    @PostMapping("/deploy")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deployWorkflow(@RequestBody DeployWorkflowRequest request) {
        workflowService.deployWorkflow(request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/templates/{formId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WorkflowTemplateResponse> updateWorkflowTemplate(
            @PathVariable Long formId,
            @RequestBody UpdateWorkflowTemplateRequest request) {
        WorkflowTemplateResponse response = workflowService.updateWorkflowTemplate(formId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/templates")
    public ResponseEntity<WorkflowTemplateResponse> getTemplateByFormId(@RequestParam Long formId) {
        WorkflowTemplateResponse templateResponse = workflowService.getOrCreateWorkflowTemplate(formId);
        return ResponseEntity.ok(templateResponse);
    }

    // --- 【核心重构】流程设计器所需的数据源 ---

    /**
     * API: 获取用户列表 (专门用于前端选择器，如审批人选择)
     * 权限: 【安全修复】已将权限从 isAuthenticated() 收紧为 hasRole('ADMIN')，防止信息泄露
     */
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserPickerDto>> getUsersForPicker() {
        List<User> users = userRepository.findAll();
        List<UserPickerDto> userDtos = users.stream().map(this::toUserPickerDto).collect(Collectors.toList());
        return ResponseEntity.ok(userDtos);
    }

    /**
     * API: 获取所有用户组列表 (用于流程设计器选择候选组)
     * 权限: 已认证用户
     */
    @GetMapping("/groups")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UserGroupDto>> getAllGroupsForWorkflow() {
        List<UserGroup> groups = userGroupRepository.findAll();
        List<UserGroupDto> groupDtos = groups.stream().map(this::toUserGroupDto).collect(Collectors.toList());
        return ResponseEntity.ok(groupDtos);
    }

    // --- 个人流程数据 ---
    @GetMapping("/my-submissions")
    public ResponseEntity<List<FormSubmissionResponse>> getMySubmissions(Principal principal) {
        String userId = principal.getName();
        List<FormSubmission> submissions = formSubmissionRepository.findBySubmitterIdOrderByCreatedAtDesc(userId);
        List<FormSubmissionResponse> responses = submissions.stream()
                .map(formService::convertToSubmissionResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/history/{submissionId}")
    @PreAuthorize("@workflowService.isSubmissionOwner(#submissionId, principal.username) " +
            "or @workflowService.isTaskAssigneeForSubmission(#submissionId, principal.username) " +
            "or hasRole('ADMIN')")
    public ResponseEntity<List<HistoryActivityDto>> getWorkflowHistory(@PathVariable Long submissionId) {
        List<HistoryActivityDto> history = workflowService.getWorkflowHistoryBySubmissionId(submissionId);
        return ResponseEntity.ok(history);
    }


    // --- DTO 转换器 ---

    /**
     * 将 User 实体转换为专门用于选择器的 UserPickerDto
     */
    private UserPickerDto toUserPickerDto(User user) {
        UserPickerDto dto = new UserPickerDto();
        dto.setId(user.getId());
        dto.setName(user.getName());
        return dto;
    }

    private UserGroupDto toUserGroupDto(UserGroup group) {
        UserGroupDto dto = new UserGroupDto();
        dto.setId(group.getId());
        dto.setName(group.getName());
        dto.setDescription(group.getDescription());
        return dto;
    }
}