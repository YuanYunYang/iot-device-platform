package com.iot.platform.timer;

import com.iot.platform.config.RedisConfig;
import com.iot.platform.model.entity.Alarm;
import com.iot.platform.model.enums.AlarmLevelEnum;
import com.iot.platform.model.enums.DeviceStatusEnum;
import com.iot.platform.repository.DeviceMapper;
import com.iot.platform.service.AlarmService;
import com.iot.platform.service.DeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Set;

/**
 * 设备心跳检测定时任务
 * <p>
 * 每 60 秒扫描一次设备在线状态：
 * <ol>
 *     <li>批量将数据库中心跳超时（超过阈值未上报）的设备置为 OFFLINE</li>
 *     <li>遍历 Redis 中的设备心跳 Key，二次校验并清理过期缓存</li>
 *     <li>对检测到离线的设备生成 LIFECYCLE 告警</li>
 * </ol>
 *
 * @author iot-platform
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceHeartbeatTask {

    private final DeviceService deviceService;
    private final DeviceMapper deviceMapper;
    private final AlarmService alarmService;
    private final RedisTemplate<String, Object> redisTemplate;

    /** 心跳超时阈值（秒），默认 90 秒 */
    @Value("${iot.heartbeat.timeout:90}")
    private int heartbeatTimeout;

    /**
     * 定时心跳扫描，固定延迟 60 秒执行一次
     */
    @Scheduled(fixedDelayString = "${iot.heartbeat.scan-interval:60000}")
    public void scanHeartbeat() {
        log.debug("开始执行设备心跳检测，超时阈值={}秒", heartbeatTimeout);

        // 批量更新数据库中超时设备状态为离线
        int offlineCount = deviceService.markTimeoutDevicesOffline(heartbeatTimeout);
        if (offlineCount > 0) {
            log.info("心跳检测：{} 台设备超时离线", offlineCount);
        }

        // 扫描 Redis 心跳缓存，二次校验并清理
        scanRedisHeartbeats();
    }

    /**
     * 扫描 Redis 中的设备心跳缓存
     */
    private void scanRedisHeartbeats() {
        Set<String> keys = redisTemplate.keys(RedisConfig.DEVICE_HEARTBEAT_KEY + "*");
        if (keys == null || keys.isEmpty()) {
            return;
        }

        long now = LocalDateTime.now().toEpochSecond(ZoneOffset.of("+8"));
        long threshold = now - heartbeatTimeout;

        for (String key : keys) {
            String deviceId = key.substring(RedisConfig.DEVICE_HEARTBEAT_KEY.length());
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                continue;
            }
            try {
                long lastHeartbeat = Long.parseLong(value.toString());
                if (lastHeartbeat < threshold) {
                    // 心跳超时，更新状态并产生离线告警
                    handleTimeoutDevice(deviceId);
                    // 清理过期缓存
                    redisTemplate.delete(key);
                }
            } catch (NumberFormatException e) {
                log.warn("心跳缓存格式异常: key={}, value={}", key, value);
                redisTemplate.delete(key);
            }
        }
    }

    /**
     * 处理超时设备：置离线并生成告警
     */
    private void handleTimeoutDevice(String deviceId) {
        var device = deviceMapper.selectByDeviceId(deviceId);
        if (device == null) {
            return;
        }
        // 仅对当前在线设备产生离线告警，避免重复
        if (DeviceStatusEnum.ONLINE.getCode().equalsIgnoreCase(device.getStatus())) {
            deviceService.updateDeviceStatus(deviceId, DeviceStatusEnum.OFFLINE.getCode(), null);
            Alarm alarm = alarmService.createAlarm(
                    deviceId, device.getProductId(),
                    AlarmLevelEnum.INFO.getCode(),
                    "LIFECYCLE",
                    "设备心跳超时离线",
                    "设备 " + deviceId + " 超过 " + heartbeatTimeout + " 秒未上报心跳，判定为离线",
                    null, null);
            log.info("设备心跳超时，已生成离线告警: deviceId={}, alarmId={}", deviceId, alarm.getId());
        }
    }
}
