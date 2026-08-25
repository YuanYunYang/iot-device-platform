package com.iot.platform.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * MQTT 连接配置
 * <p>
 * 绑定 application.yml 中 mqtt 前缀的配置项，供 MqttClientManager 读取。
 * 平台作为 MQTT 客户端连接到 Broker，订阅所有设备 Topic 并下发控制指令。
 *
 * @author iot-platform
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "mqtt")
public class MqttConfig {

    /** MQTT Broker 地址，如 tcp://localhost:1883 */
    private String host;

    /** 平台客户端ID */
    private String clientId;

    /** 用户名 */
    private String username;

    /** 密码 */
    private String password;

    /** 默认 QoS */
    private int qos;

    /** 心跳保活间隔（秒） */
    private int keepAliveInterval;

    /** 连接超时时间（秒） */
    private int connectionTimeout;

    /** 是否清除会话 */
    private boolean cleanSession;

    /** 是否自动重连 */
    private boolean automaticReconnect;

    /** 订阅主题前缀（通配符订阅所有设备消息） */
    private String topicPrefix;
}
