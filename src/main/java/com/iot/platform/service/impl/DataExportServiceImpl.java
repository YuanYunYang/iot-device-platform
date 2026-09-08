package com.iot.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iot.platform.common.GlobalExceptionHandler;
import com.iot.platform.common.ResultCode;
import com.iot.platform.model.entity.EnergyMeter;
import com.iot.platform.model.entity.EnergyReading;
import com.iot.platform.repository.EnergyMeterMapper;
import com.iot.platform.repository.EnergyReadingMapper;
import com.iot.platform.service.DataExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 数据导出服务实现
 * <p>
 * 基于 Apache POI 生成 Excel 报表，基于 HTML-to-bytes 生成 PDF 报表。
 * 能耗数据来源于 energy_reading 表，碳排放基于能耗与排放因子计算。
 *
 * @author iot-platform
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataExportServiceImpl implements DataExportService {

    private final EnergyMeterMapper energyMeterMapper;
    private final EnergyReadingMapper energyReadingMapper;

    /** 电力碳排放默认因子（kgCO2/kWh，中国区域电网平均值） */
    private static final double DEFAULT_EMISSION_FACTOR = 0.5810;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public byte[] exportEnergyReportExcel(Long meterId, String startDate, String endDate) {
        EnergyMeter meter = energyMeterMapper.selectById(meterId);
        if (meter == null) {
            throw new GlobalExceptionHandler.BusinessException(
                    ResultCode.NOT_FOUND, "能源表不存在: " + meterId);
        }

        long[] timeRange = parseDateRange(startDate, endDate);
        List<EnergyReading> readings = queryReadings(meter.getMeterCode(), timeRange[0], timeRange[1]);

        String[] headers = {"时间", "电表编号", "电压(V)", "电流(A)", "有功功率(kW)", "功率因数", "用电量(kWh)", "峰谷标志"};

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("能耗报告");

            // 标题样式
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // 表头
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 数据行
            int rowNum = 1;
            for (EnergyReading reading : readings) {
                Row row = sheet.createRow(rowNum++);
                int col = 0;
                row.createCell(col++).setCellValue(formatTimestamp(reading.getTimestamp()));
                row.createCell(col++).setCellValue(safeStr(reading.getMeterCode()));
                row.createCell(col++).setCellValue(safeDouble(reading.getVoltage()));
                row.createCell(col++).setCellValue(safeDouble(reading.getCurrent()));
                row.createCell(col++).setCellValue(safeDouble(reading.getActivePower()));
                row.createCell(col++).setCellValue(safeDouble(reading.getPowerFactor()));
                row.createCell(col++).setCellValue(safeDouble(reading.getEnergyConsumption()));
                row.createCell(col).setCellValue(peakFlagText(reading.getPeakFlag()));
            }

            // 自动列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            log.info("能耗报告 Excel 导出成功: meterCode={}, 记录数={}", meter.getMeterCode(), readings.size());
            return baos.toByteArray();
        } catch (NoClassDefFoundError e) {
            log.warn("Apache POI 未找到，回退为 CSV 格式: {}", e.getMessage());
            return exportEnergyCsv(meter, readings);
        } catch (Exception e) {
            log.error("导出能耗 Excel 失败", e);
            throw new GlobalExceptionHandler.BusinessException("导出能耗 Excel 失败: " + e.getMessage());
        }
    }

    @Override
    public byte[] exportEnergyReportPdf(Long meterId, String startDate, String endDate) {
        EnergyMeter meter = energyMeterMapper.selectById(meterId);
        if (meter == null) {
            throw new GlobalExceptionHandler.BusinessException(
                    ResultCode.NOT_FOUND, "能源表不存在: " + meterId);
        }

        long[] timeRange = parseDateRange(startDate, endDate);
        List<EnergyReading> readings = queryReadings(meter.getMeterCode(), timeRange[0], timeRange[1]);

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
        html.append("<style>");
        html.append("body{font-family:'SimSun',sans-serif;margin:20px;}");
        html.append("h1{text-align:center;color:#333;}");
        html.append("table{border-collapse:collapse;width:100%;margin-top:20px;}");
        html.append("th,td{border:1px solid #999;padding:6px 10px;text-align:center;}");
        html.append("th{background:#4472C4;color:#fff;}");
        html.append("tr:nth-child(even){background:#f2f2f2;}");
        html.append(".summary{margin:10px 0;font-size:14px;color:#666;}");
        html.append("</style></head><body>");

        html.append("<h1>能耗报告</h1>");
        html.append("<div class='summary'>");
        html.append("<p>能源表编号：").append(meter.getMeterCode()).append("</p>");
        html.append("<p>能源表名称：").append(safeStr(meter.getMeterName())).append("</p>");
        html.append("<p>报表周期：").append(startDate).append(" 至 ").append(endDate).append("</p>");
        html.append("<p>记录总数：").append(readings.size()).append(" 条</p>");
        html.append("</div>");

        html.append("<table>");
        html.append("<tr><th>时间</th><th>电表编号</th><th>电压(V)</th><th>电流(A)</th>")
                .append("<th>有功功率(kW)</th><th>功率因数</th><th>用电量(kWh)</th><th>峰谷标志</th></tr>");

        double totalConsumption = 0.0;
        for (EnergyReading reading : readings) {
            html.append("<tr>");
            html.append("<td>").append(formatTimestamp(reading.getTimestamp())).append("</td>");
            html.append("<td>").append(safeStr(reading.getMeterCode())).append("</td>");
            html.append("<td>").append(safeDouble(reading.getVoltage())).append("</td>");
            html.append("<td>").append(safeDouble(reading.getCurrent())).append("</td>");
            html.append("<td>").append(safeDouble(reading.getActivePower())).append("</td>");
            html.append("<td>").append(safeDouble(reading.getPowerFactor())).append("</td>");
            html.append("<td>").append(safeDouble(reading.getEnergyConsumption())).append("</td>");
            html.append("<td>").append(peakFlagText(reading.getPeakFlag())).append("</td>");
            html.append("</tr>");
            if (reading.getEnergyConsumption() != null) {
                totalConsumption += reading.getEnergyConsumption();
            }
        }
        html.append("</table>");

        html.append("<div class='summary'>");
        html.append("<p>总用电量：").append(Math.round(totalConsumption * 1000) / 1000.0).append(" kWh</p>");
        html.append("<p>预估碳排放：").append(Math.round(totalConsumption * DEFAULT_EMISSION_FACTOR * 1000) / 1000.0).append(" kgCO2</p>");
        html.append("</div>");

        html.append("</body></html>");

        log.info("能耗报告 PDF(HTML) 导出成功: meterCode={}, 记录数={}", meter.getMeterCode(), readings.size());
        return html.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportCarbonReportExcel(String startDate, String endDate) {
        long[] timeRange = parseDateRange(startDate, endDate);

        // 查询当前租户所有能源表
        List<EnergyMeter> meters = energyMeterMapper.selectList(null);

        String[] headers = {"能源表编号", "能源表名称", "能源类型", "总用电量(kWh)", "排放因子(kgCO2/kWh)", "碳排放量(kgCO2)"};

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("碳排放报告");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            double totalCarbonEmission = 0.0;
            double totalConsumption = 0.0;
            int rowNum = 1;

            for (EnergyMeter meter : meters) {
                List<EnergyReading> readings = queryReadings(meter.getMeterCode(), timeRange[0], timeRange[1]);
                double consumption = readings.stream()
                        .mapToDouble(r -> r.getEnergyConsumption() != null ? r.getEnergyConsumption() : 0.0)
                        .sum();

                double emissionFactor = getEmissionFactor(meter.getMeterType());
                double carbonEmission = consumption * emissionFactor;

                totalConsumption += consumption;
                totalCarbonEmission += carbonEmission;

                Row row = sheet.createRow(rowNum++);
                int col = 0;
                row.createCell(col++).setCellValue(safeStr(meter.getMeterCode()));
                row.createCell(col++).setCellValue(safeStr(meter.getMeterName()));
                row.createCell(col++).setCellValue(safeStr(meter.getMeterType()));
                row.createCell(col++).setCellValue(Math.round(consumption * 1000) / 1000.0);
                row.createCell(col++).setCellValue(emissionFactor);
                row.createCell(col).setCellValue(Math.round(carbonEmission * 1000) / 1000.0);
            }

            // 合计行
            Row totalRow = sheet.createRow(rowNum);
            totalRow.createCell(0).setCellValue("合计");
            totalRow.createCell(3).setCellValue(Math.round(totalConsumption * 1000) / 1000.0);
            totalRow.createCell(5).setCellValue(Math.round(totalCarbonEmission * 1000) / 1000.0);

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            log.info("碳排放报告 Excel 导出成功: 能源表数={}, 总排放={}kgCO2", meters.size(),
                    Math.round(totalCarbonEmission * 1000) / 1000.0);
            return baos.toByteArray();
        } catch (NoClassDefFoundError e) {
            log.warn("Apache POI 未找到，回退为 CSV 格式: {}", e.getMessage());
            return exportCarbonCsv(meters, timeRange[0], timeRange[1]);
        } catch (Exception e) {
            log.error("导出碳排放 Excel 失败", e);
            throw new GlobalExceptionHandler.BusinessException("导出碳排放 Excel 失败: " + e.getMessage());
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 解析日期范围为时间戳数组 [start, end)
     */
    private long[] parseDateRange(String startDate, String endDate) {
        LocalDate start = LocalDate.parse(startDate, DATE_FMT);
        LocalDate end = LocalDate.parse(endDate, DATE_FMT);
        long startMillis = start.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long endMillis = end.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        return new long[]{startMillis, endMillis};
    }

    /**
     * 查询指定能源表在时间范围内的读数
     */
    private List<EnergyReading> queryReadings(String meterCode, long startTime, long endTime) {
        LambdaQueryWrapper<EnergyReading> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EnergyReading::getMeterCode, meterCode)
                .ge(EnergyReading::getTimestamp, startTime)
                .lt(EnergyReading::getTimestamp, endTime)
                .orderByDesc(EnergyReading::getTimestamp);
        return energyReadingMapper.selectList(wrapper);
    }

    /**
     * 根据能源类型获取碳排放因子（kgCO2/kWh）
     */
    private double getEmissionFactor(String meterType) {
        if (meterType == null) {
            return DEFAULT_EMISSION_FACTOR;
        }
        return switch (meterType) {
            case "WATER" -> 0.0;
            case "GAS" -> 2.1622;
            default -> DEFAULT_EMISSION_FACTOR;
        };
    }

    /**
     * 峰谷标志转中文
     */
    private String peakFlagText(Integer peakFlag) {
        if (peakFlag == null) {
            return "";
        }
        return switch (peakFlag) {
            case 1 -> "峰";
            case 2 -> "谷";
            default -> "平";
        };
    }

    /**
     * 时间戳格式化为日期时间字符串
     */
    private String formatTimestamp(Long timestamp) {
        if (timestamp == null) {
            return "";
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
                .format(DATETIME_FMT);
    }

    /**
     * 安全转字符串
     */
    private String safeStr(String value) {
        return value != null ? value : "";
    }

    /**
     * 安全转数值（用于 Excel 单元格）
     */
    private double safeDouble(Double value) {
        return value != null ? value : 0.0;
    }

    /**
     * CSV 格式能耗报告（POI 不可用时的降级方案）
     */
    private byte[] exportEnergyCsv(EnergyMeter meter, List<EnergyReading> readings) {
        StringBuilder csv = new StringBuilder();
        csv.append("时间,电表编号,电压(V),电流(A),有功功率(kW),功率因数,用电量(kWh),峰谷标志\n");
        for (EnergyReading reading : readings) {
            csv.append(formatTimestamp(reading.getTimestamp())).append(",");
            csv.append(safeStr(reading.getMeterCode())).append(",");
            csv.append(safeDouble(reading.getVoltage())).append(",");
            csv.append(safeDouble(reading.getCurrent())).append(",");
            csv.append(safeDouble(reading.getActivePower())).append(",");
            csv.append(safeDouble(reading.getPowerFactor())).append(",");
            csv.append(safeDouble(reading.getEnergyConsumption())).append(",");
            csv.append(peakFlagText(reading.getPeakFlag())).append("\n");
        }
        // 添加 BOM 头以支持 Excel 正确识别 UTF-8 编码
        byte[] content = csv.toString().getBytes(StandardCharsets.UTF_8);
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] result = new byte[bom.length + content.length];
        System.arraycopy(bom, 0, result, 0, bom.length);
        System.arraycopy(content, 0, result, bom.length, content.length);
        return result;
    }

    /**
     * CSV 格式碳排放报告（POI 不可用时的降级方案）
     */
    private byte[] exportCarbonCsv(List<EnergyMeter> meters, long startTime, long endTime) {
        StringBuilder csv = new StringBuilder();
        csv.append("能源表编号,能源表名称,能源类型,总用电量(kWh),排放因子(kgCO2/kWh),碳排放量(kgCO2)\n");
        for (EnergyMeter meter : meters) {
            List<EnergyReading> readings = queryReadings(meter.getMeterCode(), startTime, endTime);
            double consumption = readings.stream()
                    .mapToDouble(r -> r.getEnergyConsumption() != null ? r.getEnergyConsumption() : 0.0)
                    .sum();
            double factor = getEmissionFactor(meter.getMeterType());
            double carbon = consumption * factor;
            csv.append(safeStr(meter.getMeterCode())).append(",");
            csv.append(safeStr(meter.getMeterName())).append(",");
            csv.append(safeStr(meter.getMeterType())).append(",");
            csv.append(Math.round(consumption * 1000) / 1000.0).append(",");
            csv.append(factor).append(",");
            csv.append(Math.round(carbon * 1000) / 1000.0).append("\n");
        }
        byte[] content = csv.toString().getBytes(StandardCharsets.UTF_8);
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] result = new byte[bom.length + content.length];
        System.arraycopy(bom, 0, result, 0, bom.length);
        System.arraycopy(content, 0, result, bom.length, content.length);
        return result;
    }
}
