package com.iot.platform.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.iot.platform.common.GlobalExceptionHandler;
import com.iot.platform.common.ResultCode;
import com.iot.platform.model.dto.EnergyMeterCreateDTO;
import com.iot.platform.model.entity.EnergyMeter;
import com.iot.platform.model.entity.EnergyReading;
import com.iot.platform.repository.EnergyMeterMapper;
import com.iot.platform.repository.EnergyReadingMapper;
import com.iot.platform.service.EnergyMeterService;
import com.iot.platform.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

/**
 * 能源表服务实现
 * <p>
 * 实现能源表创建、查询、删除及 MQTT 能耗数据处理等核心逻辑。
 * 能耗读数实时缓存至 Redis（iot:energy:latest:{meterCode}），
 * 并持久化至 MySQL energy_reading 表用于后续导出与汇总统计。
 *
 * @author iot-platform
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnergyMeterServiceImpl extends ServiceImpl<EnergyMeterMapper, EnergyMeter> implements EnergyMeterService {

    /** Redis 能耗最新读数缓存 Key 前缀 */
    private static final String ENERGY_LATEST_KEY = "iot:energy:latest:";

    private final EnergyMeterMapper energyMeterMapper;
    private final EnergyReadingMapper energyReadingMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public EnergyMeter createMeter(EnergyMeterCreateDTO dto) {
        // 校验能源表编号唯一性
        EnergyMeter exist = energyMeterMapper.selectByMeterCode(dto.getMeterCode());
        if (exist != null) {
            throw new GlobalExceptionHandler.BusinessException(
                    ResultCode.DEVICE_ALREADY_EXISTS, "能源表编号已存在: " + dto.getMeterCode());
        }

        // 构建能源表实体
        EnergyMeter meter = new EnergyMeter();
        meter.setMeterCode(dto.getMeterCode());
        meter.setMeterName(dto.getMeterName());
        meter.setMeterType(dto.getMeterType());
        meter.setProtocol(dto.getProtocol());
        meter.setLocation(dto.getLocation());
        meter.setProductId(dto.getProductId());
        meter.setDeviceId(dto.getDeviceId());
        meter.setModbusAddr(dto.getModbusAddr());
        meter.setModbusPort(dto.getModbusPort());
        meter.setCtRatio(dto.getCtRatio());
        meter.setPtRatio(dto.getPtRatio());
        meter.setStatus(1);

        energyMeterMapper.insert(meter);
        log.info("能源表创建成功: meterCode={}, meterType={}", dto.getMeterCode(), dto.getMeterType());

        return meter;
    }

    @Override
    public EnergyMeter getMeterById(Long id) {
        EnergyMeter meter = energyMeterMapper.selectById(id);
        if (meter == null) {
            throw new GlobalExceptionHandler.BusinessException(
                    ResultCode.NOT_FOUND, "能源表不存在: " + id);
        }
        return meter;
    }

    @Override
    public List<EnergyMeter> listMeters(String meterType, int page, int size) {
        LambdaQueryWrapper<EnergyMeter> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(meterType)) {
            wrapper.eq(EnergyMeter::getMeterType, meterType);
        }
        wrapper.orderByDesc(EnergyMeter::getCreateTime);

        Page<EnergyMeter> pageResult = energyMeterMapper.selectPage(new Page<>(page, size), wrapper);
        return pageResult.getRecords();
    }

    @Override
    public void deleteMeter(Long id) {
        EnergyMeter meter = energyMeterMapper.selectById(id);
        if (meter == null) {
            throw new GlobalExceptionHandler.BusinessException(
                    ResultCode.NOT_FOUND, "能源表不存在: " + id);
        }
        energyMeterMapper.deleteById(id);
        // 清理 Redis 缓存
        if (meter.getMeterCode() != null) {
            redisTemplate.delete(ENERGY_LATEST_KEY + meter.getMeterCode());
        }
        log.info("能源表已删除: id={}, meterCode={}", id, meter.getMeterCode());
    }

    @Override
    public void handleEnergyData(String deviceId, Map<String, Object> payload) {
        String meterCode = parseString(payload.get("meterCode"));

        // MQTT 消息处理上下文无租户信息，先忽略租户隔离查询能源表，再恢复租户上下文
        try {
            TenantContext.setIgnore(true);

            EnergyMeter meter = null;
            if (StrUtil.isNotBlank(meterCode)) {
                meter = energyMeterMapper.selectByMeterCode(meterCode);
            }
            if (meter == null && StrUtil.isNotBlank(deviceId)) {
                meter = getOne(new LambdaQueryWrapper<EnergyMeter>()
                        .eq(EnergyMeter::getDeviceId, deviceId));
            }

            if (meter == null) {
                log.warn("能耗数据上报失败，能源表不存在: deviceId={}, meterCode={}", deviceId, meterCode);
                return;
            }

            // 恢复租户上下文，后续写操作按租户隔离执行
            TenantContext.setIgnore(false);
            if (meter.getTenantId() != null) {
                TenantContext.setTenantId(meter.getTenantId());
            }

            Long meterId = meter.getId();
            meterCode = meter.getMeterCode();
            String meterType = parseString(payload.get("meterType"));
            if (StrUtil.isBlank(meterType)) {
                meterType = meter.getMeterType();
            }

            // 解析采集时间戳
            Long timestamp = parseLong(payload.get("timestamp"));
            if (timestamp == null) {
                timestamp = System.currentTimeMillis();
            }

            // 解析各项能耗指标
            Double voltage = parseDouble(payload.get("voltage"));
            Double current = parseDouble(payload.get("current"));
            Double activePower = parseDouble(payload.get("activePower"));
            Double reactivePower = parseDouble(payload.get("reactivePower"));
            Double powerFactor = parseDouble(payload.get("powerFactor"));
            Double frequency = parseDouble(payload.get("frequency"));
            Double energyConsumption = parseDouble(payload.get("energyConsumption"));
            Double cumulativeEnergy = parseDouble(payload.get("cumulativeEnergy"));

            // 根据小时判断峰谷平
            int peakFlag = determinePeakFlag(timestamp);

            // 构建读数实体
            EnergyReading reading = new EnergyReading();
            reading.setMeterId(meterId);
            reading.setMeterCode(meterCode);
            reading.setMeterType(meterType);
            reading.setTimestamp(timestamp);
            reading.setVoltage(voltage);
            reading.setCurrent(current);
            reading.setActivePower(activePower);
            reading.setReactivePower(reactivePower);
            reading.setPowerFactor(powerFactor);
            reading.setFrequency(frequency);
            reading.setEnergyConsumption(energyConsumption);
            reading.setCumulativeEnergy(cumulativeEnergy);
            reading.setPeakFlag(peakFlag);

            // 持久化读数到 MySQL
            energyReadingMapper.insert(reading);

            // 缓存最新读数到 Redis
            redisTemplate.opsForValue().set(ENERGY_LATEST_KEY + meterCode, reading);

            log.info("能耗数据上报成功: deviceId={}, meterCode={}, voltage={}V, current={}A, " +
                            "activePower={}kW, powerFactor={}, frequency={}Hz, " +
                            "energyConsumption={}kWh, cumulativeEnergy={}kWh, peakFlag={}",
                    deviceId, meterCode, voltage, current, activePower,
                    powerFactor, frequency, energyConsumption, cumulativeEnergy, peakFlag);
        } catch (Exception e) {
            log.error("处理能耗数据异常: deviceId={}, meterCode={}", deviceId, meterCode, e);
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * 根据时间戳判断峰谷平标志
     * <p>
     * 峰: 8-11时、18-21时
     * 谷: 0-7时
     * 平: 其他时段
     *
     * @param timestamp epoch 毫秒时间戳
     * @return 0-平 1-峰 2-谷
     */
    private int determinePeakFlag(long timestamp) {
        int hour = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()).getHour();
        if ((hour >= 8 && hour <= 11) || (hour >= 18 && hour <= 21)) {
            return 1; // 峰
        } else if (hour >= 0 && hour <= 7) {
            return 2; // 谷
        } else {
            return 0; // 平
        }
    }

    /**
     * 安全解析字符串
     */
    private String parseString(Object value) {
        return value == null ? null : value.toString();
    }

    /**
     * 安全解析 Long
     */
    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 安全解析 Double
     */
    private Double parseDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
