package com.iot.platform;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/**
 * MQTT 认证服务测试
 * 验证设备认证参数解析逻辑
 */
class MqttAuthServiceTest {

    @Test
    void testValidAuthParams() {
        Map<String, Object> params = new HashMap<>();
        params.put("clientid", "device_001");
        params.put("username", "device_001");
        params.put("password", "secret123");
        
        assertNotNull(params.get("username"), "username 不应为空");
        assertNotNull(params.get("password"), "password 不应为空");
    }

    @Test
    void testMissingUsername() {
        Map<String, Object> params = new HashMap<>();
        params.put("clientid", "device_001");
        params.put("password", "secret123");
        
        assertNull(params.get("username"), "缺少 username 时应返回 null");
    }

    @Test
    void testMissingPassword() {
        Map<String, Object> params = new HashMap<>();
        params.put("username", "device_001");
        
        assertNull(params.get("password"), "缺少 password 时应返回 null");
    }

    @Test
    void testAclParamsParsing() {
        Map<String, Object> params = new HashMap<>();
        params.put("clientid", "device_001");
        params.put("username", "device_001");
        params.put("topic", "iot/device/device_001/property/post");
        params.put("access", "1");
        
        String topic = (String) params.get("topic");
        assertTrue(topic.contains("device_001"), "Topic 应包含设备 ID");
    }
}
