package com.iot.platform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.iot.platform.model.dto.EnergyReportQueryDTO;
import com.iot.platform.model.entity.EnergyReport;
import com.iot.platform.model.vo.EnergyReportVO;

import java.util.List;
import java.util.Map;

/**
 * 能耗分析服务接口
 * <p>
 * 提供能耗报表生成（日/月）、报表查询、能耗趋势分析及尖峰平谷分析等能力。
 *
 * @author iot-platform
 */
public interface EnergyAnalysisService extends IService<EnergyReport> {

    /**
     * 生成日报表
     * <p>
     * 从 Redis 读取仪表逐小时能耗读数，计算尖峰平谷电量、最大需量、
     * 平均功率因数、同比环比及电费后持久化。
     *
     * @param meterId 仪表ID
     * @param date    日期（yyyy-MM-dd）
     * @return 生成的能耗报表
     */
    EnergyReport generateDailyReport(Long meterId, String date);

    /**
     * 生成月报表
     * <p>
     * 汇总指定仪表在整月内各日日报表数据，生成月度能耗汇总。
     *
     * @param meterId 仪表ID
     * @param month   月份（yyyy-MM）
     * @return 生成的能耗报表
     */
    EnergyReport generateMonthlyReport(Long meterId, String month);

    /**
     * 查询能耗报表列表
     *
     * @param query 查询条件
     * @return 报表列表
     */
    List<EnergyReport> getReports(EnergyReportQueryDTO query);

    /**
     * 获取报表详情（含尖峰平谷分项电费及占比）
     *
     * @param reportId 报表ID
     * @return 报表视图
     */
    EnergyReportVO getReportDetail(Long reportId);

    /**
     * 获取能耗趋势
     *
     * @param meterId     仪表ID
     * @param startDate   起始日期
     * @param endDate     结束日期
     * @param granularity 粒度：DAILY/WEEKLY/MONTHLY
     * @return 趋势数据（labels 日期列表 + datasets 能耗值列表）
     */
    Map<String, Object> getEnergyTrend(Long meterId, String startDate, String endDate, String granularity);

    /**
     * 尖峰平谷分析
     *
     * @param meterId   仪表ID
     * @param startDate 起始日期
     * @param endDate   结束日期
     * @return 尖峰平谷能耗与电费分布
     */
    Map<String, Object> getPeakValleyAnalysis(Long meterId, String startDate, String endDate);
}
