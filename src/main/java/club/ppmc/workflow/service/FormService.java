package club.ppmc.workflow.service;

import club.ppmc.workflow.aop.LogOperation;
import club.ppmc.workflow.domain.FileAttachment;
import club.ppmc.workflow.domain.FormDefinition;
import club.ppmc.workflow.domain.FormSubmission;
import club.ppmc.workflow.domain.WorkflowInstance;
import club.ppmc.workflow.dto.CreateFormDefinitionRequest;
import club.ppmc.workflow.dto.CreateFormSubmissionRequest;
import club.ppmc.workflow.dto.FormDefinitionResponse;
import club.ppmc.workflow.dto.FormSubmissionResponse;
import club.ppmc.workflow.exception.ResourceNotFoundException;
import club.ppmc.workflow.repository.*;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author cc
 * @description 表单相关的业务逻辑服务
 */
@Service("appFormService")
@RequiredArgsConstructor
@Transactional
public class FormService {

    private final FormDefinitionRepository formDefinitionRepository;
    private final FormSubmissionRepository formSubmissionRepository;
    private final WorkflowService workflowService;
    private final UserRepository userRepository;
    private final FileAttachmentRepository fileAttachmentRepository;
    private final FileService fileService;
    private final MenuRepository menuRepository; // 【新增】
    private final WorkflowTemplateRepository workflowTemplateRepository; // 【新增】

    @LogOperation(module = "表单管理", action = "创建表单定义", targetIdExpression = "#result.name")
    public FormDefinitionResponse createFormDefinition(CreateFormDefinitionRequest request) {
        FormDefinition formDefinition = new FormDefinition();
        formDefinition.setName(request.getName());
        formDefinition.setSchemaJson(request.getSchemaJson());

        FormDefinition savedForm = formDefinitionRepository.save(formDefinition);
        return convertToDefinitionResponse(savedForm);
    }

    @Transactional(readOnly = true)
    public List<FormDefinitionResponse> getAllFormDefinitions() {
        return formDefinitionRepository.findAll().stream()
                .map(this::convertToDefinitionResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FormDefinitionResponse getFormDefinitionById(Long id) {
        FormDefinition formDefinition = formDefinitionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("未找到 ID 为 " + id + " 的表单定义"));
        return convertToDefinitionResponse(formDefinition);
    }

    /**
     * 【新增】删除一个表单定义
     * @param id 表单定义ID
     */
    @LogOperation(module = "表单管理", action = "删除表单定义", targetIdExpression = "#id")
    public void deleteFormDefinition(Long id) {
        // 1. 检查表单是否存在
        if (!formDefinitionRepository.existsById(id)) {
            throw new ResourceNotFoundException("未找到表单定义 ID: " + id);
        }
        // 2. 检查是否有工作流模板关联
        if (workflowTemplateRepository.findByFormDefinitionId(id).isPresent()) {
            throw new IllegalStateException("无法删除：该表单已关联一个工作流模板。请先在流程设计器中解除关联或删除流程。");
        }
        // 3. 检查是否有菜单项关联
        if (menuRepository.existsByFormDefinitionId(id)) {
            throw new IllegalStateException("无法删除：该表单已被一个或多个菜单项使用。请先在菜单管理中解除关联。");
        }
        // 4. (可选) 检查是否已有提交数据
        // if (formSubmissionRepository.findByFormDefinitionId(id).count() > 0) { ... }

        // 5. 执行删除
        formDefinitionRepository.deleteById(id);
    }

    /**
     * 为指定的表单创建一条提交记录，并可能启动工作流。
     * @param formDefinitionId 表单定义 ID
     * @param request 包含提交数据和附件ID的 DTO
     * @param submitterId 提交人 ID
     * @return 创建后的提交记录响应 DTO
     */
    @LogOperation(module = "表单提交", action = "提交申请", targetIdExpression = "#result.id")
    public FormSubmissionResponse createFormSubmission(Long formDefinitionId, CreateFormSubmissionRequest request, String submitterId) {
        FormDefinition formDefinition = formDefinitionRepository.findById(formDefinitionId)
                .orElseThrow(() -> new ResourceNotFoundException("无法为不存在的表单 (ID: " + formDefinitionId + ") 创建提交记录"));

        FormSubmission submission = new FormSubmission();
        submission.setFormDefinition(formDefinition);
        submission.setDataJson(request.getDataJson());
        submission.setSubmitterId(submitterId);

        FormSubmission savedSubmission = formSubmissionRepository.save(submission);

        if (!CollectionUtils.isEmpty(request.getAttachmentIds())) {
            List<FileAttachment> attachments = fileAttachmentRepository.findAllById(request.getAttachmentIds());
            for (FileAttachment attachment : attachments) {
                attachment.setFormSubmission(savedSubmission);
                // --- 【数据不一致修复】将文件状态从 TEMPORARY 更新为 LINKED ---
                attachment.setStatus(FileAttachment.FileStatus.LINKED);
            }
            fileAttachmentRepository.saveAll(attachments);
        }

        workflowService.startWorkflow(savedSubmission);

        FormSubmission finalSubmission = formSubmissionRepository.findById(savedSubmission.getId())
                .orElseThrow(() -> new IllegalStateException("刚保存的提交记录找不到了")); // 理论上不会发生
        return convertToSubmissionResponse(finalSubmission);
    }

    /**
     * 【修改】根据表单 ID 和动态查询条件获取其所有提交记录
     * @param formDefinitionId 表单定义 ID
     * @param filters 查询过滤器
     * @param pageable 分页信息
     * @return 分页后的提交记录列表
     */
    @Transactional(readOnly = true)
    public Page<FormSubmissionResponse> getSubmissionsByFormId(Long formDefinitionId, Map<String, String> filters, Pageable pageable) {
        if (!formDefinitionRepository.existsById(formDefinitionId)) {
            throw new ResourceNotFoundException("未找到 ID 为 " + formDefinitionId + " 的表单定义");
        }

        Specification<FormSubmission> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("formDefinition").get("id"), formDefinitionId));

