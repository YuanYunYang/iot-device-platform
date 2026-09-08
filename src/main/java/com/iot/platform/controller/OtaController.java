package com.iot.platform.controller;

import com.iot.platform.common.Result;
import com.iot.platform.model.dto.FirmwareUploadDTO;
import com.iot.platform.model.dto.OtaTaskCreateDTO;
import com.iot.platform.model.entity.Firmware;
import com.iot.platform.model.entity.OtaDeviceProgress;
import com.iot.platform.model.vo.OtaTaskVO;
import com.iot.platform.service.OtaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * OTA 固件升级管理 Controller
 * <p>
 * 提供固件包管理、升级任务编排与设备进度查询接口。
 *
 * @author iot-platform
 */
@Slf4j
@RestController
@RequestMapping("/ota")
@RequiredArgsConstructor
@Tag(name = "OTA固件升级", description = "固件管理、升级任务与设备进度接口")
public class OtaController {

    private final OtaService otaService;

    // ==================== 固件管理 ====================

    @Operation(summary = "上传固件", description = "提交固件包元信息，默认 DRAFT 状态")
    @PostMapping("/firmware")
    public Result<Firmware> uploadFirmware(@Valid @RequestBody FirmwareUploadDTO dto) {
        log.info("上传固件请求: version={}", dto.getVersion());
        return Result.success(otaService.uploadFirmware(dto));
    }

    @Operation(summary = "发布固件", description = "将固件状态由 DRAFT 切换为 PUBLISHED")
    @PostMapping("/firmware/{firmwareId}/publish")
    public Result<Void> publishFirmware(@PathVariable Long firmwareId) {
        otaService.publishFirmware(firmwareId);
        return Result.success();
    }

    @Operation(summary = "固件列表", description = "分页查询固件列表，支持按产品过滤")
    @GetMapping("/firmware/list")
    public Result<List<Firmware>> listFirmware(
            @Parameter(description = "产品ID") @RequestParam(required = false) Long productId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int size) {
        return Result.success(otaService.listFirmware(productId, page, size));
    }

    // ==================== 升级任务 ====================

    @Operation(summary = "创建升级任务", description = "按目标范围创建升级任务并下发升级指令")
    @PostMapping("/task")
    public Result<OtaTaskVO> createTask(@Valid @RequestBody OtaTaskCreateDTO dto) {
        log.info("创建升级任务请求: firmwareId={}, taskName={}", dto.getFirmwareId(), dto.getTaskName());
        return Result.success(otaService.createTask(dto));
    }

    @Operation(summary = "任务详情", description = "查看升级任务详情，含设备进度明细")
    @GetMapping("/task/{taskId}")
    public Result<OtaTaskVO> getTaskDetail(@PathVariable Long taskId) {
        return Result.success(otaService.getTaskDetail(taskId));
    }

    @Operation(summary = "任务列表", description = "分页查询升级任务，支持按状态过滤")
    @GetMapping("/task/list")
    public Result<List<OtaTaskVO>> listTasks(
            @Parameter(description = "任务状态：PENDING/RUNNING/COMPLETED/CANCELLED") @RequestParam(required = false) String status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int size) {
        return Result.success(otaService.listTasks(status, page, size));
    }

    @Operation(summary = "取消任务", description = "取消尚未完成的升级任务")
    @PostMapping("/task/{taskId}/cancel")
    public Result<Void> cancelTask(@PathVariable Long taskId) {
        otaService.cancelTask(taskId);
        return Result.success();
    }

    // ==================== 设备进度 ====================

    @Operation(summary = "设备升级进度", description = "查询指定设备的升级进度记录")
    @GetMapping("/device/{deviceId}/progress")
    public Result<List<OtaDeviceProgress>> getDeviceProgress(@PathVariable String deviceId) {
        return Result.success(otaService.getDeviceProgress(deviceId));
    }
}
