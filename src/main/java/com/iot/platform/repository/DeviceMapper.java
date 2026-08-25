package com.iot.platform.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iot.platform.model.entity.Device;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 设备 Mapper 接口
 * <p>
 * 继承 MyBatis-Plus BaseMapper，提供标准 CRUD；
 * 自定义方法见对应 XML 文件 DeviceMapper.xml。
 *
 * @author iot-platform
 */
@Mapper
public interface DeviceMapper extends BaseMapper<Device> {

    /**
     * 根据设备ID（业务ID）查询设备
     *
     * @param deviceId 设备业务ID
     * @return 设备实体
     */
    Device selectByDeviceId(@Param("deviceId") String deviceId);

    /**
     * 将心跳超时的在线设备批量置为离线
     *
     * @param timeoutSeconds 心跳超时阈值（秒）
     * @return 受影响行数
     */
    int updateOfflineByTimeout(@Param("timeoutSeconds") int timeoutSeconds);
}
