package com.iot.platform.controller;

import com.iot.platform.common.Result;
import com.iot.platform.service.CarbonReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 碳排报告 Controller
 * <p>
 * 提供碳排放月度/年度报告生成与 PDF 导出接口。
 *
 * @author iot-platform
 */
@Slf4j
@RestController
@RequestMapping("/v1/carbon/report")
@RequiredArgsConstructor
@Tag(name = "碳排报告", description = "碳排放报告生成与导出接口")
public class CarbonReportController {

    private final CarbonReportService carbonReportService;

    @Operation(summary = "生成月度报告", description = "生成指定年月的碳排放月度报告")
    @GetMapping("/monthly")
    public Result<Map<String, Object>> generateMonthlyReport(
            @Parameter(description = "年份，如 2026", required = true) @RequestParam String year,
            @Parameter(description = "月份，如 08", required = true) @RequestParam String month) {
        log.info("生成月度碳排放报告: year={}, month={}", year, month);
        return Result.success(carbonReportService.generateCarbonReport(year, month));
    }

    @Operation(summary = "生成年度报告", description = "生成指定年份的碳排放年度报告")
    @GetMapping("/annual")
    public Result<Map<String, Object>> generateAnnualReport(
            @Parameter(description = "年份，如 2026", required = true) @RequestParam String year) {
        log.info("生成年度碳排放报告: year={}", year);
        return Result.success(carbonReportService.generateAnnualReport(year));
    }

    @Operation(summary = "导出报告PDF", description = "导出指定年月的碳排放月度报告为 PDF")
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportReport(
            @Parameter(description = "年份，如 2026", required = true) @RequestParam String year,
            @Parameter(description = "月份，如 08", required = true) @RequestParam String month) {
        log.info("导出碳排放报告 PDF: year={}, month={}", year, month);
        byte[] pdf = carbonReportService.exportCarbonReportPdf(year, month);
        String fileName = "carbon-report-" + year + month + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
