package club.ppmc.workflow.service;

import club.ppmc.workflow.aop.LogOperation;
import club.ppmc.workflow.domain.*;
import club.ppmc.workflow.dto.*;
import club.ppmc.workflow.exception.ResourceInUseException;
import club.ppmc.workflow.exception.ResourceNotFoundException;
import club.ppmc.workflow.exception.UnauthorizedException;
import club.ppmc.workflow.repository.*;
import club.ppmc.workflow.utils.FormSchemaParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author cc
 * @description 表单相关的业务逻辑服务
 */
@Service("appFormService") // 命名 Service Bean 以便在 @PreAuthorize 中使用
@RequiredArgsConstructor
@Transactional
@Slf4j
public class FormService {

    private final FormDefinitionRepository formDefinitionRepository;
    private final FormSubmissionRepository formSubmissionRepository;
    private final WorkflowService workflowService;
    private final UserRepository userRepository;
    private final FileAttachmentRepository fileAttachmentRepository;
    private final FileService fileService;
    private final MenuRepository menuRepository;
    private final WorkflowTemplateRepository workflowTemplateRepository;
    private final ObjectMapper objectMapper;

    @LogOperation(module = "表单提交", action = "提交申请", targetIdExpression = "#result?.id")
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

    @LogOperation(module = "表单管理", action = "更新表单定义", targetIdExpression = "#id")
    public FormDefinitionResponse updateFormDefinition(Long id, CreateFormDefinitionRequest request) {
        FormDefinition formDefinition = formDefinitionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("未找到 ID 为 " + id + " 的表单定义"));

        formDefinition.setName(request.getName());
        formDefinition.setSchemaJson(request.getSchemaJson());

