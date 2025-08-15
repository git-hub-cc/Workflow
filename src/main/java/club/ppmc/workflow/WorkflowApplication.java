package club.ppmc.workflow;

import club.ppmc.workflow.domain.User;
import club.ppmc.workflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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
@EnableAsync // 启用异步任务, 用于发送邮件
public class WorkflowApplication {

    private final PasswordEncoder passwordEncoder; // 注入密码编码器

    public static void main(String[] args) {
        SpringApplication.run(WorkflowApplication.class, args);
        System.out.println("==================================================");
        System.out.println("应用程序启动成功！");
        System.out.println("访问地址: http://localhost:8081");
        System.out.println("H2 数据库控制台: http://localhost:8081/h2-console");
        System.out.println("确保 Camunda 引擎正在 http://localhost:8080 运行！");
        System.out.println("==================================================");
    }

    /**
     * 在应用启动时，初始化一些用户数据用于演示
     * @param userRepository 用户仓库
     * @return CommandLineRunner
     */
    @Bean
    public CommandLineRunner initData(UserRepository userRepository) {
        return args -> {
            System.out.println("正在初始化演示用户数据...");

            // 为了演示方便，所有普通用户的密码都设置为 'password'
            String defaultPassword = passwordEncoder.encode("password");

            User user1 = new User();
            user1.setId("user001");
            user1.setName("普通员工-张三");
            user1.setRole("USER");
            user1.setPassword(defaultPassword);

            User user2 = new User();
            user2.setId("manager001");
            user2.setName("部门经理-李四");
            user2.setRole("USER");
            user2.setPassword(defaultPassword);

            User user3 = new User();
            user3.setId("hr001");
            user3.setName("财务总监-王五");
            user3.setRole("USER");
            user3.setPassword(defaultPassword);

            User admin = new User();
            admin.setId("admin");
            admin.setName("系统管理员");
            admin.setRole("ADMIN");
            admin.setPassword(passwordEncoder.encode("admin")); // 管理员密码设为 'admin'

            userRepository.saveAll(List.of(user1, user2, user3, admin));
            System.out.println("演示用户数据初始化完成！");
        };
    }
}