package com.iot.platform.service;

/**
 * 数据导出服务接口
 * <p>
 * 提供能耗数据与碳排放数据的 Excel / PDF 导出能力。
 *
 * @author iot-platform
 */
public interface DataExportService {

    /**
     * 导出能耗报告 Excel
     *
     * @param meterId   能源表ID
     * @param startDate 开始日期（yyyy-MM-dd）
     * @param endDate   结束日期（yyyy-MM-dd）
     * @return Excel 文件字节数组
     */
    byte[] exportEnergyReportExcel(Long meterId, String startDate, String endDate);

    /**
     * 导出能耗报告 PDF
     *
     * @param meterId   能源表ID
     * @param startDate 开始日期（yyyy-MM-dd）
     * @param endDate   结束日期（yyyy-MM-dd）
     * @return PDF 文件字节数组
     */
    byte[] exportEnergyReportPdf(Long meterId, String startDate, String endDate);

    /**
     * 导出碳排放报告 Excel
     *
     * @param startDate 开始日期（yyyy-MM-dd）
     * @param endDate   结束日期（yyyy-MM-dd）
     * @return Excel 文件字节数组
     */
    byte[] exportCarbonReportExcel(String startDate, String endDate);
}
