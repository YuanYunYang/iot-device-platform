package com.iot.platform.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iot.platform.model.entity.Tenant;
import org.apache.ibatis.annotations.Mapper;

/**
 * 租户 Mapper
 *
 * @author iot-platform
 */
@Mapper
public interface TenantMapper extends BaseMapper<Tenant> {
}
