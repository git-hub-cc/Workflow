package club.ppmc.workflow.controller;

import club.ppmc.workflow.domain.User;
import club.ppmc.workflow.domain.UserGroup;
import club.ppmc.workflow.dto.*;
import club.ppmc.workflow.repository.UserGroupRepository;
import club.ppmc.workflow.repository.UserRepository;
import club.ppmc.workflow.service.FormService;
import club.ppmc.workflow.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
     * 【核心修改】API: 获取用户列表 (专门用于前端选择器，如审批人选择)
     * - 权限: 【安全修复】已将权限从 isAuthenticated() 收紧为 hasRole('ADMIN')
     * - 功能: 【优化】支持分页和按关键词搜索，避免一次性加载大量用户数据
     */
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserPickerDto>> getUsersForPicker(
            @RequestParam(required = false) String keyword,
            Pageable pageable) {
        Page<User> userPage = userRepository.findByIdContainingIgnoreCaseOrNameContainingIgnoreCaseOrderByNameAsc(keyword, keyword, pageable);
        Page<UserPickerDto> dtoPage = userPage.map(this::toUserPickerDto);
        return ResponseEntity.ok(dtoPage);
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
    /**
     * 【核心修改】API: 获取我的申请列表, 支持分页和筛选
     */
    @GetMapping("/my-submissions")
    public ResponseEntity<Page<FormSubmissionResponse>> getMySubmissions(
            Principal principal,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        String userId = principal.getName();
        Page<FormSubmissionResponse> responses = formService.getMySubmissions(userId, keyword, status, pageable);
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


    // --- 【核心新增】获取流程图 API ---
    /**
     * API: 获取指定申请的流程图 BPMN XML
     * 权限: 仅限申请人、历史或当前处理人、管理员
     * @param submissionId 申请提交ID
     * @return 包含BPMN XML的DTO
     */
    @GetMapping("/submission/{submissionId}/diagram")
    @PreAuthorize("@workflowService.isSubmissionOwner(#submissionId, principal.username) " +
            "or @workflowService.isTaskAssigneeForSubmission(#submissionId, principal.username) " +
            "or hasRole('ADMIN')")
    public ResponseEntity<BpmnXmlDto> getWorkflowDiagram(@PathVariable Long submissionId) {
        return ResponseEntity.ok(workflowService.getWorkflowDiagram(submissionId));
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