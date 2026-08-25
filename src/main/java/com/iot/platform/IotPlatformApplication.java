package com.iot.platform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * IoT 设备接入管控平台启动类
 * <p>
 * 面向中小企业的物联网设备接入、监控、告警与远程控制平台。
 * <p>
 * 启用能力：
 * <ul>
 *     <li>@EnableAsync：异步处理 MQTT 消息，避免阻塞回调线程</li>
 *     <li>@EnableScheduling：定时任务，用于设备心跳检测</li>
 * </ul>
 *
 * @author iot-platform
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
@MapperScan("com.iot.platform.repository")
public class IotPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(IotPlatformApplication.class, args);
        System.out.println("""

                ====================================================
                  IoT 设备接入管控平台 启动成功
                  API 文档: http://localhost:8080/iot/doc.html
                  Swagger: http://localhost:8080/iot/swagger-ui.html
                ====================================================""");
    }
}
