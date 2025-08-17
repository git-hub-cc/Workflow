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
    private final UserGroupRepository userGroupRepository; // 【新增】
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
     * API: 更新工作流模板（仅保存，不部署）
     * @param formId 表单ID
     * @param request 包含BPMN XML和流程Key的请求体
     * @return 更新后的模板信息
     */
    @PutMapping("/templates/{formId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WorkflowTemplateResponse> updateWorkflowTemplate(
            @PathVariable Long formId,
            @RequestBody UpdateWorkflowTemplateRequest request) {
        WorkflowTemplateResponse response = workflowService.updateWorkflowTemplate(formId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * API: 根据表单ID获取其工作流模板
     * 权限: 已认证用户
     */
    @GetMapping("/templates")
    public ResponseEntity<WorkflowTemplateResponse> getTemplateByFormId(@RequestParam Long formId) {
        WorkflowTemplateResponse templateResponse = workflowService.getOrCreateWorkflowTemplate(formId);
        return ResponseEntity.ok(templateResponse);
    }

    /**
     * API: 获取所有用户列表 (用于在前端下拉选择审批人)
     * 权限: 已认证用户
     */
    @GetMapping("/users")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<User> users = userRepository.findAll();
        // 【修改】使用新的转换逻辑
        List<UserDto> userDtos = users.stream().map(this::toUserDto).collect(Collectors.toList());
        return ResponseEntity.ok(userDtos);
    }

    /**
     * 【新增】API: 获取所有用户组列表 (用于流程设计器选择候选组)
     * 权限: 已认证用户
     */
    @GetMapping("/groups")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UserGroupDto>> getAllGroupsForWorkflow() {
        List<UserGroup> groups = userGroupRepository.findAll();
        List<UserGroupDto> groupDtos = groups.stream().map(this::toUserGroupDto).collect(Collectors.toList());
        return ResponseEntity.ok(groupDtos);
    }

    /**
     * 将 User 实体转换为 UserDto
     * @param user User 实体
     * @return UserDto
     */
    private UserDto toUserDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setDepartment(user.getDepartment());
        if (user.getManager() != null) {
            dto.setManagerId(user.getManager().getId());
        }
        if (user.getRoles() != null) {
            dto.setRoleNames(user.getRoles().stream()
                    .map(role -> role.getName()) // 【修正】直接获取角色名
                    .collect(Collectors.toList()));
        }
        // 【新增】转换用户组信息
        if (user.getUserGroups() != null) {
            dto.setGroupNames(user.getUserGroups().stream().map(UserGroup::getName).collect(Collectors.toList()));
        }
        return dto;
    }

    /**
     * 【新增】将 UserGroup 实体转换为 UserGroupDto
     */
    private UserGroupDto toUserGroupDto(UserGroup group) {
        UserGroupDto dto = new UserGroupDto();
        dto.setId(group.getId());
        dto.setName(group.getName());
        dto.setDescription(group.getDescription());
        return dto;
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
     * 权限: 仅限提交人、流程参与者或管理员
     */
    @GetMapping("/history/{submissionId}")
    @PreAuthorize("@workflowService.isSubmissionOwner(#submissionId, principal.username) " +
            "or @workflowService.isTaskAssigneeForSubmission(#submissionId, principal.username) " +
            "or hasRole('ADMIN')")
    public ResponseEntity<List<HistoryActivityDto>> getWorkflowHistory(@PathVariable Long submissionId) {
        List<HistoryActivityDto> history = workflowService.getWorkflowHistoryBySubmissionId(submissionId);
        return ResponseEntity.ok(history);
    }
}