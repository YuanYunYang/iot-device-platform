package com.iot.platform.controller;

import com.iot.platform.common.Result;
import com.iot.platform.model.entity.Product;
import com.iot.platform.model.entity.ThingModel;
import com.iot.platform.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 产品/物模型管理 Controller
 * <p>
 * 提供产品（设备类型）创建、查询及物模型定义管理接口。
 *
 * @author iot-platform
 */
@Slf4j
@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
@Tag(name = "产品物模型管理", description = "产品创建、查询及物模型定义接口")
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "创建产品", description = "创建设备类型，如温湿度传感器、智能开关")
    @PostMapping
    public Result<Product> createProduct(@RequestBody Product product) {
        return Result.success(productService.createProduct(product));
    }

    @Operation(summary = "查询产品列表")
    @GetMapping("/list")
    public Result<List<Product>> listProducts() {
        return Result.success(productService.listProducts());
    }

    @Operation(summary = "查询产品详情", description = "根据产品Key查询产品信息")
    @GetMapping("/{productKey}")
    public Result<Product> getProduct(@PathVariable String productKey) {
        return Result.success(productService.getByProductKey(productKey));
    }

    @Operation(summary = "保存物模型定义", description = "保存/更新产品的属性、事件、服务定义")
    @PostMapping("/{productId}/thing-model")
    public Result<Void> saveThingModels(@PathVariable Long productId,
                                        @RequestBody List<ThingModel> thingModels) {
        productService.saveThingModels(productId, thingModels);
        return Result.success();
    }

    @Operation(summary = "查询物模型定义", description = "查询产品的属性、事件、服务定义列表")
    @GetMapping("/{productId}/thing-model")
    public Result<List<ThingModel>> getThingModels(@PathVariable Long productId) {
        return Result.success(productService.getThingModels(productId));
    }
}
