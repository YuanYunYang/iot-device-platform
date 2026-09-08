package com.iot.platform.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iot.platform.model.entity.Firmware;
import org.apache.ibatis.annotations.Mapper;

/**
 * 固件包 Mapper 接口
 * <p>
 * 继承 MyBatis-Plus BaseMapper，提供固件包标准 CRUD；
 * 多租户过滤由 TenantLineInnerInterceptor 自动注入。
 *
 * @author iot-platform
 */
@Mapper
public interface FirmwareMapper extends BaseMapper<Firmware> {
}
