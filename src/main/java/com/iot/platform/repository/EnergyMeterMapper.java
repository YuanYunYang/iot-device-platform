package com.iot.platform.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iot.platform.model.entity.EnergyMeter;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 能耗仪表 Mapper 接口
 * <p>
 * 继承 MyBatis-Plus BaseMapper，提供标准 CRUD 能力；
 * 自定义方法见对应 XML 文件 EnergyMeterMapper.xml。
 *
 * @author iot-platform
 */
@Mapper
public interface EnergyMeterMapper extends BaseMapper<EnergyMeter> {

    /**
     * 根据仪表编号查询仪表
     *
     * @param meterCode 仪表编号
     * @return 仪表实体
     */
    EnergyMeter selectByMeterCode(@Param("meterCode") String meterCode);
}
