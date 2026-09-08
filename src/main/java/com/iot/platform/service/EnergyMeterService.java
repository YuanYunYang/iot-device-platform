package com.iot.platform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.iot.platform.model.dto.EnergyMeterCreateDTO;
import com.iot.platform.model.entity.EnergyMeter;

import java.util.List;
import java.util.Map;

/**
 * 能源表服务接口
 * <p>
 * 提供能源表的创建、查询、删除及 MQTT 能耗数据处理能力。
 *
 * @author iot-platform
 */
public interface EnergyMeterService extends IService<EnergyMeter> {

    /**
     * 创建能源表
     *
     * @param dto 能源表创建信息
     * @return 创建后的能源表实体
     */
    EnergyMeter createMeter(EnergyMeterCreateDTO dto);

    /**
     * 根据主键ID查询能源表
     *
     * @param id 主键ID
     * @return 能源表实体
     */
    EnergyMeter getMeterById(Long id);

    /**
     * 分页查询能源表列表
     *
     * @param meterType 能源表类型过滤（可为空）
     * @param page      页码
     * @param size      每页条数
     * @return 能源表列表
     */
    List<EnergyMeter> listMeters(String meterType, int page, int size);

    /**
     * 删除能源表
     *
     * @param id 主键ID
     */
    void deleteMeter(Long id);

    /**
     * 处理 MQTT 上报的能耗数据
     *
     * @param deviceId 设备ID
     * @param payload  上报数据载荷
     */
    void handleEnergyData(String deviceId, Map<String, Object> payload);
}
