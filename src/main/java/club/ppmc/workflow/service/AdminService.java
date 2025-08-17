package club.ppmc.workflow.service;

import club.ppmc.workflow.aop.LogOperation;
import club.ppmc.workflow.domain.*;
import club.ppmc.workflow.dto.*;
import club.ppmc.workflow.exception.ResourceNotFoundException;
import club.ppmc.workflow.repository.LoginLogRepository;
import club.ppmc.workflow.repository.OperationLogRepository;
import club.ppmc.workflow.repository.RoleRepository;
import club.ppmc.workflow.repository.UserGroupRepository;
import club.ppmc.workflow.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.runtime.ActivityInstance;
import org.camunda.bpm.engine.runtime.ProcessInstance;
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


    // --- 用户管理 ---

    /**
     * 获取所有用户列表，用于管理页面
     * @return 包含完整信息的用户DTO列表
     */
    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toUserDto)
                .collect(Collectors.toList());
    }

    @LogOperation(module = "用户管理", action = "创建用户", targetIdExpression = "#result.id")
    public UserDto createUser(UserDto userDto) {
        if (userRepository.existsById(userDto.getId())) {
            throw new IllegalArgumentException("用户ID '" + userDto.getId() + "' 已存在");
        }
        User user = new User();
        user.setId(userDto.getId());
        user.setName(userDto.getName());
        user.setDepartment(userDto.getDepartment());
        user.setStatus(UserStatus.ACTIVE);

        user.setPassword(passwordEncoder.encode("password"));
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
        user.setDepartment(userDto.getDepartment());
        if (StringUtils.hasText(userDto.getStatus())) {
            user.setStatus(UserStatus.valueOf(userDto.getStatus().toUpperCase()));
        }

        updateUserRelations(user, userDto);
        User updatedUser = userRepository.save(user);
        return toUserDto(updatedUser);
    }

    private void updateUserRelations(User user, UserDto userDto) {
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
    public void deleteUser(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("未找到用户: " + id));
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
    }

    // --- 角色管理 ---
    @LogOperation(module = "角色管理", action = "创建角色", targetIdExpression = "#result.name")
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
        role.setDescription(roleDto.getDescription());
        return toRoleDto(roleRepository.save(role));
    }

    @LogOperation(module = "角色管理", action = "删除角色", targetIdExpression = "#id")
    public void deleteRole(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("未找到角色ID: " + id));

        entityManager.refresh(role);

        if (!role.getUsers().isEmpty()) {
            throw new IllegalStateException("无法删除角色，因为仍有 " + role.getUsers().size() + " 个用户属于该角色。");
        }
        roleRepository.delete(role);
    }


    // --- 用户组管理 ---
    @LogOperation(module = "用户组管理", action = "创建用户组", targetIdExpression = "#result.name")
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
        group.setName(groupDto.getName());
        group.setDescription(groupDto.getDescription());
        return toUserGroupDto(userGroupRepository.save(group));
    }

    @LogOperation(module = "用户组管理", action = "删除用户组", targetIdExpression = "#id")
    public void deleteGroup(Long id) {
        UserGroup group = userGroupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("未找到用户组ID: " + id));

        entityManager.refresh(group);

        if (!group.getUsers().isEmpty()) {
            throw new IllegalStateException("无法删除用户组，因为仍有 " + group.getUsers().size() + " 个用户属于该组。");
        }
        userGroupRepository.delete(group);
    }


    // --- DTO 转换器 ---
    private UserDto toUserDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setDepartment(user.getDepartment());
        dto.setStatus(user.getStatus().name());
        if (user.getManager() != null) {
            dto.setManagerId(user.getManager().getId());
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
        List<User> allUsers = userRepository.findAll();

        Map<String, List<User>> usersByDepartment = allUsers.stream()
                .filter(user -> user.getDepartment() != null)
                .collect(Collectors.groupingBy(User::getDepartment));

        Map<String, DepartmentTreeNode> departmentNodeMap = usersByDepartment.keySet().stream()
                .distinct()
                .map(deptName -> {
                    DepartmentTreeNode node = new DepartmentTreeNode();
                    node.setTitle(deptName);
                    node.setKey("dept_" + deptName);
                    node.setValue(deptName);
                    node.setType("department");
                    node.setLeaf(false);
                    return node;
                })
                .collect(Collectors.toMap(DepartmentTreeNode::getValue, Function.identity()));

        usersByDepartment.forEach((deptName, users) -> {
            DepartmentTreeNode parentNode = departmentNodeMap.get(deptName);
            if (parentNode != null) {
                List<DepartmentTreeNode> userNodes = users.stream()
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
                parentNode.getChildren().addAll(userNodes);
            }
        });

        return departmentNodeMap.values().stream()
                .sorted(Comparator.comparing(DepartmentTreeNode::getTitle))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TreeNodeDto> getDepartmentTree() {
        List<User> allUsers = userRepository.findAll();

        Map<String, List<User>> usersByDepartment = allUsers.stream()
                .filter(user -> user.getDepartment() != null)
                .collect(Collectors.groupingBy(User::getDepartment));

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