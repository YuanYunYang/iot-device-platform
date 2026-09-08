package com.iot.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iot.platform.model.entity.CarbonEmission;
import com.iot.platform.model.entity.EnergyReport;
import com.iot.platform.repository.CarbonEmissionMapper;
import com.iot.platform.repository.EnergyReportMapper;
import com.iot.platform.service.CarbonReportService;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 碳排报告服务实现
 * <p>
 * 基于 carbon_emission 与 energy_report 表自动生成月度/年度碳排放报告，
 * 包含排放总量、范围1/范围2排放、按排放源与按日分布，以及节能建议。
 * 支持将月度报告导出为 PDF 字节流。
 *
 * @author iot-platform
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CarbonReportServiceImpl implements CarbonReportService {

    private final CarbonEmissionMapper carbonEmissionMapper;
    private final EnergyReportMapper energyReportMapper;

    @Override
    public Map<String, Object> generateCarbonReport(String year, String month) {
        log.info("生成月度碳排放报告: year={}, month={}", year, month);

        // 计算月份起止日期
        int yearVal = Integer.parseInt(year);
        int monthVal = Integer.parseInt(month);
        LocalDate monthStart = LocalDate.of(yearVal, monthVal, 1);
        LocalDate monthEnd = monthStart.plusMonths(1);

        // 查询该月碳排放记录
        LambdaQueryWrapper<CarbonEmission> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(CarbonEmission::getCalculationDate, monthStart)
                .lt(CarbonEmission::getCalculationDate, monthEnd)
                .orderByAsc(CarbonEmission::getCalculationDate);
        List<CarbonEmission> emissions = carbonEmissionMapper.selectList(wrapper);

        // 查询该月能耗报表（用于交叉分析）
        String startDateStr = monthStart.toString();
        String endDateStr = monthEnd.minusDays(1).toString();
        LambdaQueryWrapper<EnergyReport> energyWrapper = new LambdaQueryWrapper<>();
        energyWrapper.eq(EnergyReport::getReportType, "DAILY")
                .between(EnergyReport::getReportDate, startDateStr, endDateStr)
                .orderByAsc(EnergyReport::getReportDate);
        List<EnergyReport> energyReports = energyReportMapper.selectList(energyWrapper);

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("reportTitle", "碳排放月度报告 " + year + "年" + month + "月");
        report.put("year", year);
        report.put("month", month);

        // 排放总量汇总（新结构：co2Emission + scope 区分）
        double totalEmission = emissions.stream()
                .mapToDouble(e -> e.getCo2Emission() == null ? 0 : e.getCo2Emission())
                .sum();
        double scope1 = emissions.stream()
                .filter(e -> "SCOPE_1".equals(e.getScope()))
                .mapToDouble(e -> e.getCo2Emission() == null ? 0 : e.getCo2Emission())
                .sum();
        double scope2 = emissions.stream()
                .filter(e -> "SCOPE_2".equals(e.getScope()))
                .mapToDouble(e -> e.getCo2Emission() == null ? 0 : e.getCo2Emission())
                .sum();
        report.put("totalEmission", round2(totalEmission));
        report.put("scope1Emission", round2(scope1));
        report.put("scope2Emission", round2(scope2));
        report.put("recordCount", emissions.size());

        // 按排放源统计
        Map<String, Double> emissionBySource = emissions.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getSourceType() == null ? "UNKNOWN" : e.getSourceType(),
                        Collectors.summingDouble(e -> e.getCo2Emission() == null ? 0 : e.getCo2Emission())
                ));
        Map<String, Object> sourceMap = new LinkedHashMap<>();
        emissionBySource.forEach((k, v) -> sourceMap.put(k, round2(v)));
        report.put("emissionBySource", sourceMap);

        // 按日统计排放量
        Map<String, Object> emissionByDay = new LinkedHashMap<>();
        emissions.forEach(e -> {
            if (e.getCalculationDate() != null) {
                emissionByDay.put(e.getCalculationDate().toString(),
                        round2(e.getCo2Emission() == null ? 0 : e.getCo2Emission()));
            }
        });
        report.put("emissionByDay", emissionByDay);

        // 峰谷平用电汇总（来自能耗报表）
        double peakTotal = energyReports.stream()
                .mapToDouble(e -> e.getPeakEnergy() == null ? 0 : e.getPeakEnergy())
                .sum();
        double flatTotal = energyReports.stream()
                .mapToDouble(e -> e.getFlatEnergy() == null ? 0 : e.getFlatEnergy())
                .sum();
        double valleyTotal = energyReports.stream()
                .mapToDouble(e -> e.getValleyEnergy() == null ? 0 : e.getValleyEnergy())
                .sum();
        Map<String, Object> energySummary = new LinkedHashMap<>();
        energySummary.put("totalConsumption", round2(peakTotal + flatTotal + valleyTotal));
        energySummary.put("peakConsumption", round2(peakTotal));
        energySummary.put("flatConsumption", round2(flatTotal));
        energySummary.put("valleyConsumption", round2(valleyTotal));
        report.put("energySummary", energySummary);

        // 节能建议
        report.put("suggestions", buildSuggestions(totalEmission, scope1, scope2, emissionBySource, peakTotal));

        log.info("月度碳排放报告生成完成: totalEmission={} kgCO2, scope1={}, scope2={}",
                round2(totalEmission), round2(scope1), round2(scope2));
        return report;
    }

    @Override
    public Map<String, Object> generateAnnualReport(String year) {
        log.info("生成年度碳排放报告: year={}", year);

        int yearVal = Integer.parseInt(year);
        LocalDate yearStart = LocalDate.of(yearVal, 1, 1);
        LocalDate yearEnd = LocalDate.of(yearVal + 1, 1, 1);

        LambdaQueryWrapper<CarbonEmission> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(CarbonEmission::getCalculationDate, yearStart)
                .lt(CarbonEmission::getCalculationDate, yearEnd)
                .orderByAsc(CarbonEmission::getCalculationDate);
        List<CarbonEmission> emissions = carbonEmissionMapper.selectList(wrapper);

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("reportTitle", "碳排放年度报告 " + year + "年");
        report.put("year", year);

        double totalEmission = emissions.stream()
                .mapToDouble(e -> e.getCo2Emission() == null ? 0 : e.getCo2Emission())
                .sum();
        double scope1 = emissions.stream()
                .filter(e -> "SCOPE_1".equals(e.getScope()))
                .mapToDouble(e -> e.getCo2Emission() == null ? 0 : e.getCo2Emission())
                .sum();
        double scope2 = emissions.stream()
                .filter(e -> "SCOPE_2".equals(e.getScope()))
                .mapToDouble(e -> e.getCo2Emission() == null ? 0 : e.getCo2Emission())
                .sum();
        report.put("totalEmission", round2(totalEmission));
        report.put("scope1Emission", round2(scope1));
        report.put("scope2Emission", round2(scope2));
        report.put("recordCount", emissions.size());

        // 按月统计
        Map<String, Double> emissionByMonth = new TreeMap<>();
        emissions.forEach(e -> {
            if (e.getCalculationDate() != null) {
                String m = String.valueOf(e.getCalculationDate().getMonthValue());
                emissionByMonth.merge(m,
                        e.getCo2Emission() == null ? 0 : e.getCo2Emission(),
                        Double::sum);
            }
        });
        Map<String, Object> monthMap = new LinkedHashMap<>();
        emissionByMonth.forEach((k, v) -> monthMap.put(k, round2(v)));
        report.put("emissionByMonth", monthMap);

        // 按排放源统计
        Map<String, Double> emissionBySource = emissions.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getSourceType() == null ? "UNKNOWN" : e.getSourceType(),
                        Collectors.summingDouble(e -> e.getCo2Emission() == null ? 0 : e.getCo2Emission())
                ));
        Map<String, Object> sourceMap = new LinkedHashMap<>();
        emissionBySource.forEach((k, v) -> sourceMap.put(k, round2(v)));
        report.put("emissionBySource", sourceMap);

        report.put("suggestions", buildSuggestions(totalEmission, scope1, scope2, emissionBySource, 0));

        log.info("年度碳排放报告生成完成: totalEmission={} kgCO2", round2(totalEmission));
        return report;
    }

    @Override
    public byte[] exportCarbonReportPdf(String year, String month) {
        log.info("导出月度碳排放报告 PDF: year={}, month={}", year, month);
        Map<String, Object> report = generateCarbonReport(year, month);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD, Color.BLACK);
            Font headerFont = new Font(Font.HELVETICA, 12, Font.BOLD, Color.BLACK);
            Font normalFont = new Font(Font.HELVETICA, 11, Font.NORMAL, Color.DARK_GRAY);

            // 标题
            Paragraph title = new Paragraph(String.valueOf(report.get("reportTitle")), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(18f);
            document.add(title);

            // 概要信息
            document.add(new Paragraph("一、排放概要", headerFont));
            document.add(new Paragraph("  排放总量: " + report.get("totalEmission") + " kgCO2", normalFont));
            document.add(new Paragraph("  范围1排放(直接): " + report.get("scope1Emission") + " kgCO2", normalFont));
            document.add(new Paragraph("  范围2排放(间接): " + report.get("scope2Emission") + " kgCO2", normalFont));
            document.add(new Paragraph("  数据记录数: " + report.get("recordCount"), normalFont));
            document.add(Paragraph.getInstance(" "));

            // 排放源分布表格
            document.add(new Paragraph("二、按排放源分布", headerFont));
            @SuppressWarnings("unchecked")
            Map<String, Object> sourceMap = (Map<String, Object>) report.get("emissionBySource");
            PdfPTable sourceTable = new PdfPTable(2);
            sourceTable.setWidthPercentage(100);
            sourceTable.setSpacingBefore(8f);
            sourceTable.setSpacingAfter(12f);
            addTableHeader(sourceTable, "排放源", "排放量(kgCO2)");
            sourceMap.forEach((k, v) -> {
                sourceTable.addCell(k);
                sourceTable.addCell(String.valueOf(v));
            });
            document.add(sourceTable);

            // 每日排放表格
            document.add(new Paragraph("三、每日排放明细", headerFont));
            @SuppressWarnings("unchecked")
            Map<String, Object> dayMap = (Map<String, Object>) report.get("emissionByDay");
            PdfPTable dayTable = new PdfPTable(2);
            dayTable.setWidthPercentage(100);
            dayTable.setSpacingBefore(8f);
            dayTable.setSpacingAfter(12f);
            addTableHeader(dayTable, "日期", "排放量(kgCO2)");
            dayMap.forEach((k, v) -> {
                dayTable.addCell(k);
                dayTable.addCell(String.valueOf(v));
            });
            document.add(dayTable);

            // 节能建议
            document.add(new Paragraph("四、节能建议", headerFont));
            @SuppressWarnings("unchecked")
            List<String> suggestions = (List<String>) report.get("suggestions");
            int idx = 1;
            for (String s : suggestions) {
                document.add(new Paragraph("  " + idx + ". " + s, normalFont));
                idx++;
            }

            document.close();
            byte[] bytes = baos.toByteArray();
            log.info("碳排放报告 PDF 生成完成: size={} bytes", bytes.length);
            return bytes;
        } catch (Exception e) {
            log.error("生成碳排放报告 PDF 失败: year={}, month={}", year, month, e);
            throw new RuntimeException("碳排放报告 PDF 导出失败: " + e.getMessage(), e);
        }
    }

    /**
     * 添加表格表头
     */
    private void addTableHeader(PdfPTable table, String... headers) {
        Font headerFont = new Font(Font.HELVETICA, 11, Font.BOLD, Color.WHITE);
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
            cell.setBackgroundColor(new Color(79, 140, 240));
            cell.setPadding(6f);
            table.addCell(cell);
        }
    }

    /**
     * 基于排放数据生成节能建议
     */
    private List<String> buildSuggestions(double totalEmission, double scope1, double scope2,
                                         Map<String, Double> emissionBySource, double peakConsumption) {
        List<String> suggestions = new ArrayList<>();

        // 范围2占比高，说明外购电力是主要排放源
        if (totalEmission > 0 && scope2 / totalEmission > 0.7) {
            suggestions.add("外购电力为碳排放主要来源（占比超过70%），建议加大屋顶光伏等绿电消纳比例，降低范围2排放。");
        }

        // 排放源最大者
        emissionBySource.entrySet().stream()
                .max(Comparator.comparingDouble(Map.Entry::getValue))
                .ifPresent(e -> suggestions.add("主要排放源为 " + sourceDesc(e.getKey()) + "（"
                        + round2(e.getValue()) + " kgCO2），建议优先开展该排放源的能效改造。"));

        // 峰段用电偏高
        if (peakConsumption > 0) {
            suggestions.add("峰段用电量较大，建议配置储能或参与需求响应削峰填谷，降低最大需量电费与碳排放强度。");
        }

        // 总量阈值提醒
        if (totalEmission > 1000000) {
            suggestions.add("本月碳排放总量较高（超过 1000 tCO2），建议建立碳排放基准线并设定逐月下降目标。");
        }

        // 功率因数建议（基于排放源类型推断）
        if (emissionBySource.containsKey("ELECTRICITY")) {
            suggestions.add("建议加装无功补偿装置提升功率因数至 0.9 以上，减少线路损耗与力调电费。");
        }

        if (suggestions.isEmpty()) {
            suggestions.add("本期碳排放处于合理水平，建议持续监测并保持节能减排措施。");
        }
        return suggestions;
    }

    /**
     * 排放源类型描述
     */
    private String sourceDesc(String sourceType) {
        switch (sourceType) {
            case "ELECTRICITY": return "外购电力";
            case "NATURAL_GAS": return "天然气";
            case "COAL": return "煤炭";
            case "DIESEL": return "柴油";
            default: return sourceType;
        }
    }

    /**
     * 保留两位小数
     */
    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
