package com.iot.platform.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iot.platform.model.entity.EnergyReading;
import org.apache.ibatis.annotations.Mapper;

/**
 * 能源读数 Mapper 接口
 * <p>
 * 继承 MyBatis-Plus BaseMapper，提供能源读数数据的标准 CRUD。
 * 读数主存储于 InfluxDB 时序库，MySQL 表用于数据导出与汇总统计。
 *
 * @author iot-platform
 */
@Mapper
public interface EnergyReadingMapper extends BaseMapper<EnergyReading> {
}
