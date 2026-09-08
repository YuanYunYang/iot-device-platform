package com.iot.platform.controller;

import com.iot.platform.common.Result;
import com.iot.platform.model.dto.CarbonEmissionQueryDTO;
import com.iot.platform.model.entity.CarbonEmission;
import com.iot.platform.model.entity.EmissionFactor;
import com.iot.platform.model.vo.CarbonEmissionVO;
import com.iot.platform.service.CarbonEmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 碳排放核算 Controller
 * <p>
 * 提供碳排放量计算、排放记录查询、排放汇总及排放因子管理等接口。
 *
 * @author iot-platform
 */
@Slf4j
@RestController
@RequestMapping("/iot/carbon")
@RequiredArgsConstructor
@Tag(name = "碳排放核算", description = "碳排放计算、查询及排放因子管理相关接口")
public class CarbonEmissionController {

    private final CarbonEmissionService carbonEmissionService;

    @Operation(summary = "计算碳排放", description = "计算指定日期范围内的碳排放量并生成排放记录")
    @GetMapping("/calculate")
    public Result<CarbonEmissionVO> calculateEmission(
            @Parameter(description = "起始日期（yyyy-MM-dd）", required = true) @RequestParam String startDate,
            @Parameter(description = "结束日期（yyyy-MM-dd）", required = true) @RequestParam String endDate) {
        log.info("计算碳排放请求: {} ~ {}", startDate, endDate);
        return Result.success(carbonEmissionService.calculateEmission(startDate, endDate));
    }

    @Operation(summary = "查询排放记录列表", description = "按排放范围、排放源类型及日期范围查询碳排放明细")
    @GetMapping("/list")
    public Result<List<CarbonEmission>> listEmissionRecords(
            @Parameter(description = "排放范围：SCOPE_1/SCOPE_2") @RequestParam(required = false) String scope,
            @Parameter(description = "排放源类型：ELECTRICITY/NATURAL_GAS/DIESEL/COAL") @RequestParam(required = false) String sourceType,
            @Parameter(description = "起始日期（yyyy-MM-dd）") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期（yyyy-MM-dd）") @RequestParam(required = false) String endDate) {
        CarbonEmissionQueryDTO query = new CarbonEmissionQueryDTO();
        query.setScope(scope);
        query.setSourceType(sourceType);
        query.setStartDate(startDate);
        query.setEndDate(endDate);
        return Result.success(carbonEmissionService.getEmissionRecords(query));
    }

    @Operation(summary = "排放汇总", description = "获取指定日期范围内的碳排放汇总数据")
    @GetMapping("/summary")
    public Result<CarbonEmissionVO> getEmissionSummary(
            @Parameter(description = "起始日期（yyyy-MM-dd）", required = true) @RequestParam String startDate,
            @Parameter(description = "结束日期（yyyy-MM-dd）", required = true) @RequestParam String endDate) {
        return Result.success(carbonEmissionService.calculateEmission(startDate, endDate));
    }

    @Operation(summary = "新增排放因子", description = "添加自定义碳排放因子")
    @PostMapping("/factor")
    public Result<EmissionFactor> addEmissionFactor(@RequestBody EmissionFactor factor) {
        log.info("新增排放因子: sourceType={}, factorValue={}", factor.getSourceType(), factor.getFactorValue());
        return Result.success(carbonEmissionService.addEmissionFactor(factor));
    }

    @Operation(summary = "查询排放因子列表", description = "查询当前租户的所有碳排放因子")
    @GetMapping("/factor/list")
    public Result<List<EmissionFactor>> listEmissionFactors() {
        return Result.success(carbonEmissionService.listEmissionFactors());
    }

    @Operation(summary = "初始化默认排放因子", description = "初始化中国电网及常见燃料的默认碳排放因子")
    @PostMapping("/factor/init")
    public Result<Void> initDefaultFactors() {
        carbonEmissionService.initDefaultFactors();
        return Result.success();
    }
}
