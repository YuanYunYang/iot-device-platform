package com.iot.platform.controller;

import com.iot.platform.common.Result;
import com.iot.platform.model.dto.EnergyReportQueryDTO;
import com.iot.platform.model.entity.EnergyReport;
import com.iot.platform.model.vo.EnergyReportVO;
import com.iot.platform.service.EnergyAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 能耗分析报表 Controller
 * <p>
 * 提供能耗日报/月报生成、报表查询、能耗趋势及尖峰平谷分析等接口。
 *
 * @author iot-platform
 */
@Slf4j
@RestController
@RequestMapping("/iot/energy/report")
@RequiredArgsConstructor
@Tag(name = "能耗分析报表", description = "能耗报表生成、查询及尖峰平谷分析相关接口")
public class EnergyReportController {

    private final EnergyAnalysisService energyAnalysisService;

    @Operation(summary = "生成日报表", description = "根据仪表逐小时能耗读数生成指定日期的日报表")
    @PostMapping("/daily/{meterId}")
    public Result<EnergyReport> generateDailyReport(
            @Parameter(description = "仪表ID") @PathVariable Long meterId,
            @Parameter(description = "日期（yyyy-MM-dd）", required = true) @RequestParam String date) {
        log.info("生成日报表请求: meterId={}, date={}", meterId, date);
        return Result.success(energyAnalysisService.generateDailyReport(meterId, date));
    }

    @Operation(summary = "生成月报表", description = "汇总仪表整月日报数据生成月度能耗汇总报表")
    @PostMapping("/monthly/{meterId}")
    public Result<EnergyReport> generateMonthlyReport(
            @Parameter(description = "仪表ID") @PathVariable Long meterId,
            @Parameter(description = "月份（yyyy-MM）", required = true) @RequestParam String month) {
        log.info("生成月报表请求: meterId={}, month={}", meterId, month);
        return Result.success(energyAnalysisService.generateMonthlyReport(meterId, month));
    }

    @Operation(summary = "查询报表列表", description = "按仪表、报表类型及日期范围查询能耗报表")
    @GetMapping("/list")
    public Result<List<EnergyReport>> listReports(
            @Parameter(description = "仪表ID") @RequestParam(required = false) Long meterId,
            @Parameter(description = "报表类型：DAILY/WEEKLY/MONTHLY") @RequestParam(required = false) String reportType,
            @Parameter(description = "起始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate) {
        EnergyReportQueryDTO query = new EnergyReportQueryDTO();
        query.setMeterId(meterId);
        query.setReportType(reportType);
        query.setStartDate(startDate);
        query.setEndDate(endDate);
        return Result.success(energyAnalysisService.getReports(query));
    }

    @Operation(summary = "报表详情", description = "查询报表详情，含尖峰平谷分项电费及占比")
    @GetMapping("/{reportId}")
    public Result<EnergyReportVO> getReportDetail(
            @Parameter(description = "报表ID") @PathVariable Long reportId) {
        return Result.success(energyAnalysisService.getReportDetail(reportId));
    }

    @Operation(summary = "能耗趋势", description = "查询仪表在指定日期范围内的能耗趋势数据")
    @GetMapping("/trend")
    public Result<Map<String, Object>> getEnergyTrend(
            @Parameter(description = "仪表ID", required = true) @RequestParam Long meterId,
            @Parameter(description = "起始日期", required = true) @RequestParam String startDate,
            @Parameter(description = "结束日期", required = true) @RequestParam String endDate,
            @Parameter(description = "粒度：DAILY/WEEKLY/MONTHLY") @RequestParam(defaultValue = "DAILY") String granularity) {
        return Result.success(energyAnalysisService.getEnergyTrend(meterId, startDate, endDate, granularity));
    }

    @Operation(summary = "尖峰平谷分析", description = "查询仪表在指定日期范围内的尖峰平谷能耗与电费分布")
    @GetMapping("/peak-valley")
    public Result<Map<String, Object>> getPeakValleyAnalysis(
            @Parameter(description = "仪表ID", required = true) @RequestParam Long meterId,
            @Parameter(description = "起始日期", required = true) @RequestParam String startDate,
            @Parameter(description = "结束日期", required = true) @RequestParam String endDate) {
        return Result.success(energyAnalysisService.getPeakValleyAnalysis(meterId, startDate, endDate));
    }
}
