package com.iot.platform.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iot.platform.model.entity.Device;
import com.iot.platform.model.entity.EnergyReading;
import com.iot.platform.repository.DeviceMapper;
import com.iot.platform.repository.EnergyReadingMapper;
import com.iot.platform.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 演示环境启动后预热 Redis：设备在线状态 + 能源表最新读数，
 * 保证前端看板无需真实 MQTT 上报也能展示“活数据”。
 */
@Slf4j
@Component
@Profile("demo")
@RequiredArgsConstructor
public class DemoCacheWarmer implements ApplicationRunner {

    private static final String ENERGY_LATEST_KEY = "iot:energy:latest:";

    private final DeviceMapper deviceMapper;
    private final EnergyReadingMapper energyReadingMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void run(ApplicationArguments args) {
        TenantContext.setIgnore(true);
        try {
            warmDeviceStatus();
            warmEnergyLatest();
            log.info("演示缓存预热完成");
        } catch (Exception e) {
            log.warn("演示缓存预热失败（不影响启动）: {}", e.getMessage());
        } finally {
            TenantContext.clear();
        }
    }

    private void warmDeviceStatus() {
        List<Device> devices = deviceMapper.selectList(new LambdaQueryWrapper<>());
        for (Device d : devices) {
            if (d.getDeviceId() == null || d.getStatus() == null) {
                continue;
            }
            redisTemplate.opsForValue().set(
                    RedisConfig.DEVICE_STATUS_KEY + d.getDeviceId(),
                    d.getStatus(),
                    1,
                    TimeUnit.DAYS);
            if (d.getLastHeartbeatTime() != null) {
                redisTemplate.opsForValue().set(
                        RedisConfig.DEVICE_HEARTBEAT_KEY + d.getDeviceId(),
                        d.getLastHeartbeatTime().toString(),
                        1,
                        TimeUnit.DAYS);
            }
        }
        log.info("预热设备状态缓存: {} 台", devices.size());
    }

    private void warmEnergyLatest() {
        // 取每个 meter_code 最新一条读数写入 Redis
        List<EnergyReading> latest = energyReadingMapper.selectList(
                new LambdaQueryWrapper<EnergyReading>()
                        .orderByDesc(EnergyReading::getTimestamp)
                        .last("LIMIT 500"));
        int count = 0;
        for (EnergyReading r : latest) {
            if (r.getMeterCode() == null) {
                continue;
            }
            String key = ENERGY_LATEST_KEY + r.getMeterCode();
            if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
                continue;
            }
            redisTemplate.opsForValue().set(key, r, 1, TimeUnit.DAYS);
            count++;
        }
        log.info("预热能源最新读数缓存: {} 个表", count);
    }
}
