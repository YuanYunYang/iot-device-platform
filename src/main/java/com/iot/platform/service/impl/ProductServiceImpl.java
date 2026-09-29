package com.iot.platform.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.iot.platform.common.GlobalExceptionHandler;
import com.iot.platform.common.ResultCode;
import com.iot.platform.model.entity.Product;
import com.iot.platform.model.entity.ThingModel;
import com.iot.platform.repository.ProductMapper;
import com.iot.platform.repository.ThingModelMapper;
import com.iot.platform.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 产品服务实现
 * <p>
 * 管理产品（设备类型）及其物模型定义，物模型用于数据校验与告警判定。
 *
 * @author iot-platform
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    private final ProductMapper productMapper;
    private final ThingModelMapper thingModelMapper;

    @Override
    public Product createProduct(Product product) {
        // 校验产品Key唯一性
        Product exist = productMapper.selectByProductKey(product.getProductKey());
        if (exist != null) {
            throw new GlobalExceptionHandler.BusinessException(
                    ResultCode.FAILED, "产品Key已存在: " + product.getProductKey());
        }
        if (StrUtil.isBlank(product.getNetType())) {
            product.setNetType("MQTT");
        }
        if (StrUtil.isBlank(product.getDataFormat())) {
            product.setDataFormat("JSON");
        }
        if (StrUtil.isBlank(product.getNodeType())) {
            product.setNodeType("DEVICE");
        }
        productMapper.insert(product);
        log.info("产品创建成功: productKey={}", product.getProductKey());
        return product;
    }

    @Override
    public Product getByProductKey(String productKey) {
        return productMapper.selectByProductKey(productKey);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveThingModels(Long productId, List<ThingModel> thingModels) {
        // 先删除旧物模型定义
        thingModelMapper.deleteByProductId(productId);
        // 批量插入新定义
        if (thingModels != null && !thingModels.isEmpty()) {
            for (ThingModel model : thingModels) {
                model.setProductId(productId);
                thingModelMapper.insert(model);
            }
        }
        log.info("物模型已更新: productId={}, 数量={}", productId,
                thingModels == null ? 0 : thingModels.size());
    }

    @Override
    public List<ThingModel> getThingModels(Long productId) {
        return thingModelMapper.selectByProductId(productId);
    }

    @Override
    public List<Product> listProducts() {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Product::getCreateTime);
        return productMapper.selectList(wrapper);
    }

    @Override
    public Product updateProduct(Product product) {
        if (product.getId() == null) {
            throw new GlobalExceptionHandler.BusinessException(ResultCode.FAILED, "产品ID不能为空");
        }
        Product exist = productMapper.selectById(product.getId());
        if (exist == null) {
            throw new GlobalExceptionHandler.BusinessException(ResultCode.FAILED, "产品不存在");
        }
        if (StrUtil.isNotBlank(product.getProductKey())
                && !product.getProductKey().equals(exist.getProductKey())) {
            Product keyConflict = productMapper.selectByProductKey(product.getProductKey());
            if (keyConflict != null && !keyConflict.getId().equals(product.getId())) {
                throw new GlobalExceptionHandler.BusinessException(
                        ResultCode.FAILED, "产品Key已存在: " + product.getProductKey());
            }
        }
        productMapper.updateById(product);
        log.info("产品已更新: id={}, productKey={}", product.getId(), product.getProductKey());
        return productMapper.selectById(product.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(Long id) {
        Product exist = productMapper.selectById(id);
        if (exist == null) {
            throw new GlobalExceptionHandler.BusinessException(ResultCode.FAILED, "产品不存在");
        }
        thingModelMapper.deleteByProductId(id);
        productMapper.deleteById(id);
        log.info("产品已删除: id={}, productKey={}", id, exist.getProductKey());
    }
}
