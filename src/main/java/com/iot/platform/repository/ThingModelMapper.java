package com.iot.platform.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iot.platform.model.entity.ThingModel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 物模型 Mapper 接口
 * <p>
 * 提供物模型定义（产品属性/事件/服务）的持久化与查询操作。
 * 告警引擎依据物模型的阈值配置进行告警判定。
 *
 * @author iot-platform
 */
@Mapper
public interface ThingModelMapper extends BaseMapper<ThingModel> {

    /**
     * 根据产品ID查询所有物模型定义
     *
     * @param productId 产品ID
     * @return 物模型列表
     */
    List<ThingModel> selectByProductId(@Param("productId") Long productId);

    /**
     * 根据产品ID删除物模型定义（用于产品物模型重建）
     *
     * @param productId 产品ID
     * @return 受影响行数
     */
    int deleteByProductId(@Param("productId") Long productId);
}
