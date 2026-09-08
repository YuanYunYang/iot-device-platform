package com.iot.platform.service;

import java.util.Map;

/**
 * 碳排报告服务接口
 * <p>
 * 提供碳排放月度/年度报告自动生成与 PDF 导出能力。
 * 报告数据来源为 carbon_emission 与 energy_report 表。
 *
 * @author iot-platform
 */
public interface CarbonReportService {

    /**
     * 生成月度碳排放报告
     *
     * @param year  年份，如 "2026"
     * @param month 月份，如 "08"
     * @return 报告数据（含标题、排放量、按源/按日分布、节能建议）
     */
    Map<String, Object> generateCarbonReport(String year, String month);

    /**
     * 生成年度碳排放报告
     *
     * @param year 年份，如 "2026"
     * @return 报告数据
     */
    Map<String, Object> generateAnnualReport(String year);

    /**
     * 导出月度碳排放报告为 PDF 字节流
     *
     * @param year  年份
     * @param month 月份
     * @return PDF 文件字节数组
     */
    byte[] exportCarbonReportPdf(String year, String month);
}
