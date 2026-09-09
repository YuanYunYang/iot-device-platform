package com.iot.platform.controller;

import com.iot.platform.service.DataExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 数据导出 Controller
 * <p>
 * 提供能耗报告与碳排放报告的 Excel / PDF 导出下载接口。
 *
 * @author iot-platform
 */
@Slf4j
@RestController
@RequestMapping("/v1/export")
@RequiredArgsConstructor
@Tag(name = "数据导出", description = "能耗报告、碳排放报告导出接口")
public class DataExportController {

    private final DataExportService dataExportService;

    @Operation(summary = "导出能耗报告 Excel", description = "按能源表与日期范围导出 Excel 格式能耗报告")
    @GetMapping("/energy/excel")
    public ResponseEntity<byte[]> exportEnergyExcel(
            @Parameter(description = "能源表ID", required = true) @RequestParam Long meterId,
            @Parameter(description = "开始日期（yyyy-MM-dd）", required = true) @RequestParam String startDate,
            @Parameter(description = "结束日期（yyyy-MM-dd）", required = true) @RequestParam String endDate) {
        log.info("导出能耗 Excel: meterId={}, startDate={}, endDate={}", meterId, startDate, endDate);
        byte[] data = dataExportService.exportEnergyReportExcel(meterId, startDate, endDate);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment",
                "energy_report_" + meterId + "_" + startDate + "_" + endDate + ".xlsx");

        return ResponseEntity.ok().headers(headers).body(data);
    }

    @Operation(summary = "导出能耗报告 PDF", description = "按能源表与日期范围导出 PDF 格式能耗报告")
    @GetMapping("/energy/pdf")
    public ResponseEntity<byte[]> exportEnergyPdf(
            @Parameter(description = "能源表ID", required = true) @RequestParam Long meterId,
            @Parameter(description = "开始日期（yyyy-MM-dd）", required = true) @RequestParam String startDate,
            @Parameter(description = "结束日期（yyyy-MM-dd）", required = true) @RequestParam String endDate) {
        log.info("导出能耗 PDF: meterId={}, startDate={}, endDate={}", meterId, startDate, endDate);
        byte[] data = dataExportService.exportEnergyReportPdf(meterId, startDate, endDate);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment",
                "energy_report_" + meterId + "_" + startDate + "_" + endDate + ".pdf");

        return ResponseEntity.ok().headers(headers).body(data);
    }

    @Operation(summary = "导出碳排放报告 Excel", description = "按日期范围导出 Excel 格式碳排放报告")
    @GetMapping("/carbon/excel")
    public ResponseEntity<byte[]> exportCarbonExcel(
            @Parameter(description = "开始日期（yyyy-MM-dd）", required = true) @RequestParam String startDate,
            @Parameter(description = "结束日期（yyyy-MM-dd）", required = true) @RequestParam String endDate) {
        log.info("导出碳排放 Excel: startDate={}, endDate={}", startDate, endDate);
        byte[] data = dataExportService.exportCarbonReportExcel(startDate, endDate);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment",
                "carbon_report_" + startDate + "_" + endDate + ".xlsx");

        return ResponseEntity.ok().headers(headers).body(data);
    }
}
