package club.ppmc.workflow;

import club.ppmc.workflow.domain.User;
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

/**
 * @author 你的名字
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
    public CommandLineRunner initData(UserRepository userRepository) {
        return args -> {
            System.out.println("正在初始化演示用户数据...");

            String defaultPassword = passwordEncoder.encode("password");

            // 1. 创建所有用户对象
            User admin = new User();
            admin.setId("admin");
            admin.setName("系统管理员");
            admin.setRole("ADMIN");
            admin.setDepartment("IT部");
            admin.setPassword(passwordEncoder.encode("admin"));

            User user3 = new User(); // 财务总监-王五
            user3.setId("hr001");
            user3.setName("财务总监-王五");
            user3.setRole("USER");
            user3.setDepartment("财务部");
            user3.setPassword(defaultPassword);

            User user2 = new User(); // 部门经理-李四
            user2.setId("manager001");
            user2.setName("研发部经理-李四");
            user2.setRole("USER");
            user2.setDepartment("研发部");
            user2.setPassword(defaultPassword);

            User user1 = new User(); // 普通员工-张三
            user1.setId("user001");
            user1.setName("研发工程师-张三");
            user1.setRole("USER");
            user1.setDepartment("研发部");
            user1.setPassword(defaultPassword);

            // --- 【核心修改：保证保存顺序并设置部门】 ---
            // 2. 先保存没有依赖或作为依赖顶端的实体
            userRepository.save(admin);
            userRepository.save(user3); // 王五是最高级，先保存

            // 3. 设置依赖关系并保存
            user2.setManager(user3); // 李四的上级是王五
            userRepository.save(user2);

            user1.setManager(user2); // 张三的上级是李四
            userRepository.save(user1);
            // --- 【修改结束】 ---

            System.out.println("演示用户数据初始化完成！");
            System.out.println("已设置 'user001' 的上级为 'manager001'");
            System.out.println("已设置 'manager001' 的上级为 'hr001'");
        };
    }
}