package club.ppmc.workflow;

import club.ppmc.workflow.domain.Department;
import club.ppmc.workflow.domain.Role;
import club.ppmc.workflow.domain.SystemSetting;
import club.ppmc.workflow.domain.User;
import club.ppmc.workflow.repository.DepartmentRepository;
import club.ppmc.workflow.repository.RoleRepository;
import club.ppmc.workflow.repository.SystemSettingRepository;
import club.ppmc.workflow.repository.UserRepository;
import club.ppmc.workflow.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.spring.boot.starter.annotation.EnableProcessApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author cc
 * @description Spring Boot 主启动类
 */
@SpringBootApplication
@RequiredArgsConstructor
@EnableProcessApplication
@EnableAsync
@EnableScheduling
public class WorkflowApplication {

    private final PasswordEncoder passwordEncoder;

    public static void main(String[] args) {
        SpringApplication.run(WorkflowApplication.class, args);
    }

    @Bean
    @Transactional
    public CommandLineRunner initData(
            UserRepository userRepository,
            RoleRepository roleRepository,
            DepartmentRepository departmentRepository,
            // --- 【核心新增】注入 SystemSettingRepository ---
            SystemSettingRepository systemSettingRepository
    ) {
        return args -> {
            System.out.println("正在初始化演示用户及角色数据...");

            // 1. 创建并保存角色
            Role adminRole = new Role();
            adminRole.setName("ADMIN");
            adminRole.setDescription("系统管理员，拥有所有权限");
            roleRepository.save(adminRole);

            Role userRole = new Role();
            userRole.setName("USER");
            userRole.setDescription("普通用户，拥有基本操作权限");
            roleRepository.save(userRole);

            Role financeRole = new Role();
            financeRole.setName("FINANCE_APPROVER");
            financeRole.setDescription("财务审批员");
            roleRepository.save(financeRole);

            // --- 【核心新增：创建部门】 ---
            Department itDept = new Department();
            itDept.setName("IT部");
            departmentRepository.save(itDept);

            Department financeDept = new Department();
            financeDept.setName("财务部");
            departmentRepository.save(financeDept);

            Department rdDept = new Department();
            rdDept.setName("研发部");
            departmentRepository.save(rdDept);


            // 2. 创建用户并分配角色和部门
            String defaultPassword = passwordEncoder.encode("password");

            User admin = new User();
            admin.setId("admin");
            admin.setName("系统管理员");
            admin.setDepartment(itDept); // 关联部门实体
            admin.setPassword(passwordEncoder.encode("admin"));
            admin.setRoles(Set.of(adminRole, userRole));
            userRepository.save(admin);

            User user3 = new User(); // 财务总监-王五
            user3.setId("hr001");
            user3.setName("财务总监-王五");
            user3.setDepartment(financeDept); // 关联部门实体
            user3.setPassword(defaultPassword);
            user3.setRoles(Set.of(userRole, financeRole));
            userRepository.save(user3);

            User user2 = new User(); // 部门经理-李四
            user2.setId("manager001");
            user2.setName("研发部经理-李四");
            user2.setDepartment(rdDept); // 关联部门实体
            user2.setPassword(defaultPassword);
            user2.setRoles(Set.of(userRole));
            user2.setManager(user3); // 李四的上级是王五
            userRepository.save(user2);

            User user1 = new User(); // 普通员工-张三
            user1.setId("user001");
            user1.setName("研发工程师-张三");
            user1.setDepartment(rdDept); // 关联部门实体
            user1.setPassword(defaultPassword);
            user1.setRoles(Set.of(userRole));
            user1.setManager(user2); // 张三的上级是李四
            userRepository.save(user1);

            System.out.println("演示用户、角色和部门数据初始化完成！");


            // ADMIN
//            Role adminRole = new Role();
//            adminRole.setName("ADMIN");
//            adminRole.setDescription("系统管理员，拥有所有权限");
//            roleRepository.save(adminRole);
//
//            Department itDept = new Department();
//            itDept.setName("IT部");
//            departmentRepository.save(itDept);
//
//            Role userRole = new Role();
//            userRole.setName("USER");
//            userRole.setDescription("普通用户，拥有基本操作权限");
//            roleRepository.save(userRole);
//
//            User admin = new User();
//            admin.setId("admin");
//            admin.setName("系统管理员");
//            admin.setDepartment(itDept); // 关联部门实体
//            admin.setPassword(passwordEncoder.encode("admin"));
//            admin.setRoles(Set.of(adminRole, userRole));
//            userRepository.save(admin);

            // --- 【核心新增】初始化系统设置默认值 ---
            System.out.println("正在初始化系统设置默认值...");
            initSystemSetting(systemSettingRepository, SystemSettingService.SettingKeys.SYSTEM_NAME, "表单工作流引擎", "系统在浏览器标签页和登录页显示的名称");
            initSystemSetting(systemSettingRepository, SystemSettingService.SettingKeys.THEME_COLOR, "#1890ff", "Ant Design Vue 的主题色");
            initSystemSetting(systemSettingRepository, SystemSettingService.SettingKeys.FOOTER_INFO, "© 2025 PPMC Workflow. All Rights Reserved.", "显示在页面底部的版权信息");
            initSystemSetting(systemSettingRepository, SystemSettingService.SettingKeys.SYSTEM_ICON_ID, null, "系统图标的文件ID (来自文件附件表)");
            initSystemSetting(systemSettingRepository, SystemSettingService.SettingKeys.LOGIN_BACKGROUND_ID, null, "登录页背景图的文件ID (来自文件附件表)");
            System.out.println("系统设置默认值初始化完成！");
        };
    }

