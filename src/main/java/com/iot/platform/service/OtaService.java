package com.iot.platform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.iot.platform.model.dto.FirmwareUploadDTO;
import com.iot.platform.model.dto.OtaTaskCreateDTO;
import com.iot.platform.model.entity.Firmware;
import com.iot.platform.model.entity.OtaDeviceProgress;
import com.iot.platform.model.vo.OtaTaskVO;

import java.util.List;
import java.util.Map;

/**
 * OTA 固件升级服务接口
 * <p>
 * 提供固件包管理、升级任务编排、设备进度跟踪与 MQTT 升级指令下发能力。
 *
 * @author iot-platform
 */
public interface OtaService extends IService<Firmware> {

    // ==================== 固件管理 ====================

    /**
     * 上传固件包元信息
     *
     * @param dto 固件上传信息
     * @return 固件实体
     */
    Firmware uploadFirmware(FirmwareUploadDTO dto);

    /**
     * 发布固件（DRAFT -> PUBLISHED）
     *
     * @param firmwareId 固件ID
     */
    void publishFirmware(Long firmwareId);

    /**
     * 分页查询固件列表
     *
     * @param productId 产品ID（可空）
     * @param page      页码
     * @param size      每页条数
     * @return 固件列表
     */
    List<Firmware> listFirmware(Long productId, int page, int size);

    // ==================== 升级任务 ====================

    /**
     * 创建升级任务，批量生成设备进度记录并（IMMEDIATE 策略）下发升级指令
     *
     * @param dto 任务创建信息
     * @return 任务视图
     */
    OtaTaskVO createTask(OtaTaskCreateDTO dto);

    /**
     * 查看任务详情（含设备进度明细）
     *
     * @param taskId 任务ID
     * @return 任务视图
     */
    OtaTaskVO getTaskDetail(Long taskId);

    /**
     * 分页查询任务列表
     *
     * @param status 任务状态过滤（可空）
     * @param page   页码
     * @param size   每页条数
     * @return 任务视图列表
     */
    List<OtaTaskVO> listTasks(String status, int page, int size);

    /**
     * 取消升级任务
     *
     * @param taskId 任务ID
     */
    void cancelTask(Long taskId);

    // ==================== 设备进度 ====================

    /**
     * 查看设备升级进度（跨所有任务）
     *
     * @param deviceId 设备ID
     * @return 进度列表
     */
    List<OtaDeviceProgress> getDeviceProgress(String deviceId);

    /**
     * 处理设备上报的 OTA 升级进度（MQTT 上行）
     * <p>
     * 由 MqttTopicHandler 在收到 {@code iot/device/{deviceId}/ota/progress} 消息时调用。
     *
     * @param deviceId 设备ID
     * @param payload 上报负载（taskId、status、errorMsg、currentVersion 等）
     */
    void handleDeviceProgress(String deviceId, Map<String, Object> payload);
}
