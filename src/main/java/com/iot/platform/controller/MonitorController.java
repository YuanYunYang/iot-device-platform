package com.iot.platform.controller;

import com.iot.platform.common.Result;
import com.iot.platform.model.vo.MonitorDataVO;
import com.iot.platform.service.MqttMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 实时监控 Controller
 * <p>
 * 提供设备监控数据查询接口，数据来源于 InfluxDB 时序存储。
 * 实时数据推送通过 WebSocket（/ws/device）完成。
 *
 * @author iot-platform
 */
@Slf4j
@RestController
@RequestMapping("/monitor")
@RequiredArgsConstructor
@Tag(name = "实时监控", description = "设备监控数据查询接口")
public class MonitorController {

    private final MqttMessageService mqttMessageService;

    @Operation(summary = "查询设备历史数据", description = "从 InfluxDB 查询设备属性的历史监控数据")
    @GetMapping("/history/{deviceId}")
    public Result<List<MonitorDataVO>> queryHistory(
            @Parameter(description = "设备ID") @PathVariable String deviceId,
            @Parameter(description = "属性标识，如 temperature") @RequestParam(required = false) String identifier,
            @Parameter(description = "返回条数") @RequestParam(defaultValue = "100") int limit) {
        return Result.success(mqttMessageService.queryHistory(deviceId, identifier, limit));
    }
}
