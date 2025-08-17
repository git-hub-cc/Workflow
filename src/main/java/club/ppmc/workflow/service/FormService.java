package club.ppmc.workflow.service;

import club.ppmc.workflow.domain.FileAttachment;
import club.ppmc.workflow.domain.FormDefinition;
import club.ppmc.workflow.domain.FormSubmission;
import club.ppmc.workflow.domain.WorkflowInstance;
import club.ppmc.workflow.dto.CreateFormDefinitionRequest;
import club.ppmc.workflow.dto.CreateFormSubmissionRequest;
import club.ppmc.workflow.dto.FormDefinitionResponse;
import club.ppmc.workflow.dto.FormSubmissionResponse;
import club.ppmc.workflow.exception.ResourceNotFoundException;
import club.ppmc.workflow.repository.FileAttachmentRepository;
import club.ppmc.workflow.repository.FormDefinitionRepository;
import club.ppmc.workflow.repository.FormSubmissionRepository;
import club.ppmc.workflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author 你的名字
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
    // --- 【新增】 ---
    private final FileAttachmentRepository fileAttachmentRepository;
    private final FileService fileService;

    // ... [createFormDefinition, getAllFormDefinitions, getFormDefinitionById methods remain unchanged] ...

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
     * 为指定的表单创建一条提交记录，并可能启动工作流。
     * @param formDefinitionId 表单定义 ID
     * @param request 包含提交数据和附件ID的 DTO
     * @param submitterId 提交人 ID
     * @return 创建后的提交记录响应 DTO
     */
    public FormSubmissionResponse createFormSubmission(Long formDefinitionId, CreateFormSubmissionRequest request, String submitterId) {
        FormDefinition formDefinition = formDefinitionRepository.findById(formDefinitionId)
                .orElseThrow(() -> new ResourceNotFoundException("无法为不存在的表单 (ID: " + formDefinitionId + ") 创建提交记录"));

        FormSubmission submission = new FormSubmission();
        submission.setFormDefinition(formDefinition);
        submission.setDataJson(request.getDataJson());
        submission.setSubmitterId(submitterId);

        FormSubmission savedSubmission = formSubmissionRepository.save(submission);

        // --- 【核心修改：关联附件】 ---
        if (!CollectionUtils.isEmpty(request.getAttachmentIds())) {
            List<FileAttachment> attachments = fileAttachmentRepository.findAllById(request.getAttachmentIds());
            for (FileAttachment attachment : attachments) {
                // 将附件与此次提交关联起来
                attachment.setFormSubmission(savedSubmission);
            }
            // JPA 会自动处理更新
            fileAttachmentRepository.saveAll(attachments);
        }
        // --- 【修改结束】 ---

        // 尝试启动工作流
        workflowService.startWorkflow(savedSubmission);

        // 重新从数据库获取最新的 submission 状态
        FormSubmission finalSubmission = formSubmissionRepository.findById(savedSubmission.getId())
                .orElseThrow(() -> new IllegalStateException("刚保存的提交记录找不到了")); // 理论上不会发生
        return convertToSubmissionResponse(finalSubmission);
    }

    /**
     * 根据表单 ID 获取其所有提交记录
     * @param formDefinitionId 表单定义 ID
     * @return 该表单的所有提交记录列表
     */
    @Transactional(readOnly = true)
    public List<FormSubmissionResponse> getSubmissionsByFormId(Long formDefinitionId) {
        if (!formDefinitionRepository.existsById(formDefinitionId)) {
            throw new ResourceNotFoundException("未找到 ID 为 " + formDefinitionId + " 的表单定义");
        }
        return formSubmissionRepository.findByFormDefinitionId(formDefinitionId).stream()
                .map(this::convertToSubmissionResponse)
                .collect(Collectors.toList());
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

        // --- 【新增：转换附件信息】 ---
        List<FileAttachment> attachments = fileAttachmentRepository.findByFormSubmissionId(entity.getId());
        if (!attachments.isEmpty()) {
            dto.setAttachments(fileService.getFilesBySubmissionId(entity.getId()));
        }

        // 设置提交人姓名
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