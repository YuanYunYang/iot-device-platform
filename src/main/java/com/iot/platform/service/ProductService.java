package com.iot.platform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.iot.platform.model.entity.Product;
import com.iot.platform.model.entity.ThingModel;

import java.util.List;

/**
 * 产品服务接口
 * <p>
 * 提供产品（设备类型）与物模型的管理能力。
 *
 * @author iot-platform
 */
public interface ProductService extends IService<Product> {

    /**
     * 创建产品
     *
     * @param product 产品信息
     * @return 创建后的产品
     */
    Product createProduct(Product product);

    /**
     * 根据产品Key查询产品
     *
     * @param productKey 产品标识
     * @return 产品实体
     */
    Product getByProductKey(String productKey);

    /**
     * 保存/更新产品物模型（先删除旧定义，再批量插入）
     *
     * @param productId    产品ID
     * @param thingModels 物模型列表
     */
    void saveThingModels(Long productId, List<ThingModel> thingModels);

    /**
     * 查询产品的物模型定义
     *
     * @param productId 产品ID
     * @return 物模型列表
     */
    List<ThingModel> getThingModels(Long productId);

    /**
     * 查询所有产品列表
     *
     * @return 产品列表
     */
    List<Product> listProducts();
}
