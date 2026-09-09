package com.iot.platform.controller;

import com.iot.platform.common.Result;
import com.iot.platform.model.dto.AlarmResult;
import com.iot.platform.model.dto.DevicePropertyFact;
import com.iot.platform.model.entity.AlarmRule;
import com.iot.platform.service.RuleEngineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 告警规则管理 Controller
 * <p>
 * 提供告警规则的 CRUD、启用/禁用、重载与测试接口。
 *
 * @author iot-platform
 */
@Slf4j
@RestController
@RequestMapping("/v1/rule")
@RequiredArgsConstructor
@Tag(name = "告警规则引擎", description = "Drools 告警规则管理与评估接口")
public class RuleController {

    private final RuleEngineService ruleEngineService;

    @Operation(summary = "创建规则", description = "新增告警规则并自动重载到规则引擎")
    @PostMapping
    public Result<AlarmRule> createRule(@RequestBody AlarmRule rule) {
        log.info("创建告警规则: ruleName={}", rule.getRuleName());
        return Result.success(ruleEngineService.addRule(rule));
    }

    @Operation(summary = "更新规则", description = "更新告警规则并自动重载")
    @PutMapping
    public Result<Void> updateRule(@RequestBody AlarmRule rule) {
        ruleEngineService.updateRule(rule);
        return Result.success();
    }

    @Operation(summary = "删除规则", description = "逻辑删除告警规则并重载")
    @DeleteMapping("/{id}")
    public Result<Void> deleteRule(@PathVariable Long id) {
        ruleEngineService.deleteRule(id);
        return Result.success();
    }

    @Operation(summary = "规则列表", description = "分页查询告警规则，支持按类型过滤")
    @GetMapping("/list")
    public Result<List<AlarmRule>> listRules(
            @Parameter(description = "规则类型：THRESHOLD/COMPOSITE/TIME_WINDOW") @RequestParam(required = false) String ruleType) {
        return Result.success(ruleEngineService.listRules(ruleType));
    }

    @Operation(summary = "启用规则", description = "启用指定告警规则并重载")
    @PostMapping("/{id}/enable")
    public Result<Void> enableRule(@PathVariable Long id) {
        ruleEngineService.enableRule(id, true);
        return Result.success();
    }

    @Operation(summary = "禁用规则", description = "禁用指定告警规则并重载")
    @PostMapping("/{id}/disable")
    public Result<Void> disableRule(@PathVariable Long id) {
        ruleEngineService.enableRule(id, false);
        return Result.success();
    }

    @Operation(summary = "重新加载规则", description = "从数据库重新编译全部启用规则到规则引擎")
    @PostMapping("/reload")
    public Result<Void> reloadRules() {
        ruleEngineService.reloadRules();
        return Result.success();
    }

    @Operation(summary = "测试规则", description = "传入模拟设备属性数据，返回评估结果（不落库告警）")
    @PostMapping("/test")
    public Result<List<AlarmResult>> testRule(@RequestBody DevicePropertyFact fact) {
        return Result.success(ruleEngineService.testRule(fact));
    }
}
