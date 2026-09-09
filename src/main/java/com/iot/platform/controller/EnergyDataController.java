package com.iot.platform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.iot.platform.common.Result;
import com.iot.platform.model.entity.EnergyReading;
import com.iot.platform.repository.EnergyReadingMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 能耗数据 Controller
 * <p>
 * 提供能耗历史数据查询、实时读数获取及每日能耗汇总等接口。
 *
 * @author iot-platform
 */
@Slf4j
@RestController
@RequestMapping("/v1/energy/data")
@RequiredArgsConstructor
@Tag(name = "能耗数据", description = "能耗历史查询、实时读数、汇总统计接口")
public class EnergyDataController {

    private final EnergyReadingMapper energyReadingMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String ENERGY_LATEST_KEY = "iot:energy:latest:";

    @Operation(summary = "查询历史能耗数据", description = "按时间范围分页查询能源表历史读数")
    @GetMapping("/history/{meterCode}")
    public Result<List<EnergyReading>> queryHistory(
            @Parameter(description = "能源表编号") @PathVariable String meterCode,
            @Parameter(description = "开始时间（epoch毫秒）") @RequestParam Long startTime,
            @Parameter(description = "结束时间（epoch毫秒）") @RequestParam Long endTime,
            @Parameter(description = "页码，默认1") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数，默认100") @RequestParam(defaultValue = "100") int size) {
        LambdaQueryWrapper<EnergyReading> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EnergyReading::getMeterCode, meterCode)
                .ge(EnergyReading::getTimestamp, startTime)
                .le(EnergyReading::getTimestamp, endTime)
                .orderByDesc(EnergyReading::getTimestamp);

        Page<EnergyReading> pageResult = energyReadingMapper.selectPage(new Page<>(page, size), wrapper);
        return Result.success(pageResult.getRecords());
    }

    @Operation(summary = "获取实时读数", description = "从 Redis 缓存获取能源表最新读数")
    @GetMapping("/realtime/{meterCode}")
    public Result<EnergyReading> getRealtimeReading(
            @Parameter(description = "能源表编号") @PathVariable String meterCode) {
        Object cached = redisTemplate.opsForValue().get(ENERGY_LATEST_KEY + meterCode);
        if (cached instanceof EnergyReading) {
            return Result.success((EnergyReading) cached);
        }
        return Result.success(null);
    }

    @Operation(summary = "获取今日能耗汇总", description = "查询当日总用电量及峰谷平分时段统计")
    @GetMapping("/summary/{meterCode}")
    public Result<Map<String, Object>> getEnergySummary(
            @Parameter(description = "能源表编号") @PathVariable String meterCode) {
        // 计算今日零点与明日零点的时间戳
        ZoneId zoneId = ZoneId.systemDefault();
        long startOfToday = LocalDate.now().atStartOfDay(zoneId).toInstant().toEpochMilli();
        long startOfTomorrow = LocalDate.now().plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli();

        LambdaQueryWrapper<EnergyReading> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EnergyReading::getMeterCode, meterCode)
                .ge(EnergyReading::getTimestamp, startOfToday)
                .lt(EnergyReading::getTimestamp, startOfTomorrow);

        List<EnergyReading> readings = energyReadingMapper.selectList(wrapper);

        double totalConsumption = 0.0;
        double peakConsumption = 0.0;
        double valleyConsumption = 0.0;
        double flatConsumption = 0.0;

        for (EnergyReading reading : readings) {
            double consumption = reading.getEnergyConsumption() != null ? reading.getEnergyConsumption() : 0.0;
            totalConsumption += consumption;
            if (reading.getPeakFlag() != null) {
                switch (reading.getPeakFlag()) {
                    case 1 -> peakConsumption += consumption;
                    case 2 -> valleyConsumption += consumption;
                    default -> flatConsumption += consumption;
                }
            }
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("meterCode", meterCode);
        summary.put("date", LocalDate.now().toString());
        summary.put("totalConsumption", Math.round(totalConsumption * 1000) / 1000.0);
        summary.put("peakConsumption", Math.round(peakConsumption * 1000) / 1000.0);
        summary.put("valleyConsumption", Math.round(valleyConsumption * 1000) / 1000.0);
        summary.put("flatConsumption", Math.round(flatConsumption * 1000) / 1000.0);
        summary.put("recordCount", readings.size());

        return Result.success(summary);
    }
}
