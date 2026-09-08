package com.iot.platform.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iot.platform.model.entity.LoadDispatchStrategy;
import org.apache.ibatis.annotations.Mapper;

/**
 * 负荷调度策略 Mapper 接口
 * <p>
 * 提供负荷调度策略的持久化与查询操作。
 *
 * @author iot-platform
 */
@Mapper
public interface LoadDispatchStrategyMapper extends BaseMapper<LoadDispatchStrategy> {
}
