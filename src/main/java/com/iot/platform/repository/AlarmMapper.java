package com.iot.platform.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iot.platform.model.entity.Alarm;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 告警 Mapper 接口
 * <p>
 * 提供告警记录的持久化与查询操作。
 *
 * @author iot-platform
 */
@Mapper
public interface AlarmMapper extends BaseMapper<Alarm> {

    /**
     * 按设备ID分页查询告警记录
     *
     * @param deviceId 设备ID
     * @param level    告警等级（可为空，表示全部）
     * @param offset   偏移量
     * @param limit    每页条数
     * @return 告警列表
     */
    List<Alarm> selectAlarmsByDeviceId(@Param("deviceId") String deviceId,
                                       @Param("level") String level,
                                       @Param("offset") int offset,
                                       @Param("limit") int limit);
}
