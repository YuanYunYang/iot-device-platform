package com.iot.platform.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j (OpenAPI 3) 接口文档配置
 * <p>
 * 配置 API 文档元信息，启动后访问：
 * <ul>
 *     <li>Knife4j 增强文档：http://localhost:8080/iot/doc.html</li>
 *     <li>原生 Swagger UI：http://localhost:8080/iot/swagger-ui.html</li>
 * </ul>
 *
 * @author iot-platform
 */
@Configuration
public class KnifeConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("IoT 设备接入管控平台 API 文档")
                        .description("物联网设备接入、监控、告警与远程控制接口文档")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("iot-platform")
                                .email("dev@iot-platform.com")));
    }
}