    /**
     * 【新增】一个辅助方法，用于在数据库中不存在某个设置项时创建它
     */
    private void initSystemSetting(SystemSettingRepository repository, String key, String value, String description) {
        if (repository.findBySettingKey(key).isEmpty()) {
            SystemSetting setting = new SystemSetting();
            setting.setSettingKey(key);
            setting.setSettingValue(value);
            setting.setDescription(description);
            repository.save(setting);
        }
    }


    /**
     * 【重要】这是一个数据迁移的示例。
     * 如果您是从旧版本（使用 department 字符串）升级，可以临时启用此 Bean 来迁移数据。
     * 迁移完成后应将其注释或删除。
     */
    // @Bean
    @Transactional
    public CommandLineRunner migrateDepartmentData(UserRepository userRepository, DepartmentRepository departmentRepository) {
        return args -> {
            System.out.println("【数据迁移】开始将用户的 department 字符串迁移到 Department 实体...");

            List<User> allUsers = userRepository.findAll();
            // 1. 找出所有唯一的部门名称
            Set<String> departmentNames = allUsers.stream()
                    .map(User::getDepartmentName) // 假设您临时在User实体中添加一个getDepartmentName()来获取旧的字符串值
                    .filter(name -> name != null && !name.isEmpty())
                    .collect(Collectors.toSet());

            // 2. 为每个部门名称创建 Department 实体
            Map<String, Department> departmentMap = departmentNames.stream()
                    .map(name -> {
                        Department dept = new Department();
                        dept.setName(name);
                        return departmentRepository.save(dept);
                    })
                    .collect(Collectors.toMap(Department::getName, dept -> dept));

            System.out.println("【数据迁移】创建了 " + departmentMap.size() + " 个部门实体。");

            // 3. 更新所有用户的 department 关联
            for (User user : allUsers) {
                String deptName = user.getDepartmentName();
                if (deptName != null && departmentMap.containsKey(deptName)) {
                    user.setDepartment(departmentMap.get(deptName));
                }
            }
            userRepository.saveAll(allUsers);

            System.out.println("【数据迁移】所有用户已成功关联到新的 Department 实体。迁移完成！");
            System.out.println("【注意】现在可以安全地从 User 实体中移除旧的 department 字符串字段，并删除此迁移 Bean。");
        };
    }
}