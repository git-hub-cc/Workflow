package club.ppmc.workflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * @author 你的名字
 * @description Camunda 相关的配置类
 */
@Configuration
public class CamundaConfig {

    /**
     * 创建一个 RestTemplate Bean，用于可能的手动 API 调用。
     * @return RestTemplate 实例
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}