            if (filters != null) {
                filters.forEach((key, value) -> {
                    if (StringUtils.hasText(value)) {
                        // 示例：可以根据特定字段进行查询，如提交人
                        if ("submitterId".equalsIgnoreCase(key)) {
                            predicates.add(cb.like(root.get("submitterId"), "%" + value + "%"));
                        }
                        // 更多查询条件可在此扩展...
                    }
                });
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return formSubmissionRepository.findAll(spec, pageable).map(this::convertToSubmissionResponse);
    }


    public FormDefinitionResponse convertToDefinitionResponse(FormDefinition entity) {
        FormDefinitionResponse dto = new FormDefinitionResponse();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setSchemaJson(entity.getSchemaJson());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public FormSubmissionResponse convertToSubmissionResponse(FormSubmission entity) {
        FormSubmissionResponse dto = new FormSubmissionResponse();
        dto.setId(entity.getId());
        dto.setFormDefinitionId(entity.getFormDefinition().getId());
        dto.setFormName(entity.getFormDefinition().getName()); // 设置表单名称
        dto.setDataJson(entity.getDataJson());
        dto.setCreatedAt(entity.getCreatedAt());

        List<FileAttachment> attachments = fileAttachmentRepository.findByFormSubmissionId(entity.getId());
        if (!attachments.isEmpty()) {
            dto.setAttachments(fileService.getFilesBySubmissionId(entity.getId()));
        }

        userRepository.findById(entity.getSubmitterId())
                .ifPresent(user -> dto.setSubmitterName(user.getName()));

        WorkflowInstance instance = entity.getWorkflowInstance();
        if (instance == null) {
            dto.setWorkflowStatus("无流程");
        } else {
            dto.setWorkflowStatus(switch (instance.getStatus()) {
                case PROCESSING -> "审批中";
                case APPROVED -> "已通过";
                case REJECTED -> "已拒绝";
            });
        }
        return dto;
    }
}