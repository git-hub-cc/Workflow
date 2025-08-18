package club.ppmc.workflow.controller;

import club.ppmc.workflow.domain.FormSubmission;
import club.ppmc.workflow.dto.*;
import club.ppmc.workflow.exception.ResourceNotFoundException;
import club.ppmc.workflow.repository.FormSubmissionRepository;
import club.ppmc.workflow.service.FormService;
import club.ppmc.workflow.service.WordImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * @author cc
 * @description 表单相关的 RESTful API 控制器
 */
@RestController
@RequestMapping("/api/forms")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FormController {

    private final FormService formService;
    private final FormSubmissionRepository formSubmissionRepository;
    private final WordImportService wordImportService;

    /**
     * API: 创建一个新的表单定义
     * 权限: 仅限管理员
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FormDefinitionResponse> createFormDefinition(@RequestBody CreateFormDefinitionRequest request) {
        FormDefinitionResponse response = formService.createFormDefinition(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * API: 从 Word 文档导入并创建表单定义
     * 权限: 仅限管理员
     * @param file 上传的 .docx 文件
     * @return 解析后的表单定义响应 DTO
     * @throws IOException
     */
    @PostMapping("/import-word")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FormDefinitionResponse> importFromWord(@RequestParam("file") MultipartFile file) throws IOException {
        FormDefinitionResponse response = wordImportService.parseWordAndGenerateForm(file);
        return ResponseEntity.ok(response);
    }

    /**
     * API: 获取所有表单定义的列表
     * 权限: 已认证用户
     */
    @GetMapping
    public ResponseEntity<List<FormDefinitionResponse>> getAllFormDefinitions() {
        List<FormDefinitionResponse> responses = formService.getAllFormDefinitions();
        return ResponseEntity.ok(responses);
    }

    /**
     * API: 根据 ID 获取单个表单定义
     * 权限: 已认证用户
     */
    @GetMapping("/{id}")
    public ResponseEntity<FormDefinitionResponse> getFormDefinitionById(@PathVariable Long id) {
        FormDefinitionResponse response = formService.getFormDefinitionById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 【新增】API: 更新一个已存在的表单定义
     * 权限: 仅限管理员
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FormDefinitionResponse> updateFormDefinition(
            @PathVariable Long id,
            @RequestBody CreateFormDefinitionRequest request) {
        FormDefinitionResponse response = formService.updateFormDefinition(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * API: 删除一个表单定义
     * 权限: 仅限管理员
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteFormDefinition(@PathVariable Long id) {
        formService.deleteFormDefinition(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * API: 为指定表单提交一份数据
     * @param principal Spring Security 自动注入的当前用户信息
     * 权限: 已认证用户
     */
    @PostMapping("/{formId}/submissions")
    public ResponseEntity<FormSubmissionResponse> createFormSubmission(
            @PathVariable Long formId,
            @RequestBody CreateFormSubmissionRequest request,
            Principal principal) {
        // 从 Principal 安全地获取当前登录用户的ID
        String submitterId = principal.getName();
        FormSubmissionResponse response = formService.createFormSubmission(formId, request, submitterId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * API: 获取指定表单的所有提交数据 (支持分页、过滤和数据范围)
     * @param menuId (可选) 从哪个菜单入口访问，用于数据范围权限控制
     * 权限: 已认证用户
     */
    @GetMapping("/{formId}/submissions")
    public ResponseEntity<Page<FormSubmissionResponse>> getSubmissionsByFormId(
            @PathVariable Long formId,
            @RequestParam(required = false) Long menuId,
            @RequestParam(required = false) Map<String, String> filters,
            Pageable pageable) {
        Page<FormSubmissionResponse> responses = formService.getSubmissionsByFormId(formId, menuId, filters, pageable);
        return ResponseEntity.ok(responses);
    }

    /**
     * API: 根据提交ID获取单个提交数据详情
     * 权限: 已认证用户
     */
    @GetMapping("/submissions/{submissionId}")
    public ResponseEntity<FormSubmissionResponse> getSubmissionById(@PathVariable Long submissionId) {
        FormSubmission submission = formSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("未找到提交记录 ID: " + submissionId));
        return ResponseEntity.ok(formService.convertToSubmissionResponse(submission));
    }

    // 【新增】API: 更新指定的提交记录
    @PutMapping("/submissions/{submissionId}")
    @PreAuthorize("hasRole('ADMIN')") // 初始权限设置为仅管理员，可根据业务需求放宽
    public ResponseEntity<FormSubmissionResponse> updateSubmission(
            @PathVariable Long submissionId,
            @RequestBody UpdateFormSubmissionRequest request) {
        FormSubmissionResponse response = formService.updateSubmission(submissionId, request);
        return ResponseEntity.ok(response);
    }

    // 【新增】API: 删除指定的提交记录
    @DeleteMapping("/submissions/{submissionId}")
    @PreAuthorize("hasRole('ADMIN')") // 初始权限设置为仅管理员
    public ResponseEntity<Void> deleteSubmission(@PathVariable Long submissionId) {
        formService.deleteSubmission(submissionId);
        return ResponseEntity.noContent().build();
    }
}