package club.ppmc.workflow;

import club.ppmc.workflow.domain.Role;
import club.ppmc.workflow.domain.User;
import club.ppmc.workflow.repository.RoleRepository;
import club.ppmc.workflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.spring.boot.starter.annotation.EnableProcessApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Set;

/**
 * @author cc
 * @description Spring Boot 主启动类
 */
@SpringBootApplication
@RequiredArgsConstructor
@EnableProcessApplication
@EnableAsync
public class WorkflowApplication {

    private final PasswordEncoder passwordEncoder;

    public static void main(String[] args) {
        SpringApplication.run(WorkflowApplication.class, args);
        // ... (省略日志输出) ...
    }

    @Bean
    public CommandLineRunner initData(UserRepository userRepository, RoleRepository roleRepository) {
        return args -> {
            System.out.println("正在初始化演示用户及角色数据...");

            // --- 【核心修改：先初始化角色】 ---
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

            // 2. 创建用户并分配角色
            String defaultPassword = passwordEncoder.encode("password");

            User admin = new User();
            admin.setId("admin");
            admin.setName("系统管理员");
            admin.setDepartment("IT部");
            admin.setPassword(passwordEncoder.encode("admin"));
            admin.setRoles(Set.of(adminRole, userRole)); // 管理员同时也是一个用户
            userRepository.save(admin);

            User user3 = new User(); // 财务总监-王五
            user3.setId("hr001");
            user3.setName("财务总监-王五");
            user3.setDepartment("财务部");
            user3.setPassword(defaultPassword);
            user3.setRoles(Set.of(userRole, financeRole)); // 王五是普通用户，也是财务审批员
            userRepository.save(user3);

            User user2 = new User(); // 部门经理-李四
            user2.setId("manager001");
            user2.setName("研发部经理-李四");
            user2.setDepartment("研发部");
            user2.setPassword(defaultPassword);
            user2.setRoles(Set.of(userRole));
            user2.setManager(user3); // 李四的上级是王五
            userRepository.save(user2);

            User user1 = new User(); // 普通员工-张三
            user1.setId("user001");
            user1.setName("研发工程师-张三");
            user1.setDepartment("研发部");
            user1.setPassword(defaultPassword);
            user1.setRoles(Set.of(userRole));
            user1.setManager(user2); // 张三的上级是李四
            userRepository.save(user1);
            // --- 【修改结束】 ---

            System.out.println("演示用户和角色数据初始化完成！");
            System.out.println("已创建角色: ADMIN, USER, FINANCE_APPROVER");
            System.out.println("已设置 'user001' 的上级为 'manager001'");
            System.out.println("已设置 'manager001' 的上级为 'hr001'");
        };
    }
}