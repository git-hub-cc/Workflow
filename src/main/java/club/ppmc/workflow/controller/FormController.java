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

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FormDefinitionResponse> createFormDefinition(@RequestBody CreateFormDefinitionRequest request) {
        FormDefinitionResponse response = formService.createFormDefinition(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/import-word")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FormDefinitionResponse> importFromWord(@RequestParam("file") MultipartFile file) throws IOException {
        FormDefinitionResponse response = wordImportService.parseWordAndGenerateForm(file);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<FormDefinitionResponse>> getAllFormDefinitions() {
        List<FormDefinitionResponse> responses = formService.getAllFormDefinitions();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FormDefinitionResponse> getFormDefinitionById(@PathVariable Long id) {
        FormDefinitionResponse response = formService.getFormDefinitionById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FormDefinitionResponse> updateFormDefinition(
            @PathVariable Long id,
            @RequestBody CreateFormDefinitionRequest request) {
        FormDefinitionResponse response = formService.updateFormDefinition(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 【核心修改】API: 删除一个表单定义，增加级联删除选项
     * 权限: 仅限管理员
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteFormDefinition(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean cascade) {
        formService.deleteFormDefinition(id, cascade);
        return ResponseEntity.noContent().build();
    }

    /**
     * 【核心新增】API: 检查表单的依赖关系
     * 权限: 仅限管理员
     */
    @GetMapping("/{id}/dependencies")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FormDependencyDto> getFormDependencies(@PathVariable Long id) {
        return ResponseEntity.ok(formService.getFormDependencies(id));
    }


    @PostMapping("/{formId}/submissions")
    public ResponseEntity<FormSubmissionResponse> createFormSubmission(
            @PathVariable Long formId,
            @RequestBody CreateFormSubmissionRequest request,
            Principal principal) {
        String submitterId = principal.getName();
        FormSubmissionResponse response = formService.createFormSubmission(formId, request, submitterId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{formId}/submissions")
    public ResponseEntity<Page<FormSubmissionResponse>> getSubmissionsByFormId(
            @PathVariable Long formId,
            @RequestParam(required = false) Long menuId,
            @RequestParam(required = false) Map<String, String> filters,
            Pageable pageable) {
        Page<FormSubmissionResponse> responses = formService.getSubmissionsByFormId(formId, menuId, filters, pageable);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/submissions/{submissionId}")
    public ResponseEntity<FormSubmissionResponse> getSubmissionById(@PathVariable Long submissionId) {
        FormSubmission submission = formSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("未找到提交记录 ID: " + submissionId));
        return ResponseEntity.ok(formService.convertToSubmissionResponse(submission));
    }

    @PutMapping("/submissions/{submissionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FormSubmissionResponse> updateSubmission(
            @PathVariable Long submissionId,
            @RequestBody UpdateFormSubmissionRequest request) {
        FormSubmissionResponse response = formService.updateSubmission(submissionId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/submissions/{submissionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteSubmission(@PathVariable Long submissionId) {
        formService.deleteSubmission(submissionId);
        return ResponseEntity.noContent().build();
    }
}