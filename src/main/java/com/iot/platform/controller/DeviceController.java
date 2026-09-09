package com.iot.platform.controller;

import com.iot.platform.common.Result;
import com.iot.platform.model.dto.DeviceControlDTO;
import com.iot.platform.model.dto.DeviceRegisterDTO;
import com.iot.platform.model.vo.DeviceVO;
import com.iot.platform.service.DeviceControlService;
import com.iot.platform.service.DeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 设备管理 Controller
 * <p>
 * 提供设备注册、查询、删除及远程控制等接口。
 *
 * @author iot-platform
 */
@Slf4j
@RestController
@RequestMapping("/v1/device")
@RequiredArgsConstructor
@Tag(name = "设备管理", description = "设备注册、查询、控制相关接口")
public class DeviceController {

    private final DeviceService deviceService;
    private final DeviceControlService deviceControlService;

    @Operation(summary = "注册设备", description = "将新设备注册到平台，关联产品并继承物模型")
    @PostMapping("/register")
    public Result<DeviceVO> register(@Valid @RequestBody DeviceRegisterDTO dto) {
        log.info("注册设备请求: deviceId={}", dto.getDeviceId());
        return Result.success(deviceService.register(dto));
    }

    @Operation(summary = "查询设备详情", description = "根据设备ID查询设备信息及实时状态")
    @GetMapping("/{deviceId}")
    public Result<DeviceVO> getDevice(
            @Parameter(description = "设备ID") @PathVariable String deviceId) {
        return Result.success(deviceService.getDeviceByDeviceId(deviceId));
    }

    @Operation(summary = "分页查询设备列表", description = "支持按状态过滤")
    @GetMapping("/list")
    public Result<List<DeviceVO>> listDevices(
            @Parameter(description = "设备状态：ONLINE/OFFLINE/UNKNOWN") @RequestParam(required = false) String status,
            @Parameter(description = "页码，默认1") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数，默认20") @RequestParam(defaultValue = "20") int size) {
        return Result.success(deviceService.listDevices(status, page, size));
    }

    @Operation(summary = "删除设备", description = "逻辑删除设备并清理缓存")
    @DeleteMapping("/{deviceId}")
    public Result<Void> deleteDevice(@PathVariable String deviceId) {
        deviceService.deleteDevice(deviceId);
        return Result.success();
    }

    @Operation(summary = "远程控制设备", description = "通过 MQTT 下发控制指令，同步等待设备响应（超时30秒）")
    @PostMapping("/control")
    public Result<Map<String, Object>> control(@Valid @RequestBody DeviceControlDTO dto) {
        log.info("远程控制请求: deviceId={}, command={}", dto.getDeviceId(), dto.getCommand());
        return Result.success("控制指令已执行", deviceControlService.sendCommand(dto));
    }
}
