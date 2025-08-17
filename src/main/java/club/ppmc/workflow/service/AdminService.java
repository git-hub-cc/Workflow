package club.ppmc.workflow.service;

import club.ppmc.workflow.domain.*;
import club.ppmc.workflow.dto.*;
import club.ppmc.workflow.exception.ResourceNotFoundException;
import club.ppmc.workflow.repository.RoleRepository;
import club.ppmc.workflow.repository.UserGroupRepository;
import club.ppmc.workflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.runtime.ActivityInstance;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author 你的名字
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

    // ... [流程实例管理相关方法保持不变] ...
    public List<ProcessInstanceDto> getActiveProcessInstances() {
        // 1. 获取所有正在运行的流程实例
        List<ProcessInstance> instances = runtimeService.createProcessInstanceQuery().active().list();
        if (instances.isEmpty()) {
            return List.of();
        }

        List<String> processInstanceIds = instances.stream()
                .map(ProcessInstance::getProcessInstanceId)
                .collect(Collectors.toList());

        // 2. 批量获取这些实例的历史信息 (包含发起人等)
        // 【修正】使用 .processInstanceIds(Set<String>) 方法，它接受一个 Set 集合，兼容性更好。
        Set<String> processInstanceIdSet = new HashSet<>(processInstanceIds);
        Map<String, HistoricProcessInstance> historicInstanceMap = historyService.createHistoricProcessInstanceQuery()
                .processInstanceIds(processInstanceIdSet)
                .list()
                .stream()
                .collect(Collectors.toMap(HistoricProcessInstance::getId, Function.identity()));

        // 3. 预加载所有相关的用户信息
        List<String> userIds = historicInstanceMap.values().stream()
                .map(HistoricProcessInstance::getStartUserId)
                .filter(Objects::nonNull) // 过滤掉可能为 null 的 startUserId
                .distinct()
                .collect(Collectors.toList());
        Map<String, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        // 4. 组合最终结果
        return instances.stream()
                .map(instance -> {
                    HistoricProcessInstance historicInstance = historicInstanceMap.get(instance.getProcessInstanceId());
                    if (historicInstance == null) {
                        return null; // 数据不一致，跳过
                    }

                    ActivityInstance activityInstance = runtimeService.getActivityInstance(instance.getProcessInstanceId());
                    String activityName = "未知";
                    if (activityInstance != null && activityInstance.getChildActivityInstances().length > 0) {
                        // 通常当前活动是子实例, 比如用户任务
                        activityName = Arrays.stream(activityInstance.getChildActivityInstances())
                                .map(ActivityInstance::getActivityName)
                                .filter(Objects::nonNull)
                                .collect(Collectors.joining(", "));
                    } else if (activityInstance != null) {
                        activityName = activityInstance.getActivityName();
                    }
                    if (activityName == null || activityName.isEmpty()){
                        activityName = "进行中";
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
                            .build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public void terminateProcessInstance(String processInstanceId, String reason) {
        runtimeService.deleteProcessInstance(processInstanceId, reason);
    }

    public void suspendProcessInstance(String processInstanceId) {
        runtimeService.suspendProcessInstanceById(processInstanceId);
    }

    public void activateProcessInstance(String processInstanceId) {
        runtimeService.activateProcessInstanceById(processInstanceId);
    }

    public void reassignTask(String taskId, String newAssigneeId) {
        if (!userRepository.existsById(newAssigneeId)) {
            throw new ResourceNotFoundException("新的办理人ID不存在: " + newAssigneeId);
        }
        // Camunda API 内部会检查 task 是否存在
        taskService.setAssignee(taskId, newAssigneeId);
    }

    // --- 用户管理 (已更新) ---

    /**
     * 创建一个新用户
     * @param userDto 用户数据
     * @return 创建后的用户DTO
     */
    public UserDto createUser(UserDto userDto) {
        if (userRepository.existsById(userDto.getId())) {
            throw new IllegalArgumentException("用户ID '" + userDto.getId() + "' 已存在");
        }
        User user = new User();
        user.setId(userDto.getId());
        user.setName(userDto.getName());
        user.setDepartment(userDto.getDepartment());
        user.setStatus(UserStatus.ACTIVE); // 新用户默认为激活状态

        // 新用户默认密码为 'password'，并强制首次登录修改
        user.setPassword(passwordEncoder.encode("password"));
        user.setPasswordChangeRequired(true);

        updateUserRelations(user, userDto);

        User savedUser = userRepository.save(user);
        return toUserDto(savedUser);
    }

    /**
     * 更新用户信息 (不允许更新ID和密码)
     * @param id 用户ID
     * @param userDto 更新数据
     * @return 更新后的用户DTO
     */
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

    /**
     * 更新用户的关联关系 (上级、角色、用户组)
     */
    private void updateUserRelations(User user, UserDto userDto) {
        // 更新上级
        if (StringUtils.hasText(userDto.getManagerId())) {
            if (user.getId() != null && user.getId().equals(userDto.getManagerId())) {
                throw new IllegalArgumentException("用户不能将自己设置为上级");
            }
            User manager = userRepository.findById(userDto.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("未找到上级用户: " + userDto.getManagerId()));
            user.setManager(manager);
        } else {
            user.setManager(null); // 清除上级
        }

        // 更新角色
        if (!CollectionUtils.isEmpty(userDto.getRoleNames())) {
            Set<Role> roles = roleRepository.findByNameIn(userDto.getRoleNames());
            user.setRoles(roles);
        } else {
            user.getRoles().clear();
        }
    }

    /**
     * 删除用户 (逻辑删除，将状态改为 INACTIVE)
     * @param id 用户ID
     */
    public void deleteUser(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("未找到用户: " + id));
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
    }

    // --- 【新增】角色与用户组管理 ---
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
    // ... 可以添加更新和删除角色的方法 ...


    // --- 转换器 ---

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
        return dto;
    }

    private RoleDto toRoleDto(Role role) {
        RoleDto dto = new RoleDto();
        dto.setId(role.getId());
        dto.setName(role.getName());
        dto.setDescription(role.getDescription());
        return dto;
    }


    // --- 组织架构 (保持不变) ---
    /**
     * 获取组织架构树 (用于Ant Design Tree)
     * @return 部门和用户的树形结构列表
     */
    @Transactional(readOnly = true)
    public List<DepartmentTreeNode> getOrganizationTree() {
        List<User> allUsers = userRepository.findAll();

        // 1. 按部门对所有用户进行分组
        Map<String, List<User>> usersByDepartment = allUsers.stream()
                .filter(user -> user.getDepartment() != null)
                .collect(Collectors.groupingBy(User::getDepartment));

        // 2. 提取所有唯一的部门名称，并为每个部门创建一个树节点
        Map<String, DepartmentTreeNode> departmentNodeMap = usersByDepartment.keySet().stream()
                .distinct()
                .map(deptName -> {
                    DepartmentTreeNode node = new DepartmentTreeNode();
                    node.setTitle(deptName);
                    node.setKey("dept_" + deptName); // 构造唯一的 key
                    node.setValue(deptName);
                    node.setType("department");
                    node.setLeaf(false);
                    return node;
                })
                .collect(Collectors.toMap(DepartmentTreeNode::getValue, Function.identity()));

        // 3. 将用户作为叶子节点添加到对应的部门节点下
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
                        .sorted(Comparator.comparing(DepartmentTreeNode::getTitle)) // 按姓名排序
                        .collect(Collectors.toList());
                parentNode.getChildren().addAll(userNodes);
            }
        });

        // 4. 返回所有部门节点，并按部门名称排序
        return departmentNodeMap.values().stream()
                .sorted(Comparator.comparing(DepartmentTreeNode::getTitle))
                .collect(Collectors.toList());
    }

    /**
     * 获取部门-用户树形结构 (用于 a-tree-select)
     * @return 符合 TreeNodeDto 结构的列表
     */
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

                    // 创建部门节点 (父节点)
                    TreeNodeDto deptNode = new TreeNodeDto();
                    deptNode.setTitle(deptName);
                    deptNode.setValue("dept_" + deptName); // 部门节点的值是唯一的，但通常不可选
                    deptNode.setKey("dept_" + deptName);

                    // 创建用户节点 (子节点)
                    List<TreeNodeDto> userNodes = usersInDept.stream()
                            .map(user -> {
                                TreeNodeDto userNode = new TreeNodeDto();
                                userNode.setTitle(user.getName());
                                userNode.setValue(user.getId()); // 用户节点的值是用户的ID
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
}