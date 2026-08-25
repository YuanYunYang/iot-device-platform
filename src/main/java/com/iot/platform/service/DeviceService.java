package com.iot.platform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.iot.platform.model.dto.DeviceRegisterDTO;
import com.iot.platform.model.entity.Device;
import com.iot.platform.model.vo.DeviceVO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 设备服务接口
 * <p>
 * 提供设备注册、查询、状态更新等能力。
 *
 * @author iot-platform
 */
public interface DeviceService extends IService<Device> {

    /**
     * 注册新设备
     *
     * @param dto 设备注册信息
     * @return 注册后的设备视图
     */
    DeviceVO register(DeviceRegisterDTO dto);

    /**
     * 根据设备ID查询设备详情
     *
     * @param deviceId 设备业务ID
     * @return 设备视图
     */
    DeviceVO getDeviceByDeviceId(String deviceId);

    /**
     * 分页查询设备列表
     *
     * @param status 设备状态过滤（可为空）
     * @param page    页码
     * @param size    每页条数
     * @return 设备视图列表
     */
    List<DeviceVO> listDevices(String status, int page, int size);

    /**
     * 更新设备状态
     *
     * @param deviceId 设备ID
     * @param status   目标状态：ONLINE/OFFLINE
     * @param clientIp 设备IP（可空）
     */
    void updateDeviceStatus(String deviceId, String status, String clientIp);

    /**
     * 更新设备心跳时间
     *
     * @param deviceId 设备ID
     */
    void updateHeartbeat(String deviceId);

    /**
     * 删除设备
     *
     * @param deviceId 设备ID
     */
    void deleteDevice(String deviceId);

    /**
     * 将心跳超时的设备置为离线
     *
     * @param timeoutSeconds 超时阈值（秒）
     * @return 置离线的设备数量
     */
    int markTimeoutDevicesOffline(int timeoutSeconds);

    /**
     * 获取设备最后心跳时间
     *
     * @param deviceId 设备ID
     * @return 最后心跳时间，不存在返回 null
     */
    LocalDateTime getLastHeartbeat(String deviceId);
}
