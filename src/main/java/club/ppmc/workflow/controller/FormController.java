package club.ppmc.workflow.controller;

import club.ppmc.workflow.domain.FormSubmission;
import club.ppmc.workflow.dto.CreateFormDefinitionRequest;
import club.ppmc.workflow.dto.CreateFormSubmissionRequest;
import club.ppmc.workflow.dto.FormDefinitionResponse;
import club.ppmc.workflow.dto.FormSubmissionResponse;
import club.ppmc.workflow.exception.ResourceNotFoundException;
import club.ppmc.workflow.repository.FormSubmissionRepository;
import club.ppmc.workflow.service.FormService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author 你的名字
 * @description 表单相关的 RESTful API 控制器
 */
@RestController
@RequestMapping("/api/forms")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // 允许所有来源的跨域请求，方便前后端分离开发
public class FormController {

    private final FormService formService;
    private final FormSubmissionRepository formSubmissionRepository;

    /**
     * API: 创建一个新的表单定义
     * METHOD: POST
     * PATH: /api/forms
     */
    @PostMapping
    public ResponseEntity<FormDefinitionResponse> createFormDefinition(@RequestBody CreateFormDefinitionRequest request) {
        FormDefinitionResponse response = formService.createFormDefinition(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * API: 获取所有表单定义的列表
     * METHOD: GET
     * PATH: /api/forms
     */
    @GetMapping
    public ResponseEntity<List<FormDefinitionResponse>> getAllFormDefinitions() {
        List<FormDefinitionResponse> responses = formService.getAllFormDefinitions();
        return ResponseEntity.ok(responses);
    }

    /**
     * API: 根据 ID 获取单个表单定义
     * METHOD: GET
     * PATH: /api/forms/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<FormDefinitionResponse> getFormDefinitionById(@PathVariable Long id) {
        FormDefinitionResponse response = formService.getFormDefinitionById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * API: 为指定表单提交一份数据
     * METHOD: POST
     * PATH: /api/forms/{formId}/submissions
     * @param submitterId 提交人ID，为了简化从请求头获取
     */
    @PostMapping("/{formId}/submissions")
    public ResponseEntity<FormSubmissionResponse> createFormSubmission(
            @PathVariable Long formId,
            @RequestBody CreateFormSubmissionRequest request,
            @RequestHeader("X-User-ID") String submitterId) {

        if (submitterId == null || submitterId.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        FormSubmissionResponse response = formService.createFormSubmission(formId, request, submitterId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * API: 获取指定表单的所有提交数据
     * METHOD: GET
     * PATH: /api/forms/{formId}/submissions
     */
    @GetMapping("/{formId}/submissions")
    public ResponseEntity<List<FormSubmissionResponse>> getSubmissionsByFormId(@PathVariable Long formId) {
        List<FormSubmissionResponse> responses = formService.getSubmissionsByFormId(formId);
        return ResponseEntity.ok(responses);
    }

    /**
     * API: 根据提交ID获取单个提交数据详情
     * METHOD: GET
     * PATH: /api/forms/submissions/{submissionId}
     */
    @GetMapping("/submissions/{submissionId}")
    public ResponseEntity<FormSubmissionResponse> getSubmissionById(@PathVariable Long submissionId) {
        FormSubmission submission = formSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("未找到提交记录 ID: " + submissionId));
        return ResponseEntity.ok(formService.convertToSubmissionResponse(submission));
    }
}