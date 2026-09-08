package com.iot.platform.controller;

import com.iot.platform.common.Result;
import com.iot.platform.model.dto.EnergyMeterCreateDTO;
import com.iot.platform.model.entity.EnergyMeter;
import com.iot.platform.model.entity.EnergyReading;
import com.iot.platform.service.EnergyMeterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 能源表管理 Controller
 * <p>
 * 提供能源表的创建、查询、删除及最新读数获取等接口。
 *
 * @author iot-platform
 */
@Slf4j
@RestController
@RequestMapping("/iot/energy/meter")
@RequiredArgsConstructor
@Tag(name = "能源表管理", description = "能源表创建、查询、删除相关接口")
public class EnergyMeterController {

    private final EnergyMeterService energyMeterService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String ENERGY_LATEST_KEY = "iot:energy:latest:";

    @Operation(summary = "创建能源表", description = "创建新的能源计量仪表，支持电表/水表/燃气表")
    @PostMapping
    public Result<EnergyMeter> createMeter(@Valid @RequestBody EnergyMeterCreateDTO dto) {
        log.info("创建能源表请求: meterCode={}", dto.getMeterCode());
        return Result.success(energyMeterService.createMeter(dto));
    }

    @Operation(summary = "查询能源表详情", description = "根据主键ID查询能源表信息")
    @GetMapping("/{id}")
    public Result<EnergyMeter> getMeter(
            @Parameter(description = "能源表ID") @PathVariable Long id) {
        return Result.success(energyMeterService.getMeterById(id));
    }

    @Operation(summary = "分页查询能源表列表", description = "支持按能源表类型过滤")
    @GetMapping("/list")
    public Result<List<EnergyMeter>> listMeters(
            @Parameter(description = "能源表类型：ELECTRICITY/WATER/GAS") @RequestParam(required = false) String meterType,
            @Parameter(description = "页码，默认1") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数，默认20") @RequestParam(defaultValue = "20") int size) {
        return Result.success(energyMeterService.listMeters(meterType, page, size));
    }

    @Operation(summary = "删除能源表", description = "删除能源表并清理缓存")
    @DeleteMapping("/{id}")
    public Result<Void> deleteMeter(@PathVariable Long id) {
        energyMeterService.deleteMeter(id);
        return Result.success();
    }

    @Operation(summary = "获取最新读数", description = "从 Redis 缓存获取能源表最新读数")
    @GetMapping("/{meterCode}/latest")
    public Result<EnergyReading> getLatestReading(
            @Parameter(description = "能源表编号") @PathVariable String meterCode) {
        Object cached = redisTemplate.opsForValue().get(ENERGY_LATEST_KEY + meterCode);
        if (cached instanceof EnergyReading) {
            return Result.success((EnergyReading) cached);
        }
        return Result.success(null);
    }
}
