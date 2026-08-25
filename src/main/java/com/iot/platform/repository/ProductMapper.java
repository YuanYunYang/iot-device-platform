package com.iot.platform.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iot.platform.model.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 产品 Mapper 接口
 * <p>
 * 提供产品（设备类型）的持久化操作。
 *
 * @author iot-platform
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    /**
     * 根据产品Key查询产品
     *
     * @param productKey 产品标识
     * @return 产品实体
     */
    Product selectByProductKey(@Param("productKey") String productKey);
}
