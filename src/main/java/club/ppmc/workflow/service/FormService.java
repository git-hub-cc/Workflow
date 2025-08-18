package club.ppmc.workflow.service;

import club.ppmc.workflow.aop.LogOperation;
import club.ppmc.workflow.domain.*;
import club.ppmc.workflow.dto.CreateFormDefinitionRequest;
import club.ppmc.workflow.dto.CreateFormSubmissionRequest;
import club.ppmc.workflow.dto.FormDefinitionResponse;
import club.ppmc.workflow.dto.FormSubmissionResponse;
import club.ppmc.workflow.exception.ResourceNotFoundException;
import club.ppmc.workflow.exception.UnauthorizedException;
import club.ppmc.workflow.repository.*;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final MenuRepository menuRepository;
    private final WorkflowTemplateRepository workflowTemplateRepository;

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

    @LogOperation(module = "表单管理", action = "删除表单定义", targetIdExpression = "#id")
    public void deleteFormDefinition(Long id) {
        if (!formDefinitionRepository.existsById(id)) {
            throw new ResourceNotFoundException("未找到表单定义 ID: " + id);
        }
        if (workflowTemplateRepository.findByFormDefinitionId(id).isPresent()) {
            throw new IllegalStateException("无法删除：该表单已关联一个工作流模板。请先在流程设计器中解除关联或删除流程。");
        }
        if (menuRepository.existsByFormDefinitionId(id)) {
            throw new IllegalStateException("无法删除：该表单已被一个或多个菜单项使用。请先在菜单管理中解除关联。");
        }
        formDefinitionRepository.deleteById(id);
    }

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
                attachment.setStatus(FileAttachment.FileStatus.LINKED);
            }
            fileAttachmentRepository.saveAll(attachments);
        }

        workflowService.startWorkflow(savedSubmission);

        FormSubmission finalSubmission = formSubmissionRepository.findById(savedSubmission.getId())
                .orElseThrow(() -> new IllegalStateException("刚保存的提交记录找不到了"));
        return convertToSubmissionResponse(finalSubmission);
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
                Join<FormSubmission, WorkflowInstance> instanceJoin = root.join("workflowInstance");
                predicates.add(cb.equal(instanceJoin.get("status"), WorkflowInstance.Status.valueOf(status)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return formSubmissionRepository.findAll(spec, pageable).map(this::convertToSubmissionResponse);
    }

    @Transactional(readOnly = true)
    public Page<FormSubmissionResponse> getSubmissionsByFormId(Long formDefinitionId, Long menuId, Map<String, String> filters, Pageable pageable) {
        if (!formDefinitionRepository.existsById(formDefinitionId)) {
            throw new ResourceNotFoundException("未找到 ID 为 " + formDefinitionId + " 的表单定义");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        Specification<FormSubmission> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("formDefinition").get("id"), formDefinitionId));

            if (menuId != null) {
                Menu menu = menuRepository.findById(menuId)
                        .orElseThrow(() -> new ResourceNotFoundException("未找到菜单ID: " + menuId));

                // --- 【核心修复】开始 ---
                // 1. 检查当前用户是否为管理员
                boolean isAdmin = currentUser.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

                // 2. 检查当前用户是否拥有菜单的显式角色
                boolean hasExplicitRole = currentUser.getRoles().stream()
                        .anyMatch(userRole -> menu.getRoles().contains(userRole));

                // 3. 如果用户既不是管理员，也没有被显式授权，才抛出异常
                if (!isAdmin && !hasExplicitRole) {
                    throw new UnauthorizedException("您没有权限访问此菜单的数据");
                }
                // --- 【核心修复】结束 ---

                DataScope dataScope = menu.getDataScope();
                if (dataScope != null) {
                    switch (dataScope) {
                        case BY_DEPARTMENT:
                            if (currentUser.getDepartment() != null) {
                                // 这里需要一个别名来避免JPA的路径冲突
                                Join<FormSubmission, User> submitterJoinDept = root.join("submitter");
                                predicates.add(cb.equal(submitterJoinDept.get("department").get("id"), currentUser.getDepartment().getId()));
                            } else {
                                // 如果用户自己没有部门，则他看不到任何按部门筛选的数据
                                predicates.add(cb.disjunction()); // 总是返回 false
                            }
                            break;
                        case BY_GROUP:
                            Set<UserGroup> userGroups = currentUser.getUserGroups();
                            if (!userGroups.isEmpty()) {
                                Join<FormSubmission, User> submitterJoinGroup = root.join("submitter");
                                predicates.add(submitterJoinGroup.join("userGroups").in(userGroups));
                            } else {
                                // 如果用户不在任何组，则他看不到任何按组筛选的数据
                                predicates.add(cb.disjunction());
                            }
                            break;
                        case OWNER_ONLY:
                            predicates.add(cb.equal(root.get("submitterId"), currentUser.getId()));
                            break;
                        case ALL:
                            // 即使是管理员，也需要经过这个case，但这里的检查确保只有管理员能看到ALL数据
                            if (!isAdmin) {
                                throw new UnauthorizedException("只有管理员才能查看全部数据");
                            }
                            // 管理员无需添加额外条件
                            break;
                    }
                }
            } else {
                // 如果没有通过菜单访问，则只允许管理员访问（例如，从表单管理页的“查看数据”按钮进入）
                if (!currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                    throw new UnauthorizedException("访问此数据列表需要通过指定的菜单入口");
                }
            }

            if (filters != null) {
                filters.forEach((key, value) -> {
                    if (StringUtils.hasText(value) && !"page".equals(key) && !"size".equals(key) && !"sort".equals(key) && !"menuId".equals(key)) {
                        // 示例：这里可以根据表单字段的schema来构建更复杂的查询
                        // 为简单起见，我们只查询提交人ID
                        predicates.add(cb.like(root.get("submitterId"), "%" + value + "%"));
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
        dto.setFormName(entity.getFormDefinition().getName());
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