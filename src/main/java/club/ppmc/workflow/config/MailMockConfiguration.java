package club.ppmc.workflow.config;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;

import java.io.InputStream;
import java.util.Arrays;

/**
 * @author cc
 * @description 【新增】邮件模拟配置 (仅在 dev 环境下激活)
 * <p>
 * 这个配置类创建了一个 JavaMailSender 的模拟实现 (Mock Bean)。
 * 当应用程序以 "dev" profile 运行时，Spring Boot 会使用这个 Bean 来代替 application.properties 中配置的真实邮件服务器。
 * 它的 send() 方法不会真正发送邮件，而是将邮件内容打印到控制台日志中，便于开发和调试。
 */
@Configuration
@Profile("dev") // 关键注解：指定此配置仅在 "dev" profile 激活时生效
@Slf4j
public class MailMockConfiguration {

    @Bean
    public JavaMailSender javaMailSender() {
        log.warn("===============================================================");
        log.warn("=====           邮件服务模拟器 (Mail Mock) 已激活         =====");
        log.warn("=====         所有邮件将被打印到控制台，不会真实发送         =====");
        log.warn("===============================================================");
        return new MockJavaMailSender();
    }

    /**
     * 一个实现了 JavaMailSender 接口的内部类，用于模拟邮件发送行为。
     */
    public static class MockJavaMailSender implements JavaMailSender {

        @Override
        public MimeMessage createMimeMessage() {
            // 在我们的场景中，我们只使用 SimpleMailMessage，所以这个方法可以返回 null 或一个空的 MimeMessage 实例。
            return null;
        }

        @Override
        public MimeMessage createMimeMessage(InputStream contentStream) throws MailException {
            return null;
        }

        @Override
        public void send(MimeMessage mimeMessage) throws MailException {
            // Not implemented for mock
        }

        @Override
        public void send(MimeMessage... mimeMessages) throws MailException {
            // Not implemented for mock
        }

        @Override
        public void send(MimeMessagePreparator mimeMessagePreparator) throws MailException {
            // Not implemented for mock
        }

        @Override
        public void send(MimeMessagePreparator... mimeMessagePreparators) throws MailException {
            // Not implemented for mock
        }

        @Override
        public void send(SimpleMailMessage simpleMessage) throws MailException {
            log.info("--- [模拟邮件发送] ---");
            log.info("发件人: {}", simpleMessage.getFrom());
            log.info("收件人: {}", Arrays.toString(simpleMessage.getTo()));
            log.info("主  题: {}", simpleMessage.getSubject());
            log.info("内  容:\n---\n{}\n---", simpleMessage.getText());
            log.info("--- [邮件发送结束] ---");
        }

        @Override
        public void send(SimpleMailMessage... simpleMessages) throws MailException {
            for (SimpleMailMessage message : simpleMessages) {
                send(message);
            }
        }
    }
}