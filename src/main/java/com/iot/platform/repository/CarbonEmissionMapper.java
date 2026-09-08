package com.iot.platform.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iot.platform.model.entity.CarbonEmission;
import org.apache.ibatis.annotations.Mapper;

/**
 * 碳排放记录 Mapper 接口
 * <p>
 * 继承 MyBatis-Plus BaseMapper，提供标准 CRUD 能力。
 *
 * @author iot-platform
 */
@Mapper
public interface CarbonEmissionMapper extends BaseMapper<CarbonEmission> {
}