        FormDefinition updatedForm = formDefinitionRepository.save(formDefinition);
        return convertToDefinitionResponse(updatedForm);
    }

    @LogOperation(module = "表单管理", action = "删除表单定义", targetIdExpression = "#id")
    public void deleteFormDefinition(Long id, boolean cascade) {
        if (!formDefinitionRepository.existsById(id)) {
            throw new ResourceNotFoundException("未找到表单定义 ID: " + id);
        }

        if (menuRepository.existsByFormDefinitionId(id)) {
            throw new ResourceInUseException("无法删除：该表单已被一个或多个菜单项使用。请先在菜单管理中解除关联。");
        }

        if (cascade) {
            log.warn("执行级联删除操作，表单ID: {}", id);
            workflowTemplateRepository.deleteByFormDefinitionId(id);
            formSubmissionRepository.deleteByFormDefinitionId(id);
        } else {
            if (workflowTemplateRepository.findByFormDefinitionId(id).isPresent()) {
                throw new ResourceInUseException("无法删除：该表单已关联一个工作流模板。请先在流程设计器中解除关联或删除流程。");
            }
            if (formSubmissionRepository.countByFormDefinitionId(id) > 0) {
                throw new ResourceInUseException("无法删除：该表单已存在提交数据。");
            }
        }

        formDefinitionRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public FormDependencyDto getFormDependencies(Long formId) {
        List<DependencyItemDto> dependencies = new ArrayList<>();

        workflowTemplateRepository.findByFormDefinitionId(formId).ifPresent(template ->
                dependencies.add(DependencyItemDto.builder()
                        .type(DependencyItemDto.DependencyType.WORKFLOW)
                        .name(StringUtils.hasText(template.getProcessDefinitionKey()) ? template.getProcessDefinitionKey() : "未命名工作流")
                        .id(template.getId())
                        .build())
        );

        List<Menu> relatedMenus = menuRepository.findByFormDefinitionId(formId);
        if (!relatedMenus.isEmpty()) {
            relatedMenus.forEach(menu ->
                    dependencies.add(DependencyItemDto.builder()
                            .type(DependencyItemDto.DependencyType.MENU)
                            .name(menu.getName())
                            .id(menu.getId())
                            .build())
            );
        }

        long submissionCount = formSubmissionRepository.countByFormDefinitionId(formId);
        if (submissionCount > 0) {
            dependencies.add(DependencyItemDto.builder()
                    .type(DependencyItemDto.DependencyType.SUBMISSION)
                    .name("提交数据")
                    .count(submissionCount)
                    .build());
        }

        return FormDependencyDto.builder()
                .canDelete(dependencies.isEmpty())
                .dependencies(dependencies)
                .build();
    }

    /**
     * 【核心新增】仅创建草稿，不启动工作流
     */
    @LogOperation(module = "表单提交", action = "保存草稿", targetIdExpression = "#result?.id")
    public FormSubmissionResponse createDraft(Long formDefinitionId, CreateFormSubmissionRequest request, String submitterId) {
        FormDefinition formDefinition = formDefinitionRepository.findById(formDefinitionId)
                .orElseThrow(() -> new ResourceNotFoundException("无法为不存在的表单 (ID: " + formDefinitionId + ") 创建提交记录"));

        FormSubmission submission = new FormSubmission();
        submission.setFormDefinition(formDefinition);
        submission.setDataJson(request.getDataJson());
        submission.setSubmitterId(submitterId);
        // 核心：状态强制为 DRAFT
        submission.setStatus(FormSubmission.SubmissionStatus.DRAFT);

        // 保存附件
        if (!CollectionUtils.isEmpty(request.getAttachmentIds())) {
            List<FileAttachment> attachments = fileAttachmentRepository.findAllById(request.getAttachmentIds());
            for (FileAttachment attachment : attachments) {
                attachment.setFormSubmission(submission);
                attachment.setStatus(FileAttachment.FileStatus.LINKED);
            }
            submission.setAttachments(attachments);
        }

        FormSubmission savedSubmission = formSubmissionRepository.save(submission);
        // 注意：这里不调用 workflowService.startWorkflow
        return convertToSubmissionResponse(savedSubmission);
    }

    /**
     * 【核心修改】创建表单提交记录并启动工作流。移除了创建草稿的逻辑。
     */
    @LogOperation(module = "表单提交", action = "创建并提交申请", targetIdExpression = "#result?.id")
    public FormSubmissionResponse createFormSubmission(Long formDefinitionId, CreateFormSubmissionRequest request, String submitterId) {
        FormDefinition formDefinition = formDefinitionRepository.findById(formDefinitionId)
                .orElseThrow(() -> new ResourceNotFoundException("无法为不存在的表单 (ID: " + formDefinitionId + ") 创建提交记录"));

        FormSubmission submission = new FormSubmission();
        submission.setFormDefinition(formDefinition);
        submission.setDataJson(request.getDataJson());
        submission.setSubmitterId(submitterId);

        // 保存附件
        if (!CollectionUtils.isEmpty(request.getAttachmentIds())) {
            List<FileAttachment> attachments = fileAttachmentRepository.findAllById(request.getAttachmentIds());
            for (FileAttachment attachment : attachments) {
                attachment.setFormSubmission(submission);
                attachment.setStatus(FileAttachment.FileStatus.LINKED);
            }
            submission.setAttachments(attachments);
        }

        String action = request.getInitialAction();

        // ⭐ 核心简化：此方法现在只处理需要立即启动工作流的场景
        if ("proceed".equals(action) || "terminate".equals(action)) {
            // 【核心修复】在启动工作流前，检查是否存在已部署的流程模板
            WorkflowTemplate template = workflowTemplateRepository.findByFormDefinitionId(formDefinitionId)
                    .orElseThrow(() -> new IllegalStateException(
                            "此表单未关联任何工作流，无法提交申请。请联系管理员。"));

            if (!StringUtils.hasText(template.getCamundaDeploymentId())) {
                throw new IllegalStateException(
                        "此表单关联的工作流尚未部署，无法提交申请。请联系管理员完成流程部署。");
            }
            // 【修复结束】

            submission.setStatus(FormSubmission.SubmissionStatus.PROCESSING);
            FormSubmission savedSubmission = formSubmissionRepository.save(submission);
            workflowService.startWorkflow(savedSubmission, action);
            return convertToSubmissionResponse(savedSubmission);
        } else {
            // 不再支持 'staging' 或其他隐式草稿创建
            throw new IllegalArgumentException("无效的初始动作: " + action);
        }
    }

    @LogOperation(module = "我的申请", action = "更新草稿", targetIdExpression = "#submissionId")
    public FormSubmissionResponse updateDraft(Long submissionId, UpdateFormSubmissionRequest request, String updaterId) {
        FormSubmission submission = formSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("未找到提交记录 ID: " + submissionId));

        if (!submission.getSubmitterId().equals(updaterId)) {
            throw new UnauthorizedException("您没有权限修改此草稿");
        }
        if (submission.getStatus() != FormSubmission.SubmissionStatus.DRAFT) {
            throw new IllegalStateException("只有草稿状态的申请才能被修改");
        }

        submission.setDataJson(request.getDataJson());
        updateAttachments(submission, request.getAttachmentIds());

        FormSubmission updatedSubmission = formSubmissionRepository.save(submission);
        return convertToSubmissionResponse(updatedSubmission);
    }

    @LogOperation(module = "我的申请", action = "提交草稿并发起流程", targetIdExpression = "#submissionId")
    public FormSubmissionResponse submitDraft(Long submissionId, CreateFormSubmissionRequest request, String submitterId) {
        FormSubmission submission = formSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("未找到提交记录 ID: " + submissionId));

        if (!submission.getSubmitterId().equals(submitterId)) {
            throw new UnauthorizedException("您没有权限提交此草稿");
        }
        if (submission.getStatus() != FormSubmission.SubmissionStatus.DRAFT) {
            throw new IllegalStateException("此申请已在流程中，无法重复提交");
        }

        submission.setDataJson(request.getDataJson());
        updateAttachments(submission, request.getAttachmentIds());

        // 【核心修复】在启动工作流前，检查是否存在已部署的流程模板
        WorkflowTemplate template = workflowTemplateRepository.findByFormDefinitionId(submission.getFormDefinition().getId())
                .orElseThrow(() -> new IllegalStateException(
                        "此表单未关联任何工作流，无法提交申请。请联系管理员。"));

        if (!StringUtils.hasText(template.getCamundaDeploymentId())) {
            throw new IllegalStateException(
                    "此表单关联的工作流尚未部署，无法提交申请。请联系管理员完成流程部署。");
        }
        // 【修复结束】

        workflowService.startWorkflow(submission, request.getInitialAction());

        submission.setStatus(FormSubmission.SubmissionStatus.PROCESSING);
        FormSubmission savedSubmission = formSubmissionRepository.save(submission);

        return convertToSubmissionResponse(savedSubmission);
    }

    @LogOperation(module = "数据列表", action = "更新提交记录", targetIdExpression = "#submissionId")
    public FormSubmissionResponse updateSubmission(Long submissionId, UpdateFormSubmissionRequest request) {
        FormSubmission submission = formSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("未找到提交记录 ID: " + submissionId));

        submission.setDataJson(request.getDataJson());
        updateAttachments(submission, request.getAttachmentIds());

        FormSubmission updatedSubmission = formSubmissionRepository.save(submission);
        return convertToSubmissionResponse(updatedSubmission);
    }

    @LogOperation(module = "数据列表", action = "删除提交记录", targetIdExpression = "#submissionId")
    public void deleteSubmission(Long submissionId) {
        FormSubmission submission = formSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("未找到提交记录 ID: " + submissionId));

        if (submission.getWorkflowInstance() != null && submission.getWorkflowInstance().getStatus() == WorkflowInstance.Status.PROCESSING) {
            throw new ResourceInUseException("无法删除：该记录关联的流程正在进行中。");
        }

        formSubmissionRepository.delete(submission);
    }

    // --- 【核心新增】删除草稿的业务逻辑 ---
    @LogOperation(module = "我的申请", action = "删除草稿", targetIdExpression = "#submissionId")
    public void deleteDraft(Long submissionId, String deleterId) {
        FormSubmission submission = formSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("未找到提交记录 ID: " + submissionId));

        // 双重校验：确保操作者是所有者，且记录状态是草稿
        if (!submission.getSubmitterId().equals(deleterId)) {
            throw new UnauthorizedException("您没有权限删除此草稿");
        }
        if (submission.getStatus() != FormSubmission.SubmissionStatus.DRAFT) {
            throw new IllegalStateException("只能删除草稿状态的申请");
        }

        formSubmissionRepository.delete(submission);
    }

    @Transactional(readOnly = true)
    public Page<FormSubmissionResponse> getMySubmissions(String userId, String keyword, String status, Pageable pageable) {
        Specification<FormSubmission> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("submitterId"), userId));

            if (StringUtils.hasText(keyword)) {
                Join<FormSubmission, FormDefinition> formDefJoin = root.join("formDefinition");
                predicates.add(cb.like(formDefJoin.get("name"), "%" + keyword + "%"));
            }

            if (StringUtils.hasText(status)) {
                predicates.add(cb.equal(root.get("status"), FormSubmission.SubmissionStatus.valueOf(status)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return formSubmissionRepository.findAll(spec, pageable).map(this::convertToSubmissionResponse);
    }

    @Transactional(readOnly = true)
    public Page<FormSubmissionResponse> getSubmissionsByFormId(Long formDefinitionId, Long menuId, Map<String, String> filters, Pageable pageable) {
        formDefinitionRepository.findById(formDefinitionId)
                .orElseThrow(() -> new ResourceNotFoundException("未找到 ID 为 " + formDefinitionId + " 的表单定义"));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        Specification<FormSubmission> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("formDefinition").get("id"), formDefinitionId));

            applyDataScope(predicates, query, cb, root, menuId, currentUser);

            if (filters != null) {
                filters.forEach((key, value) -> {
                    if (StringUtils.hasText(value) && !isPaginationParam(key) && !"menuId".equals(key)) {
                        Predicate jsonPredicate = cb.like(
                                cb.function("JSON_EXTRACT", String.class, root.get("dataJson"), cb.literal("$." + key)),
                                "%" + value + "%"
                        );
                        predicates.add(jsonPredicate);
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

        try {
            FormSchemaParser.ParsedSchema schema = FormSchemaParser.parse(entity.getSchemaJson(), objectMapper);
            dto.setFilterableFields(schema.getFilterableFields());
            dto.setListDisplayFields(schema.getListDisplayFields());
        } catch (Exception e) {
            log.error("解析表单 {} 的 schemaJson 失败", entity.getId(), e);
            dto.setFilterableFields(Collections.emptyList());
            dto.setListDisplayFields(Collections.emptyList());
        }

        return dto;
    }

    public FormSubmissionResponse convertToSubmissionResponse(FormSubmission entity) {
        FormSubmissionResponse dto = new FormSubmissionResponse();
        dto.setId(entity.getId());
        dto.setFormDefinitionId(entity.getFormDefinition().getId());
        dto.setFormName(entity.getFormDefinition().getName());
        dto.setDataJson(entity.getDataJson());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setSubmissionStatus(entity.getStatus().name());

        List<FileAttachment> attachments = fileAttachmentRepository.findByFormSubmissionId(entity.getId());
        if (!attachments.isEmpty()) {
            dto.setAttachments(fileService.getFilesBySubmissionId(entity.getId()));
        }

        userRepository.findById(entity.getSubmitterId())
                .ifPresent(user -> dto.setSubmitterName(user.getName()));

        dto.setWorkflowStatus(switch (entity.getStatus()) {
            case DRAFT -> "草稿";
            case PROCESSING -> "审批中";
            case APPROVED -> "已通过";
            case REJECTED -> "已拒绝";
            case TERMINATED -> "已终止";
        });
        return dto;
    }

    private void applyDataScope(List<Predicate> predicates, jakarta.persistence.criteria.CriteriaQuery<?> query, jakarta.persistence.criteria.CriteriaBuilder cb, jakarta.persistence.criteria.Root<FormSubmission> root, Long menuId, User currentUser) {
        if (menuId != null) {
            Menu menu = menuRepository.findById(menuId)
                    .orElseThrow(() -> new ResourceNotFoundException("未找到菜单ID: " + menuId));

            boolean isAdmin = currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            boolean hasExplicitRole = currentUser.getRoles().stream().anyMatch(userRole -> menu.getRoles().contains(userRole));

            if (!isAdmin && !hasExplicitRole) {
                throw new UnauthorizedException("您没有权限访问此菜单的数据");
            }

            DataScope dataScope = menu.getDataScope();
            if (dataScope != null && !isAdmin) {
                switch (dataScope) {
                    case BY_DEPARTMENT:
                        if (currentUser.getDepartment() != null) {
                            Subquery<String> subquery = query.subquery(String.class);
                            jakarta.persistence.criteria.Root<User> userRoot = subquery.from(User.class);
                            subquery.select(userRoot.get("id"))
                                    .where(cb.equal(userRoot.get("department").get("id"), currentUser.getDepartment().getId()));
                            predicates.add(root.get("submitterId").in(subquery));
                        } else {
                            predicates.add(cb.disjunction());
                        }
                        break;
                    case BY_GROUP:
                        Set<UserGroup> userGroups = currentUser.getUserGroups();
                        if (!userGroups.isEmpty()) {
                            Subquery<String> subquery = query.subquery(String.class);
                            jakarta.persistence.criteria.Root<User> userRoot = subquery.from(User.class);
                            subquery.select(userRoot.get("id"))
                                    .where(userRoot.join("userGroups").in(userGroups));
                            predicates.add(root.get("submitterId").in(subquery));
                        } else {
                            predicates.add(cb.disjunction());
                        }
                        break;
                    case OWNER_ONLY:
                        predicates.add(cb.equal(root.get("submitterId"), currentUser.getId()));
                        break;
                    case ALL:
                        if (!isAdmin) {
                            throw new UnauthorizedException("只有管理员才能查看全部数据");
                        }
                        break;
                }
            }
        } else {
            if (!currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                throw new UnauthorizedException("访问此数据列表需要通过指定的菜单入口");
            }
        }
    }

    private boolean isPaginationParam(String key) {
        return "page".equals(key) || "size".equals(key) || "sort".equals(key);
    }

    private void updateAttachments(FormSubmission submission, List<Long> newAttachmentIds) {
        if (newAttachmentIds == null) {
            newAttachmentIds = Collections.emptyList();
        }

        List<FileAttachment> toRemove = new ArrayList<>();
        for (FileAttachment existingAttachment : submission.getAttachments()) {
            if (!newAttachmentIds.contains(existingAttachment.getId())) {
                toRemove.add(existingAttachment);
            }
        }

        List<Long> existingIds = submission.getAttachments().stream()
                .map(FileAttachment::getId)
                .collect(Collectors.toList());
        List<Long> toAddIds = newAttachmentIds.stream()
                .filter(id -> !existingIds.contains(id))
                .collect(Collectors.toList());

        if (!toRemove.isEmpty()) {
            submission.getAttachments().removeAll(toRemove);
        }

        if (!toAddIds.isEmpty()) {
            List<FileAttachment> attachmentsToAdd = fileAttachmentRepository.findAllById(toAddIds);
            for (FileAttachment attachment : attachmentsToAdd) {
                attachment.setFormSubmission(submission);
                attachment.setStatus(FileAttachment.FileStatus.LINKED);
                submission.getAttachments().add(attachment);
            }
        }
    }

    @Transactional(readOnly = true)
    public boolean isOwner(Long submissionId, String username) {
        if (username == null) return false;
        return formSubmissionRepository.findById(submissionId)
                .map(sub -> sub.getSubmitterId().equals(username))
                .orElse(false);
    }
}