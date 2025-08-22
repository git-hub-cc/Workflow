package club.ppmc.workflow.service;

import club.ppmc.workflow.aop.LogOperation;
import club.ppmc.workflow.domain.*;
import club.ppmc.workflow.dto.*;
import club.ppmc.workflow.exception.ResourceInUseException;
import club.ppmc.workflow.exception.ResourceNotFoundException;
import club.ppmc.workflow.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.runtime.ActivityInstance;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.variable.VariableMap;
import org.camunda.bpm.engine.variable.Variables;
import org.camunda.bpm.engine.variable.value.TypedValue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author cc
 * @description 包含管理员操作的业务逻辑服务
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserGroupRepository userGroupRepository;
    private final RuntimeService runtimeService;
    private final HistoryService historyService;
    private final TaskService taskService;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;
    private final LoginLogRepository loginLogRepository;
    private final OperationLogRepository operationLogRepository;
    private final MenuService menuService;
    private final WorkflowTemplateRepository workflowTemplateRepository;
    private final DepartmentRepository departmentRepository;
    private final DepartmentService departmentService;
    private final ObjectMapper objectMapper;
    private final SystemSettingService systemSettingService; // 注入系统设置服务

    // --- 流程实例管理 ---
    public List<ProcessInstanceDto> getActiveProcessInstances() {
        List<ProcessInstance> instances = runtimeService.createProcessInstanceQuery().active().list();
        if (instances.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> processInstanceIdSet = instances.stream()
                .map(ProcessInstance::getProcessInstanceId)
                .collect(Collectors.toSet());

        Map<String, HistoricProcessInstance> historicInstanceMap = historyService.createHistoricProcessInstanceQuery()
                .processInstanceIds(processInstanceIdSet)
                .list()
                .stream()
                .collect(Collectors.toMap(HistoricProcessInstance::getId, Function.identity()));

        List<String> userIds = historicInstanceMap.values().stream()
                .map(HistoricProcessInstance::getStartUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<String, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return instances.stream()
                .map(instance -> {
                    HistoricProcessInstance historicInstance = historicInstanceMap.get(instance.getProcessInstanceId());
                    if (historicInstance == null) return null;

                    ActivityInstance activityInstance = runtimeService.getActivityInstance(instance.getProcessInstanceId());
                    String activityName = "进行中";
                    if (activityInstance != null && activityInstance.getChildActivityInstances().length > 0) {
                        activityName = Arrays.stream(activityInstance.getChildActivityInstances())
                                .map(ActivityInstance::getActivityName)
                                .filter(Objects::nonNull)
                                .collect(Collectors.joining(", "));
                    } else if (activityInstance != null && activityInstance.getActivityName() != null) {
                        activityName = activityInstance.getActivityName();
                    }

                    String startUserName = Optional.ofNullable(historicInstance.getStartUserId())
                            .map(userMap::get)
                            .map(User::getName)
                            .orElse("未知用户");

                    return ProcessInstanceDto.builder()
                            .processInstanceId(instance.getProcessInstanceId())
                            .businessKey(instance.getBusinessKey())
                            .processDefinitionName(historicInstance.getProcessDefinitionName())
                            .version(historicInstance.getProcessDefinitionVersion())
                            .startTime(historicInstance.getStartTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime())
                            .startUserId(historicInstance.getStartUserId())
                            .startUserName(startUserName)
                            .currentActivityName(activityName)
                            .suspended(instance.isSuspended())
                            .build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @LogOperation(module = "实例管理", action = "终止流程实例", targetIdExpression = "#processInstanceId")
    public void terminateProcessInstance(String processInstanceId, String reason) {
        runtimeService.deleteProcessInstance(processInstanceId, reason);
    }

    @LogOperation(module = "实例管理", action = "挂起流程实例", targetIdExpression = "#processInstanceId")
    public void suspendProcessInstance(String processInstanceId) {
        runtimeService.suspendProcessInstanceById(processInstanceId);
    }

    @LogOperation(module = "实例管理", action = "激活流程实例", targetIdExpression = "#processInstanceId")
    public void activateProcessInstance(String processInstanceId) {
        runtimeService.activateProcessInstanceById(processInstanceId);
    }

    @LogOperation(module = "实例管理", action = "转办任务", targetIdExpression = "#taskId")
    public void reassignTask(String taskId, String newAssigneeId) {
        if (!userRepository.existsById(newAssigneeId)) {
            throw new ResourceNotFoundException("新的办理人ID不存在: " + newAssigneeId);
        }
        taskService.setAssignee(taskId, newAssigneeId);
    }

    // --- 【核心修复】流程变量管理逻辑 ---
    @Transactional(readOnly = true)
    public List<ProcessVariableDto> getProcessInstanceVariables(String processInstanceId) {
        VariableMap variables = runtimeService.getVariablesTyped(processInstanceId, true);

        // 【FIX】遍历 VariableMap 的 keySet，然后使用 getValueTyped(key) 确保获取到 TypedValue 对象
        return variables.keySet().stream()
                .map(variableName -> {
                    TypedValue typedValue = variables.getValueTyped(variableName);
                    return ProcessVariableDto.builder()
                            .name(variableName)
                            .type(typedValue.getType().getName())
                            .value(formatVariableValue(typedValue))
                            .build();
                })
                .collect(Collectors.toList());
    }

    private Object formatVariableValue(TypedValue typedValue) {
        if (typedValue == null) {
            return null;
        }
        // Camunda 的 SPIN/JSON 类型可以直接获取为字符串
        if ("json".equalsIgnoreCase(typedValue.getType().getName())) {
            return typedValue.getValue().toString();
        }
        // 对于其他类型，直接返回值
        return typedValue.getValue();
    }

    @LogOperation(module = "实例管理", action = "修改流程变量", targetIdExpression = "#processInstanceId")
    public void updateProcessInstanceVariable(String processInstanceId, ProcessVariableDto variableDto) {
        Object value;
        try {
            // 尝试将值反序列化为最合适的 Java 类型
            value = deserializeValue(variableDto.getValue(), variableDto.getType());
        } catch (Exception e) { // 捕获更广泛的异常，如 NumberFormatException
            throw new IllegalArgumentException("无法解析变量值 '" + variableDto.getValue() + "' 为类型 '" + variableDto.getType() + "': " + e.getMessage(), e);
        }

        runtimeService.setVariable(processInstanceId, variableDto.getName(), value);
    }

    private Object deserializeValue(Object value, String type) throws JsonProcessingException {
        if (value == null) {
            return null;
        }

        String valueStr = value.toString();
        if (!StringUtils.hasText(type)) {
            // 如果类型为空，则尝试智能判断
            if (valueStr.equalsIgnoreCase("true") || valueStr.equalsIgnoreCase("false")) {
                return Boolean.parseBoolean(valueStr);
            }
            try {
                return Integer.parseInt(valueStr);
            } catch (NumberFormatException e1) {
                try {
                    return Long.parseLong(valueStr);
                } catch (NumberFormatException e2) {
                    try {
                        return Double.parseDouble(valueStr);
                    } catch (NumberFormatException e3) {
                        return valueStr; // 最终回退到字符串
                    }
                }
            }
        }

        switch (type.toLowerCase()) {
            case "string":
                return valueStr;
            case "boolean":
                return Boolean.parseBoolean(valueStr);
            case "integer":
                return Integer.parseInt(valueStr);
            case "long":
                return Long.parseLong(valueStr);
            case "double":
                return Double.parseDouble(valueStr);
            case "json":
                // Camunda 的 SPIN 库可以处理 JSON 字符串
                return Variables.objectValue(valueStr).serializationDataFormat(Variables.SerializationDataFormats.JSON).create();
            default:
                // 默认按字符串处理
                return valueStr;
        }
    }


    // --- 用户管理 ---
    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toUserDto)
                .collect(Collectors.toList());
    }

    @LogOperation(module = "用户管理", action = "创建用户", targetIdExpression = "#result?.id")
    public UserDto createUser(UserDto userDto) {
        if (userRepository.existsById(userDto.getId())) {
            throw new IllegalArgumentException("用户ID '" + userDto.getId() + "' 已存在");
        }
        User user = new User();
        user.setId(userDto.getId());
        user.setName(userDto.getName());
        user.setStatus(UserStatus.ACTIVE);

        // --- 【核心修改】从系统设置中读取默认密码 ---
        String defaultPassword = systemSettingService.getSettingsMap()
                .getOrDefault("DEFAULT_PASSWORD", "password");
        user.setPassword(passwordEncoder.encode(defaultPassword));
        user.setPasswordChangeRequired(true);

        updateUserRelations(user, userDto);
        User savedUser = userRepository.save(user);
        return toUserDto(savedUser);
    }

    @LogOperation(module = "用户管理", action = "更新用户", targetIdExpression = "#id")
    public UserDto updateUser(String id, UserDto userDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("未找到用户: " + id));
        user.setName(userDto.getName());
        user.setEmail(userDto.getEmail());
        user.setPhoneNumber(userDto.getPhoneNumber());

        if (StringUtils.hasText(userDto.getStatus())) {
            user.setStatus(UserStatus.valueOf(userDto.getStatus().toUpperCase()));
        }

        updateUserRelations(user, userDto);
        User updatedUser = userRepository.save(user);
        return toUserDto(updatedUser);
    }

    private void updateUserRelations(User user, UserDto userDto) {
        if (userDto.getDepartmentId() != null) {
            Department department = departmentRepository.findById(userDto.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("未找到部门 ID: " + userDto.getDepartmentId()));
            user.setDepartment(department);
        } else {
            user.setDepartment(null);
        }

        if (StringUtils.hasText(userDto.getManagerId())) {
            if (user.getId() != null && user.getId().equals(userDto.getManagerId())) {
                throw new IllegalArgumentException("用户不能将自己设置为上级");
            }
            User manager = userRepository.findById(userDto.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("未找到上级用户: " + userDto.getManagerId()));
            user.setManager(manager);
        } else {
            user.setManager(null);
        }

        if (!CollectionUtils.isEmpty(userDto.getRoleNames())) {
            user.setRoles(roleRepository.findByNameIn(userDto.getRoleNames()));
        } else {
            user.getRoles().clear();
        }

        if (!CollectionUtils.isEmpty(userDto.getGroupNames())) {
            user.setUserGroups(userGroupRepository.findByNameIn(userDto.getGroupNames()));
        } else {
            user.getUserGroups().clear();
        }
    }

    @LogOperation(module = "用户管理", action = "禁用用户", targetIdExpression = "#id")
    public void disableUser(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("未找到用户: " + id));
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
    }

    @LogOperation(module = "用户管理", action = "启用用户", targetIdExpression = "#id")
    public void enableUser(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("未找到用户: " + id));
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
    }

    // --- 角色管理 ---
    @LogOperation(module = "角色管理", action = "创建角色", targetIdExpression = "#result?.name")
    public RoleDto createRole(RoleDto roleDto) {
        if(roleRepository.findByName(roleDto.getName()).isPresent()) {
            throw new IllegalArgumentException("角色名称已存在: " + roleDto.getName());
        }
        Role role = new Role();
        role.setName(roleDto.getName());
        role.setDescription(roleDto.getDescription());
        return toRoleDto(roleRepository.save(role));
    }

    public List<RoleDto> getAllRoles() {
        return roleRepository.findAll().stream().map(this::toRoleDto).collect(Collectors.toList());
    }

    @LogOperation(module = "角色管理", action = "更新角色", targetIdExpression = "#id")
    public RoleDto updateRole(Long id, RoleDto roleDto) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("未找到角色ID: " + id));

        // --- 【核心修改】增加对角色名称修改的校验 ---
        String oldName = role.getName();
        String newName = roleDto.getName();

        if (newName != null && !newName.equals(oldName)) {
            if (roleRepository.findByName(newName).isPresent()) {
                throw new IllegalArgumentException("角色名称 '" + newName + "' 已存在。");
            }
            // 检查旧名称是否在流程中被使用
            String roleSearchPattern = "\"" + oldName + "\"";
            long workflowUsageCount = workflowTemplateRepository.countByBpmnXmlContaining(roleSearchPattern);
            if (workflowUsageCount > 0) {
                throw new ResourceInUseException("无法修改角色名称 '" + oldName + "'，因为它正被 " + workflowUsageCount + " 个工作流模板使用。请先在流程设计器中修改。");
            }
            role.setName(newName);
        }

        role.setDescription(roleDto.getDescription());
        return toRoleDto(roleRepository.save(role));
    }

    @LogOperation(module = "角色管理", action = "删除角色", targetIdExpression = "#id")
    public void deleteRole(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("未找到角色ID: " + id));

        entityManager.refresh(role);

        if (!role.getUsers().isEmpty()) {
            throw new ResourceInUseException("无法删除角色，因为仍有 " + role.getUsers().size() + " 个用户属于该角色。");
        }

        if (!role.getMenus().isEmpty()) {
            throw new ResourceInUseException("无法删除角色，因为它已被 " + role.getMenus().size() + " 个菜单项使用。");
        }

        String roleSearchPattern = "\"" + role.getName() + "\"";
        long workflowUsageCount = workflowTemplateRepository.countByBpmnXmlContaining(roleSearchPattern);
        if (workflowUsageCount > 0) {
            throw new ResourceInUseException("无法删除角色 '" + role.getName() + "'，因为它正被 " + workflowUsageCount + " 个工作流模板使用。");
        }

        roleRepository.delete(role);
    }


    // --- 用户组管理 ---
    @LogOperation(module = "用户组管理", action = "创建用户组", targetIdExpression = "#result?.name")
    public UserGroupDto createGroup(UserGroupDto groupDto) {
        if(userGroupRepository.findByName(groupDto.getName()).isPresent()) {
            throw new IllegalArgumentException("用户组名称已存在: " + groupDto.getName());
        }
        UserGroup group = new UserGroup();
        group.setName(groupDto.getName());
        group.setDescription(groupDto.getDescription());
        return toUserGroupDto(userGroupRepository.save(group));
    }

    public List<UserGroupDto> getAllGroups() {
        return userGroupRepository.findAll().stream().map(this::toUserGroupDto).collect(Collectors.toList());
    }

    @LogOperation(module = "用户组管理", action = "更新用户组", targetIdExpression = "#id")
    public UserGroupDto updateGroup(Long id, UserGroupDto groupDto) {
        UserGroup group = userGroupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("未找到用户组ID: " + id));

        // --- 【核心修改】增加对用户组名称修改的校验 ---
        String oldName = group.getName();
        String newName = groupDto.getName();
        if (newName != null && !newName.equals(oldName)) {
            if (userGroupRepository.findByName(newName).isPresent()) {
                throw new IllegalArgumentException("用户组名称 '" + newName + "' 已存在。");
            }
            String groupSearchPattern = "\"" + oldName + "\"";
            long workflowUsageCount = workflowTemplateRepository.countByBpmnXmlContaining(groupSearchPattern);
            if (workflowUsageCount > 0) {
                throw new ResourceInUseException("无法修改用户组名称 '" + oldName + "'，因为它正被 " + workflowUsageCount + " 个工作流模板使用。请先在流程设计器中修改。");
            }
            group.setName(newName);
        }

        group.setDescription(groupDto.getDescription());
        return toUserGroupDto(userGroupRepository.save(group));
    }

    @LogOperation(module = "用户组管理", action = "删除用户组", targetIdExpression = "#id")
    public void deleteGroup(Long id) {
        UserGroup group = userGroupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("未找到用户组ID: " + id));

        entityManager.refresh(group);

        if (!group.getUsers().isEmpty()) {
            throw new ResourceInUseException("无法删除用户组，因为仍有 " + group.getUsers().size() + " 个用户属于该组。");
        }

        String groupSearchPattern = "\"" + group.getName() + "\"";
        long workflowUsageCount = workflowTemplateRepository.countByBpmnXmlContaining(groupSearchPattern);
        if (workflowUsageCount > 0) {
            throw new ResourceInUseException("无法删除用户组 '" + group.getName() + "'，因为它正被 " + workflowUsageCount + " 个工作流模板用作候选组。");
        }

        userGroupRepository.delete(group);
    }


    // --- DTO 转换器 ---
    private UserDto toUserDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setStatus(user.getStatus().name());
        if (user.getManager() != null) {
            dto.setManagerId(user.getManager().getId());
        }
        if (user.getDepartment() != null) {
            dto.setDepartmentId(user.getDepartment().getId());
            dto.setDepartmentName(user.getDepartment().getName());
        }
        if (user.getRoles() != null) {
            dto.setRoleNames(user.getRoles().stream().map(Role::getName).collect(Collectors.toList()));
        }
        if (user.getUserGroups() != null) {
            dto.setGroupNames(user.getUserGroups().stream().map(UserGroup::getName).collect(Collectors.toList()));
        }
        return dto;
    }

    private RoleDto toRoleDto(Role role) {
        RoleDto dto = new RoleDto();
        dto.setId(role.getId());
        dto.setName(role.getName());
        dto.setDescription(role.getDescription());
        return dto;
    }

    private UserGroupDto toUserGroupDto(UserGroup group) {
        UserGroupDto dto = new UserGroupDto();
        dto.setId(group.getId());
        dto.setName(group.getName());
        dto.setDescription(group.getDescription());
        return dto;
    }


    // --- 组织架构与日志 ---

    @Transactional(readOnly = true)
    public List<DepartmentTreeNode> getOrganizationTree() {
        List<DepartmentDto> departmentTree = departmentService.getDepartmentTree();
        List<User> allUsers = userRepository.findAll();
        Map<Long, List<User>> usersByDeptId = allUsers.stream()
                .filter(u -> u.getDepartment() != null)
                .collect(Collectors.groupingBy(u -> u.getDepartment().getId()));

        return departmentTree.stream()
                .map(deptDto -> convertDeptDtoToTreeNode(deptDto, usersByDeptId))
                .collect(Collectors.toList());
    }

    private DepartmentTreeNode convertDeptDtoToTreeNode(DepartmentDto deptDto, Map<Long, List<User>> usersByDeptId) {
        DepartmentTreeNode deptNode = new DepartmentTreeNode();
        deptNode.setTitle(deptDto.getName());
        deptNode.setKey("dept_" + deptDto.getId());
        deptNode.setValue(String.valueOf(deptDto.getId()));
        deptNode.setType("department");
        deptNode.setLeaf(false);

        if (!CollectionUtils.isEmpty(deptDto.getChildren())) {
            List<DepartmentTreeNode> childDeptNodes = deptDto.getChildren().stream()
                    .map(childDto -> convertDeptDtoToTreeNode(childDto, usersByDeptId))
                    .collect(Collectors.toList());
            deptNode.getChildren().addAll(childDeptNodes);
        }

        List<User> usersInDept = usersByDeptId.get(deptDto.getId());
        if (!CollectionUtils.isEmpty(usersInDept)) {
            List<DepartmentTreeNode> userNodes = usersInDept.stream()
                    .map(user -> {
                        DepartmentTreeNode userNode = new DepartmentTreeNode();
                        userNode.setTitle(user.getName() + " (" + user.getId() + ")");
                        userNode.setKey("user_" + user.getId());
                        userNode.setValue(user.getId());
                        userNode.setType("user");
                        userNode.setLeaf(true);
                        return userNode;
                    })
                    .sorted(Comparator.comparing(DepartmentTreeNode::getTitle))
                    .collect(Collectors.toList());
            deptNode.getChildren().addAll(userNodes);
        }

        return deptNode;
    }

    @Transactional(readOnly = true)
    public List<TreeNodeDto> getDepartmentTree() {
        List<User> allUsers = userRepository.findAll();

        Map<String, List<User>> usersByDepartment = allUsers.stream()
                .filter(user -> user.getDepartment() != null && StringUtils.hasText(user.getDepartment().getName()))
                .collect(Collectors.groupingBy(user -> user.getDepartment().getName()));

        return usersByDepartment.entrySet().stream()
                .map(entry -> {
                    String deptName = entry.getKey();
                    List<User> usersInDept = entry.getValue();

                    TreeNodeDto deptNode = new TreeNodeDto();
                    deptNode.setTitle(deptName);
                    deptNode.setValue("dept_" + deptName);
                    deptNode.setKey("dept_" + deptName);

                    List<TreeNodeDto> userNodes = usersInDept.stream()
                            .map(user -> {
                                TreeNodeDto userNode = new TreeNodeDto();
                                userNode.setTitle(user.getName());
                                userNode.setValue(user.getId());
                                userNode.setKey(user.getId());
                                return userNode;
                            })
                            .sorted(Comparator.comparing(TreeNodeDto::getTitle))
                            .collect(Collectors.toList());

                    deptNode.setChildren(userNodes);
                    return deptNode;
                })
                .sorted(Comparator.comparing(TreeNodeDto::getTitle))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<LoginLogDto> getLoginLogs(String userId, String status, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable) {
        Specification<LoginLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(userId)) {
                predicates.add(cb.like(root.get("userId"), "%" + userId + "%"));
            }
            if (StringUtils.hasText(status)) {
                predicates.add(cb.equal(root.get("status"), LoginLog.LoginStatus.valueOf(status.toUpperCase())));
            }
            if (startTime != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("loginTime"), startTime));
            }
            if (endTime != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("loginTime"), endTime));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return loginLogRepository.findAll(spec, pageable).map(this::toLoginLogDto);
    }

    @Transactional(readOnly = true)
    public Page<OperationLogDto> getOperationLogs(String operatorId, String module, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable) {
        Specification<OperationLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(operatorId)) {
                predicates.add(cb.like(root.get("operatorId"), "%" + operatorId + "%"));
            }
            if (StringUtils.hasText(module)) {
                predicates.add(cb.equal(root.get("module"), module));
            }
            if (startTime != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("operationTime"), startTime));
            }
            if (endTime != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("operationTime"), endTime));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return operationLogRepository.findAll(spec, pageable).map(this::toOperationLogDto);
    }

    private LoginLogDto toLoginLogDto(LoginLog log) {
        LoginLogDto dto = new LoginLogDto();
        dto.setId(log.getId());
        dto.setUserId(log.getUserId());
        dto.setLoginTime(log.getLoginTime());
        dto.setIpAddress(log.getIpAddress());
        dto.setUserAgent(log.getUserAgent());
        dto.setStatus(log.getStatus().name());
        dto.setFailureReason(log.getFailureReason());
        return dto;
    }

    private OperationLogDto toOperationLogDto(OperationLog log) {
        OperationLogDto dto = new OperationLogDto();
        dto.setId(log.getId());
        dto.setOperatorId(log.getOperatorId());
        dto.setOperatorName(log.getOperatorName());
        dto.setOperationTime(log.getOperationTime());
        dto.setModule(log.getModule());
        dto.setAction(log.getAction());
        dto.setTargetId(log.getTargetId());
        dto.setDetails(log.getDetails());
        dto.setIpAddress(log.getIpAddress());
        return dto;
    }
}