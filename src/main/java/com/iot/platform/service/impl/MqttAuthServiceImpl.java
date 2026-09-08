package com.iot.platform.service.impl;

import cn.hutool.core.util.StrUtil;
import com.iot.platform.model.entity.Device;
import com.iot.platform.repository.DeviceMapper;
import com.iot.platform.service.MqttAuthService;
import com.iot.platform.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * MQTT 设备认证鉴权服务实现
 * <p>
 * 实现 EMQX HTTP Auth 插件的认证与 ACL 回调逻辑：
 * <ul>
 *     <li>认证：以 username(deviceId) 查询 device 表，比对 deviceSecret；结果缓存至 Redis（5分钟）</li>
 *     <li>ACL：设备只能发布/订阅 iot/device/{自己的 deviceId}/# 主题</li>
 * </ul>
 * 由于 EMQX 回调不携带 JWT，无法识别租户上下文，设备查询时临时关闭租户隔离
 * （按 deviceId 全局匹配，deviceId 在租户内已唯一）。
 *
 * @author iot-platform
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MqttAuthServiceImpl implements MqttAuthService {

    /** 设备认证结果缓存 Key 前缀，格式：iot:mqtt:auth:{deviceId}:{secretHash} */
    private static final String AUTH_CACHE_PREFIX = "iot:mqtt:auth:";

    /** 认证缓存 TTL（分钟） */
    private static final long AUTH_CACHE_TTL_MINUTES = 5;

    /** 设备 Topic 前缀（与 MqttTopicHandler 保持一致） */
    private static final String DEVICE_TOPIC_PREFIX = "iot/device/";

    private final DeviceMapper deviceMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public boolean authenticate(Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return false;
        }
        // EMQX 以 username 传入 deviceId，password 传入 deviceSecret
        String deviceId = getString(params, "username");
        if (StrUtil.isBlank(deviceId)) {
            // 部分版本以 clientid 兜底
            deviceId = getString(params, "clientid");
        }
        String password = getString(params, "password");
        if (StrUtil.isBlank(deviceId) || StrUtil.isBlank(password)) {
            log.warn("MQTT 认证参数缺失: params={}", params);
            return false;
        }

        // 优先读取 Redis 缓存认证结果
        String cacheKey = buildCacheKey(deviceId, password);
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return Boolean.TRUE.toString().equalsIgnoreCase(cached.toString());
        }

        // 未命中缓存，查询数据库（EMQX 回调无租户上下文，临时关闭租户隔离）
        boolean passed;
        try {
            TenantContext.setIgnore(true);
            Device device = deviceMapper.selectByDeviceId(deviceId);
            passed = device != null && password.equals(device.getDeviceSecret());
        } finally {
            TenantContext.clear();
        }

        // 缓存认证结果，降低 EMQX 高频回调对数据库的冲击
        redisTemplate.opsForValue().set(cacheKey, String.valueOf(passed),
                AUTH_CACHE_TTL_MINUTES, TimeUnit.MINUTES);

        if (passed) {
            log.debug("MQTT 设备认证通过: deviceId={}", deviceId);
        } else {
            log.warn("MQTT 设备认证失败: deviceId={}", deviceId);
        }
        return passed;
    }

    @Override
    public boolean checkAcl(Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return false;
        }
        // EMQX 透传 clientid（=deviceId）、topic、access(publish=2/subscribe=1)
        String deviceId = getString(params, "username");
        if (StrUtil.isBlank(deviceId)) {
            deviceId = getString(params, "clientid");
        }
        String topic = getString(params, "topic");
        if (StrUtil.isBlank(deviceId) || StrUtil.isBlank(topic)) {
            log.warn("MQTT ACL 参数缺失: params={}", params);
            return false;
        }

        // 设备仅允许操作自身 Topic：iot/device/{自己的 deviceId}/#
        String allowedPrefix = DEVICE_TOPIC_PREFIX + deviceId + "/";
        boolean allowed = topic.startsWith(allowedPrefix);
        if (!allowed) {
            log.warn("MQTT ACL 拒绝: deviceId={}, topic={}", deviceId, topic);
        }
        return allowed;
    }

    /**
     * 构建认缓存 Key
     */
    private String buildCacheKey(String deviceId, String password) {
        // 简单拼接，避免引入额外摘要；密钥本身就是机密信息
        return AUTH_CACHE_PREFIX + deviceId + ":" + (password == null ? "" : password.hashCode());
    }

    /**
     * 从参数 Map 中安全获取字符串
     */
    private String getString(Map<String, Object> params, String key) {
        if (params == null || !params.containsKey(key)) {
            return null;
        }
        Object value = params.get(key);
        return value == null ? null : value.toString();
    }
}
