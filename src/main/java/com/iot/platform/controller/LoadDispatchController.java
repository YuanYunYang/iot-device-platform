package com.iot.platform.controller;

import com.iot.platform.common.Result;
import com.iot.platform.model.dto.LoadDispatchStrategyCreateDTO;
import com.iot.platform.model.entity.LoadDispatchStrategy;
import com.iot.platform.model.vo.LoadDispatchVO;
import com.iot.platform.service.LoadDispatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 负荷调度策略 Controller
 * <p>
 * 提供负荷调度策略的创建、手动执行、自动调度触发、查询与启停管理接口。
 *
 * @author iot-platform
 */
@Slf4j
@RestController
@RequestMapping("/v1/dispatch")
@RequiredArgsConstructor
@Tag(name = "负荷调度策略", description = "负荷调度策略管理与执行接口")
public class LoadDispatchController {

    private final LoadDispatchService loadDispatchService;

    @Operation(summary = "创建调度策略", description = "新增一条负荷调度策略")
    @PostMapping("/strategy")
    public Result<LoadDispatchStrategy> createStrategy(@Valid @RequestBody LoadDispatchStrategyCreateDTO dto) {
        log.info("创建调度策略请求: name={}, type={}", dto.getStrategyName(), dto.getStrategyType());
        return Result.success(loadDispatchService.createStrategy(dto));
    }

    @Operation(summary = "手动执行策略", description = "手动触发指定策略的调度下发")
    @PostMapping("/strategy/{id}/execute")
    public Result<Void> executeStrategy(@Parameter(description = "策略ID") @PathVariable Long id) {
        log.info("手动执行调度策略: id={}", id);
        loadDispatchService.executeStrategy(id);
        return Result.success();
    }

    @Operation(summary = "触发自动调度", description = "立即执行一次自动调度检查（遍历已启用策略并校验触发条件）")
    @PostMapping("/auto")
    public Result<Void> triggerAutoDispatch() {
        log.info("触发自动负荷调度检查");
        loadDispatchService.executeAutoDispatch();
        return Result.success();
    }

    @Operation(summary = "策略列表", description = "分页查询调度策略，支持按策略类型过滤")
    @GetMapping("/strategy/list")
    public Result<List<LoadDispatchVO>> listStrategies(
            @Parameter(description = "策略类型：PEAK_SHAVING/VALLEY_FILLING/DEMAND_RESPONSE")
            @RequestParam(required = false) String strategyType,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int size) {
        return Result.success(loadDispatchService.listStrategies(strategyType, page, size));
    }

    @Operation(summary = "启停策略", description = "启用或禁用指定策略")
    @PutMapping("/strategy/{id}/enable")
    public Result<Void> enableStrategy(
            @Parameter(description = "策略ID") @PathVariable Long id,
            @Parameter(description = "是否启用") @RequestParam boolean enabled) {
        log.info("更新策略启停状态: id={}, enabled={}", id, enabled);
        loadDispatchService.enableStrategy(id, enabled);
        return Result.success();
    }

    @Operation(summary = "删除策略", description = "删除指定调度策略")
    @DeleteMapping("/strategy/{id}")
    public Result<Void> deleteStrategy(@Parameter(description = "策略ID") @PathVariable Long id) {
        log.info("删除调度策略: id={}", id);
        loadDispatchService.deleteStrategy(id);
        return Result.success();
    }

    @Operation(summary = "执行历史", description = "分页查询调度策略执行历史")
    @GetMapping("/history")
    public Result<List<LoadDispatchVO>> getExecutionHistory(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int size) {
        return Result.success(loadDispatchService.getExecutionHistory(page, size));
    }
}
