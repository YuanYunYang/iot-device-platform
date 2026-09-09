package com.iot.platform.controller;

import com.iot.platform.common.Result;
import com.iot.platform.model.entity.Alarm;
import com.iot.platform.service.AlarmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 告警查询 Controller
 * <p>
 * 提供告警记录查询接口，支持按设备、等级过滤及最近告警查询。
 *
 * @author iot-platform
 */
@Slf4j
@RestController
@RequestMapping("/v1/alarm")
@RequiredArgsConstructor
@Tag(name = "告警管理", description = "告警记录查询接口")
public class AlarmController {

    private final AlarmService alarmService;

    @Operation(summary = "按设备查询告警", description = "分页查询指定设备的告警记录")
    @GetMapping("/device/{deviceId}")
    public Result<List<Alarm>> listAlarmsByDevice(
            @Parameter(description = "设备ID") @PathVariable String deviceId,
            @Parameter(description = "告警等级：INFO/WARNING/CRITICAL") @RequestParam(required = false) String level,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int size) {
        return Result.success(alarmService.listAlarmsByDevice(deviceId, level, page, size));
    }

    @Operation(summary = "查询最近告警", description = "获取最近的告警记录列表")
    @GetMapping("/recent")
    public Result<List<Alarm>> listRecentAlarms(
            @Parameter(description = "返回条数") @RequestParam(defaultValue = "50") int limit) {
        return Result.success(alarmService.listRecentAlarms(limit));
    }
}
