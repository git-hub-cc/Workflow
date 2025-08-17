package club.ppmc.workflow.controller;

import club.ppmc.workflow.domain.FormSubmission;
import club.ppmc.workflow.dto.CreateFormDefinitionRequest;
import club.ppmc.workflow.dto.CreateFormSubmissionRequest;
import club.ppmc.workflow.dto.FormDefinitionResponse;
import club.ppmc.workflow.dto.FormSubmissionResponse;
import club.ppmc.workflow.exception.ResourceNotFoundException;
import club.ppmc.workflow.repository.FormSubmissionRepository;
import club.ppmc.workflow.service.FormService;
import club.ppmc.workflow.service.WordImportService; // 【新增】导入
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile; // 【新增】导入

import java.io.IOException; // 【新增】导入
import java.security.Principal;
import java.util.List;

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
    private final WordImportService wordImportService; // 【新增】注入服务

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
     * 【新增】API: 从 Word 文档导入并创建表单定义
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

    // ... 其他方法保持不变 ...

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
     * API: 获取指定表单的所有提交数据
     * 权限: 已认证用户
     */
    @GetMapping("/{formId}/submissions")
    public ResponseEntity<List<FormSubmissionResponse>> getSubmissionsByFormId(@PathVariable Long formId) {
        List<FormSubmissionResponse> responses = formService.getSubmissionsByFormId(formId);
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
}