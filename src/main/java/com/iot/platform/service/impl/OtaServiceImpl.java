package com.iot.platform.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.iot.platform.common.GlobalExceptionHandler;
import com.iot.platform.common.ResultCode;
import com.iot.platform.model.dto.FirmwareUploadDTO;
import com.iot.platform.model.dto.OtaTaskCreateDTO;
import com.iot.platform.model.entity.Device;
import com.iot.platform.model.entity.Firmware;
import com.iot.platform.model.entity.OtaDeviceProgress;
import com.iot.platform.model.entity.OtaTask;
import com.iot.platform.model.vo.OtaTaskVO;
import com.iot.platform.mqtt.MqttClientManager;
import com.iot.platform.repository.DeviceMapper;
import com.iot.platform.repository.FirmwareMapper;
import com.iot.platform.repository.OtaDeviceProgressMapper;
import com.iot.platform.repository.OtaTaskMapper;
import com.iot.platform.service.OtaService;
import com.iot.platform.websocket.DeviceWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * OTA 固件升级服务实现
 * <p>
 * 实现固件管理、升级任务编排与设备进度跟踪：
 * <ol>
 *     <li>固件上传后默认 DRAFT，发布（PUBLISHED）后可被任务引用</li>
 *     <li>创建任务时按目标范围解析设备，批量生成进度记录并下发升级指令</li>
 *     <li>设备通过 MQTT 上报进度，更新进度状态与任务统计，完成后 WebSocket 通知前端</li>
 * </ol>
 *
 * @author iot-platform
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtaServiceImpl extends ServiceImpl<FirmwareMapper, Firmware> implements OtaService {

    /** 设备 OTA 命令下行 Topic 模板 */
    private static final String OTA_COMMAND_TOPIC_TEMPLATE = "iot/device/%s/ota/command";

    /** 固件状态 */
    private static final String FIRMWARE_DRAFT = "DRAFT";
    private static final String FIRMWARE_PUBLISHED = "PUBLISHED";

    /** 任务状态 */
    private static final String TASK_PENDING = "PENDING";
    private static final String TASK_RUNNING = "RUNNING";
    private static final String TASK_COMPLETED = "COMPLETED";
    private static final String TASK_CANCELLED = "CANCELLED";

    /** 设备升级状态 */
    private static final String PROGRESS_PENDING = "PENDING";
    private static final String PROGRESS_DOWNLOADING = "DOWNLOADING";
    private static final String PROGRESS_INSTALLING = "INSTALLING";
    private static final String PROGRESS_SUCCESS = "SUCCESS";
    private static final String PROGRESS_FAILED = "FAILED";

    private final FirmwareMapper firmwareMapper;
    private final OtaTaskMapper otaTaskMapper;
    private final OtaDeviceProgressMapper otaDeviceProgressMapper;
    private final DeviceMapper deviceMapper;
    private final MqttClientManager mqttClientManager;
    private final DeviceWebSocketHandler webSocketHandler;

    // ==================== 固件管理 ====================

    @Override
    public Firmware uploadFirmware(FirmwareUploadDTO dto) {
        Firmware firmware = new Firmware();
        firmware.setProductId(dto.getProductId());
        firmware.setVersion(dto.getVersion());
        firmware.setFileUrl(dto.getFileUrl());
        firmware.setFileSize(dto.getFileSize());
        firmware.setChecksumMd5(dto.getChecksumMd5());
        firmware.setChecksumSha256(dto.getChecksumSha256());
        firmware.setDescription(dto.getDescription());
        firmware.setStatus(FIRMWARE_DRAFT);
        firmwareMapper.insert(firmware);
        log.info("固件上传成功: id={}, version={}", firmware.getId(), firmware.getVersion());
        return firmware;
    }

    @Override
    public void publishFirmware(Long firmwareId) {
        Firmware firmware = firmwareMapper.selectById(firmwareId);
        if (firmware == null) {
            throw new GlobalExceptionHandler.BusinessException(ResultCode.NOT_FOUND, "固件不存在: " + firmwareId);
        }
        Firmware update = new Firmware();
        update.setId(firmwareId);
        update.setStatus(FIRMWARE_PUBLISHED);
        firmwareMapper.updateById(update);
        log.info("固件已发布: id={}", firmwareId);
    }

    @Override
    public List<Firmware> listFirmware(Long productId, int page, int size) {
        LambdaQueryWrapper<Firmware> wrapper = new LambdaQueryWrapper<>();
        if (productId != null) {
            wrapper.eq(Firmware::getProductId, productId);
        }
        wrapper.orderByDesc(Firmware::getCreateTime);
        return firmwareMapper.selectPage(new Page<>(page, size), wrapper).getRecords();
    }

    // ==================== 升级任务 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OtaTaskVO createTask(OtaTaskCreateDTO dto) {
        // 1. 校验固件并获取目标版本
        Firmware firmware = firmwareMapper.selectById(dto.getFirmwareId());
        if (firmware == null) {
            throw new GlobalExceptionHandler.BusinessException(ResultCode.NOT_FOUND, "固件不存在: " + dto.getFirmwareId());
        }
        if (!FIRMWARE_PUBLISHED.equals(firmware.getStatus())) {
            throw new GlobalExceptionHandler.BusinessException(ResultCode.FAILED, "固件未发布，无法创建升级任务");
        }

        // 2. 解析目标设备
        List<Device> targetDevices = resolveTargetDevices(dto);
        if (targetDevices.isEmpty()) {
            throw new GlobalExceptionHandler.BusinessException(ResultCode.FAILED, "目标设备为空，无法创建升级任务");
        }

        // 3. 创建升级任务
        String targetType = StrUtil.isBlank(dto.getTargetType()) ? "ALL" : dto.getTargetType();
        String strategy = StrUtil.isBlank(dto.getStrategy()) ? "IMMEDIATE" : dto.getStrategy();
        boolean immediate = "IMMEDIATE".equals(strategy);

        OtaTask task = new OtaTask();
        task.setFirmwareId(firmware.getId());
        task.setTaskName(dto.getTaskName());
        task.setTargetType(targetType);
        task.setTargetProductKey(dto.getTargetProductKey());
        if (dto.getTargetDeviceIds() != null && !dto.getTargetDeviceIds().isEmpty()) {
            task.setTargetDeviceIds(String.join(",", dto.getTargetDeviceIds()));
        }
        task.setStrategy(strategy);
        task.setScheduledTime(dto.getScheduledTime());
        task.setStatus(immediate ? TASK_RUNNING : TASK_PENDING);
        task.setTotalCount(targetDevices.size());
        task.setSuccessCount(0);
        task.setFailCount(0);
        otaTaskMapper.insert(task);

        // 4. 批量生成设备进度记录
        List<OtaDeviceProgress> progressList = new ArrayList<>();
        for (Device device : targetDevices) {
            OtaDeviceProgress progress = new OtaDeviceProgress();
            progress.setTaskId(task.getId());
            progress.setDeviceId(device.getDeviceId());
            progress.setStatus(PROGRESS_PENDING);
            progress.setCurrentVersion(device.getFirmwareVersion());
            progress.setTargetVersion(firmware.getVersion());
            otaDeviceProgressMapper.insert(progress);
            progressList.add(progress);
        }

        // 5. 立即下发升级指令
        if (immediate) {
            for (Device device : targetDevices) {
                publishOtaCommand(device.getDeviceId(), task.getId(), firmware);
            }
        }

        log.info("OTA 升级任务创建成功: taskId={}, deviceCount={}", task.getId(), targetDevices.size());

        OtaTaskVO vo = convertToVO(task, firmware.getVersion(), progressList);
        return vo;
    }

    @Override
    public OtaTaskVO getTaskDetail(Long taskId) {
        OtaTask task = otaTaskMapper.selectById(taskId);
        if (task == null) {
            throw new GlobalExceptionHandler.BusinessException(ResultCode.NOT_FOUND, "升级任务不存在: " + taskId);
        }
        Firmware firmware = firmwareMapper.selectById(task.getFirmwareId());
        List<OtaDeviceProgress> progressList = otaDeviceProgressMapper.selectByTaskId(taskId);
        return convertToVO(task, firmware == null ? null : firmware.getVersion(), progressList);
    }

    @Override
    public List<OtaTaskVO> listTasks(String status, int page, int size) {
        LambdaQueryWrapper<OtaTask> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(status)) {
            wrapper.eq(OtaTask::getStatus, status);
        }
        wrapper.orderByDesc(OtaTask::getCreateTime);
        List<OtaTask> tasks = otaTaskMapper.selectPage(new Page<>(page, size), wrapper).getRecords();
        return tasks.stream().map(task -> {
            Firmware firmware = task.getFirmwareId() == null ? null : firmwareMapper.selectById(task.getFirmwareId());
            return convertToVO(task, firmware == null ? null : firmware.getVersion(), null);
        }).collect(Collectors.toList());
    }

    @Override
    public void cancelTask(Long taskId) {
        OtaTask task = otaTaskMapper.selectById(taskId);
        if (task == null) {
            throw new GlobalExceptionHandler.BusinessException(ResultCode.NOT_FOUND, "升级任务不存在: " + taskId);
        }
        if (TASK_COMPLETED.equals(task.getStatus()) || TASK_CANCELLED.equals(task.getStatus())) {
            throw new GlobalExceptionHandler.BusinessException(ResultCode.FAILED, "任务已结束，无法取消");
        }
        OtaTask update = new OtaTask();
        update.setId(taskId);
        update.setStatus(TASK_CANCELLED);
        otaTaskMapper.updateById(update);
        log.info("OTA 升级任务已取消: taskId={}", taskId);
    }

    // ==================== 设备进度 ====================

    @Override
    public List<OtaDeviceProgress> getDeviceProgress(String deviceId) {
        LambdaQueryWrapper<OtaDeviceProgress> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OtaDeviceProgress::getDeviceId, deviceId)
                .orderByDesc(OtaDeviceProgress::getCreateTime);
        return otaDeviceProgressMapper.selectList(wrapper);
    }

    @Override
    public void handleDeviceProgress(String deviceId, Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            log.warn("OTA 进度上报负载为空: deviceId={}", deviceId);
            return;
        }
        Long taskId = toLong(payload.get("taskId"));
        String status = getString(payload, "status");
        if (taskId == null || StrUtil.isBlank(status)) {
            log.warn("OTA 进度上报缺少 taskId/status: deviceId={}, payload={}", deviceId, payload);
            return;
        }

        OtaDeviceProgress progress = otaDeviceProgressMapper.selectByTaskAndDevice(taskId, deviceId);
        if (progress == null) {
            log.warn("未找到设备升级进度记录: taskId={}, deviceId={}", taskId, deviceId);
            return;
        }

        // 更新进度状态
        String errorMsg = getString(payload, "errorMsg");
        String upgradeTime = null;
        if (PROGRESS_SUCCESS.equals(status) || PROGRESS_FAILED.equals(status)) {
            upgradeTime = LocalDateTime.now().toString();
        }
        otaDeviceProgressMapper.updateProgress(progress.getId(), status, errorMsg, upgradeTime);

        // 终态更新任务统计（跨租户，使用忽略租户的方法）
        if (PROGRESS_SUCCESS.equals(status)) {
            otaTaskMapper.incrementSuccessCount(taskId);
        } else if (PROGRESS_FAILED.equals(status)) {
            otaTaskMapper.incrementFailCount(taskId);
        } else {
            // DOWNLOADING / INSTALLING 仅更新进度，不计入统计
            return;
        }

        // 检查任务是否完成
        checkTaskCompletion(taskId);

        log.info("OTA 进度已更新: taskId={}, deviceId={}, status={}", taskId, deviceId, status);
    }

    // ==================== 私有方法 ====================

    /**
     * 按目标范围解析设备列表
     */
    private List<Device> resolveTargetDevices(OtaTaskCreateDTO dto) {
        String targetType = StrUtil.isBlank(dto.getTargetType()) ? "ALL" : dto.getTargetType();
        switch (targetType) {
            case "SELECTED" -> {
                if (dto.getTargetDeviceIds() == null || dto.getTargetDeviceIds().isEmpty()) {
                    return List.of();
                }
                LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
                wrapper.in(Device::getDeviceId, dto.getTargetDeviceIds());
                return deviceMapper.selectList(wrapper);
            }
            case "BY_PRODUCT" -> {
                if (StrUtil.isBlank(dto.getTargetProductKey())) {
                    return List.of();
                }
                LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(Device::getProductKey, dto.getTargetProductKey());
                return deviceMapper.selectList(wrapper);
            }
            default -> {
                // ALL：当前租户下全部设备
                return deviceMapper.selectList(null);
            }
        }
    }

    /**
     * 通过 MQTT 下发 OTA 升级指令到设备
     */
    private void publishOtaCommand(String deviceId, Long taskId, Firmware firmware) {
        try {
            String topic = String.format(OTA_COMMAND_TOPIC_TEMPLATE, deviceId);
            Map<String, Object> command = new HashMap<>();
            command.put("taskId", taskId);
            command.put("firmwareId", firmware.getId());
            command.put("version", firmware.getVersion());
            command.put("fileUrl", firmware.getFileUrl());
            command.put("fileSize", firmware.getFileSize());
            command.put("checksumMd5", firmware.getChecksumMd5());
            command.put("checksumSha256", firmware.getChecksumSha256());
            command.put("timestamp", System.currentTimeMillis());
            mqttClientManager.publish(topic, JSONUtil.toJsonStr(command));
            log.debug("OTA 升级指令已下发: deviceId={}, taskId={}", deviceId, taskId);
        } catch (Exception e) {
            log.error("OTA 升级指令下发失败: deviceId={}, taskId={}, error={}", deviceId, taskId, e.getMessage(), e);
        }
    }

    /**
     * 检查任务是否全部完成，完成则置 COMPLETED 并 WebSocket 推送
     */
    private void checkTaskCompletion(Long taskId) {
        OtaTask task = otaTaskMapper.selectByIdIgnoreTenant(taskId);
        if (task == null) {
            return;
        }
        if (TASK_COMPLETED.equals(task.getStatus()) || TASK_CANCELLED.equals(task.getStatus())) {
            return;
        }
        int total = task.getTotalCount() == null ? 0 : task.getTotalCount();
        int success = task.getSuccessCount() == null ? 0 : task.getSuccessCount();
        int fail = task.getFailCount() == null ? 0 : task.getFailCount();
        if (success + fail >= total && total > 0) {
            otaTaskMapper.updateStatusIgnoreTenant(taskId, TASK_COMPLETED);
            log.info("OTA 升级任务完成: taskId={}, total={}, success={}, fail={}", taskId, total, success, fail);

            // WebSocket 推送任务完成通知
            Map<String, Object> notice = new HashMap<>();
            notice.put("taskId", taskId);
            notice.put("taskName", task.getTaskName());
            notice.put("status", TASK_COMPLETED);
            notice.put("total", total);
            notice.put("success", success);
            notice.put("fail", fail);
            notice.put("time", LocalDateTime.now().toString());
            webSocketHandler.broadcast("ota", notice);
        }
    }

    /**
     * 实体转 VO
     */
    private OtaTaskVO convertToVO(OtaTask task, String targetVersion, List<OtaDeviceProgress> progressList) {
        OtaTaskVO vo = new OtaTaskVO();
        vo.setId(task.getId());
        vo.setTenantId(task.getTenantId());
        vo.setFirmwareId(task.getFirmwareId());
        vo.setTargetVersion(targetVersion);
        vo.setTaskName(task.getTaskName());
        vo.setTargetType(task.getTargetType());
        vo.setTargetProductKey(task.getTargetProductKey());
        vo.setTargetDeviceIds(task.getTargetDeviceIds());
        vo.setStrategy(task.getStrategy());
        vo.setScheduledTime(task.getScheduledTime());
        vo.setStatus(task.getStatus());
        vo.setTotalCount(task.getTotalCount());
        vo.setSuccessCount(task.getSuccessCount());
        vo.setFailCount(task.getFailCount());
        vo.setCreateTime(task.getCreateTime());
        vo.setUpdateTime(task.getUpdateTime());

        // 计算待升级数
        int pending = 0;
        if (progressList != null) {
            for (OtaDeviceProgress p : progressList) {
                if (PROGRESS_PENDING.equals(p.getStatus())
                        || PROGRESS_DOWNLOADING.equals(p.getStatus())
                        || PROGRESS_INSTALLING.equals(p.getStatus())) {
                    pending++;
                }
            }
            vo.setProgressList(progressList);
        }
        vo.setPendingCount(pending);
        return vo;
    }

    /**
     * 安全转为 Long
     */
    private Long toLong(Object value) {
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
     * 从 Map 中安全获取字符串
     */
    private String getString(Map<String, Object> map, String key) {
        if (map == null || !map.containsKey(key)) {
            return null;
        }
        Object value = map.get(key);
        return value == null ? null : value.toString();
    }
}
