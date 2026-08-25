package com.iot.platform;

import com.iot.platform.mqtt.MqttTopicHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 平台测试类
 * <p>
 * 包含基础单元测试，验证核心组件可被 Spring 容器加载、核心工具逻辑正确。
 * 完整集成测试需启动 docker-compose 依赖（MySQL/Redis/MQTT/InfluxDB）。
 *
 * @author iot-platform
 */
@SpringBootTest
class IotPlatformApplicationTests {

    @Autowired
    private MqttTopicHandler mqttTopicHandler;

    /**
     * 验证 Topic 解析逻辑正确性
     */
    @Test
    void testTopicParse() {
        String topic = "iot/device/device_001/property";
        String deviceId = mqttTopicHandler.parseDeviceId(topic);
        String messageType = mqttTopicHandler.parseMessageType(topic);

        Assertions.assertEquals("device_001", deviceId);
        Assertions.assertEquals("property", messageType);
    }

    /**
     * 验证控制指令 Topic 构建
     */
    @Test
    void testCommandTopicBuild() {
        String topic = mqttTopicHandler.buildCommandTopic("device_002");
        Assertions.assertEquals("iot/device/device_002/command", topic);
    }

    /**
     * 验证指令响应 Topic 构建
     */
    @Test
    void testCommandRespTopicBuild() {
        String topic = mqttTopicHandler.buildCommandRespTopic("device_002");
        Assertions.assertEquals("iot/device/device_002/command/resp", topic);
    }

    /**
     * 验证非法 Topic 解析返回 null
     */
    @Test
    void testInvalidTopicParse() {
        Assertions.assertNull(mqttTopicHandler.parseDeviceId("invalid/topic"));
        Assertions.assertNull(mqttTopicHandler.parseMessageType("invalid/topic"));
    }
}
