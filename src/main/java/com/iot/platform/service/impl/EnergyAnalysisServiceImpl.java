package com.iot.platform.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.iot.platform.common.GlobalExceptionHandler;
import com.iot.platform.common.ResultCode;
import com.iot.platform.model.dto.EnergyReportQueryDTO;
import com.iot.platform.model.entity.EnergyMeter;
import com.iot.platform.model.entity.EnergyReport;
import com.iot.platform.model.vo.EnergyReportVO;
import com.iot.platform.repository.EnergyMeterMapper;
import com.iot.platform.repository.EnergyReportMapper;
import com.iot.platform.service.EnergyAnalysisService;
import com.iot.platform.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 能耗分析服务实现
 * <p>
 * 实现能耗报表生成、趋势分析及尖峰平谷分析等核心逻辑。
 * 仪表逐小时能耗读数存储于 Redis（Key: iot:energy:reading:{meterId}:{date}），
 * 报表生成时按尖峰（8-11h/18-21h）、谷（0-7h）、平（其他时段）分时段汇总。
 *
 * @author iot-platform
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnergyAnalysisServiceImpl extends ServiceImpl<EnergyReportMapper, EnergyReport>
        implements EnergyAnalysisService {

    /** 能耗读数 Redis Key 前缀 */
    private static final String ENERGY_READING_KEY = "iot:energy:reading:";

    /** 尖峰电价（元/kWh） */
    private static final double PEAK_PRICE = 1.0;

    /** 平时段电价（元/kWh） */
    private static final double FLAT_PRICE = 0.6;

    /** 谷时段电价（元/kWh） */
    private static final double VALLEY_PRICE = 0.3;

    private final EnergyReportMapper energyReportMapper;
    private final EnergyMeterMapper energyMeterMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    // ==================== 报表生成 ====================

    @Override
    public EnergyReport generateDailyReport(Long meterId, String date) {
        log.info("生成日报表: meterId={}, date={}", meterId, date);

        // 1. 获取仪表信息
        EnergyMeter meter = energyMeterMapper.selectById(meterId);
        if (meter == null) {
            throw new GlobalExceptionHandler.BusinessException(
                    ResultCode.NOT_FOUND, "仪表不存在: " + meterId);
        }

        // 2. 从 Redis 查询当天逐小时能耗读数
        List<Map<String, Double>> hourlyReadings = getHourlyReadings(meterId, date);

        // 3. 计算尖峰平谷电量
        double totalEnergy = 0.0;
        double peakEnergy = 0.0;
        double valleyEnergy = 0.0;
        double flatEnergy = 0.0;
        double maxDemand = 0.0;
        double powerFactorSum = 0.0;
        int powerFactorCount = 0;

        for (int hour = 0; hour < 24; hour++) {
            Map<String, Double> reading = hourlyReadings.get(hour);
            double energy = reading.getOrDefault("energyConsumption", 0.0);
            double power = reading.getOrDefault("activePower", 0.0);
            double pf = reading.getOrDefault("powerFactor", 0.0);

            totalEnergy += energy;
            if (power > maxDemand) {
                maxDemand = power;
            }
            if (pf > 0) {
                powerFactorSum += pf;
                powerFactorCount++;
            }

            if (isPeakHour(hour)) {
                peakEnergy += energy;
            } else if (isValleyHour(hour)) {
                valleyEnergy += energy;
            } else {
                flatEnergy += energy;
            }
        }

        double avgPowerFactor = powerFactorCount > 0 ? powerFactorSum / powerFactorCount : 0.0;

        // 4. 计算同比、环比
        LocalDate currentDate = LocalDate.parse(date);
        Double yoyRatio = calculateYoyRatio(meterId, currentDate, round2(totalEnergy));
        Double momRatio = calculateMomRatio(meterId, currentDate, round2(totalEnergy));

        // 5. 计算电费
        double cost = peakEnergy * PEAK_PRICE + flatEnergy * FLAT_PRICE + valleyEnergy * VALLEY_PRICE;

        // 6. 构建并保存报表
        EnergyReport report = new EnergyReport();
        report.setTenantId(TenantContext.getTenantId());
        report.setMeterId(meterId);
        report.setMeterCode(meter.getMeterCode());
        report.setReportType("DAILY");
        report.setReportDate(date);
        report.setTotalEnergy(round2(totalEnergy));
        report.setPeakEnergy(round2(peakEnergy));
        report.setValleyEnergy(round2(valleyEnergy));
        report.setFlatEnergy(round2(flatEnergy));
        report.setMaxDemand(round2(maxDemand));
        report.setAvgPowerFactor(round2(avgPowerFactor));
        report.setYoyRatio(yoyRatio);
        report.setMomRatio(momRatio);
        report.setCost(round2(cost));

        energyReportMapper.insert(report);
        log.info("日报表生成成功: meterId={}, date={}, totalEnergy={}", meterId, date, report.getTotalEnergy());
        return report;
    }

    @Override
    public EnergyReport generateMonthlyReport(Long meterId, String month) {
        log.info("生成月报表: meterId={}, month={}", meterId, month);

        // 1. 获取仪表信息
        EnergyMeter meter = energyMeterMapper.selectById(meterId);
        if (meter == null) {
            throw new GlobalExceptionHandler.BusinessException(
                    ResultCode.NOT_FOUND, "仪表不存在: " + meterId);
        }

        // 2. 查询当月所有日报表
        YearMonth yearMonth = YearMonth.parse(month);
        String startDate = month + "-01";
        String endDate = month + "-" + String.format("%02d", yearMonth.lengthOfMonth());

        LambdaQueryWrapper<EnergyReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EnergyReport::getMeterId, meterId)
                .eq(EnergyReport::getReportType, "DAILY")
                .between(EnergyReport::getReportDate, startDate, endDate)
                .orderByAsc(EnergyReport::getReportDate);
        List<EnergyReport> dailyReports = energyReportMapper.selectList(wrapper);

        // 3. 汇总月度数据
        double totalEnergy = 0.0;
        double peakEnergy = 0.0;
        double valleyEnergy = 0.0;
        double flatEnergy = 0.0;
        double maxDemand = 0.0;
        double powerFactorSum = 0.0;
        int powerFactorCount = 0;
        double totalCost = 0.0;

        for (EnergyReport daily : dailyReports) {
            totalEnergy += safeGet(daily.getTotalEnergy());
            peakEnergy += safeGet(daily.getPeakEnergy());
            valleyEnergy += safeGet(daily.getValleyEnergy());
            flatEnergy += safeGet(daily.getFlatEnergy());
            if (safeGet(daily.getMaxDemand()) > maxDemand) {
                maxDemand = safeGet(daily.getMaxDemand());
            }
            if (daily.getAvgPowerFactor() != null && daily.getAvgPowerFactor() > 0) {
                powerFactorSum += daily.getAvgPowerFactor();
                powerFactorCount++;
            }
            totalCost += safeGet(daily.getCost());
        }

        double avgPowerFactor = powerFactorCount > 0 ? powerFactorSum / powerFactorCount : 0.0;

        // 4. 计算同比（去年同期）、环比（上月）
        Double yoyRatio = calculateMonthlyYoyRatio(meterId, month, round2(totalEnergy));
        Double momRatio = calculateMonthlyMomRatio(meterId, month, round2(totalEnergy));

        // 5. 构建并保存月报表
        EnergyReport report = new EnergyReport();
        report.setTenantId(TenantContext.getTenantId());
        report.setMeterId(meterId);
        report.setMeterCode(meter.getMeterCode());
        report.setReportType("MONTHLY");
        report.setReportDate(month);
        report.setTotalEnergy(round2(totalEnergy));
        report.setPeakEnergy(round2(peakEnergy));
        report.setValleyEnergy(round2(valleyEnergy));
        report.setFlatEnergy(round2(flatEnergy));
        report.setMaxDemand(round2(maxDemand));
        report.setAvgPowerFactor(round2(avgPowerFactor));
        report.setYoyRatio(yoyRatio);
        report.setMomRatio(momRatio);
        report.setCost(round2(totalCost));

        energyReportMapper.insert(report);
        log.info("月报表生成成功: meterId={}, month={}, totalEnergy={}", meterId, month, report.getTotalEnergy());
        return report;
    }

    // ==================== 报表查询 ====================

    @Override
    public List<EnergyReport> getReports(EnergyReportQueryDTO query) {
        LambdaQueryWrapper<EnergyReport> wrapper = new LambdaQueryWrapper<>();
        if (query.getMeterId() != null) {
            wrapper.eq(EnergyReport::getMeterId, query.getMeterId());
        }
        if (StrUtil.isNotBlank(query.getReportType())) {
            wrapper.eq(EnergyReport::getReportType, query.getReportType());
        }
        if (StrUtil.isNotBlank(query.getStartDate())) {
            wrapper.ge(EnergyReport::getReportDate, query.getStartDate());
        }
        if (StrUtil.isNotBlank(query.getEndDate())) {
            wrapper.le(EnergyReport::getReportDate, query.getEndDate());
        }
        wrapper.orderByDesc(EnergyReport::getReportDate);
        return energyReportMapper.selectList(wrapper);
    }

    @Override
    public EnergyReportVO getReportDetail(Long reportId) {
        EnergyReport report = energyReportMapper.selectById(reportId);
        if (report == null) {
            throw new GlobalExceptionHandler.BusinessException(
                    ResultCode.NOT_FOUND, "报表不存在: " + reportId);
        }
        return convertToVO(report);
    }

    // ==================== 趋势与尖峰平谷分析 ====================

    @Override
    public Map<String, Object> getEnergyTrend(Long meterId, String startDate, String endDate, String granularity) {
        log.info("获取能耗趋势: meterId={}, {} ~ {}, granularity={}", meterId, startDate, endDate, granularity);

        LambdaQueryWrapper<EnergyReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EnergyReport::getMeterId, meterId)
                .eq(EnergyReport::getReportType, granularity != null ? granularity : "DAILY")
                .ge(EnergyReport::getReportDate, startDate)
                .le(EnergyReport::getReportDate, endDate)
                .orderByAsc(EnergyReport::getReportDate);
        List<EnergyReport> reports = energyReportMapper.selectList(wrapper);

        List<String> labels = new ArrayList<>();
        List<Double> datasets = new ArrayList<>();
        for (EnergyReport report : reports) {
            labels.add(report.getReportDate());
            datasets.add(report.getTotalEnergy());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("labels", labels);
        result.put("datasets", datasets);
        return result;
    }

    @Override
    public Map<String, Object> getPeakValleyAnalysis(Long meterId, String startDate, String endDate) {
        log.info("尖峰平谷分析: meterId={}, {} ~ {}", meterId, startDate, endDate);

        LambdaQueryWrapper<EnergyReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EnergyReport::getMeterId, meterId)
                .eq(EnergyReport::getReportType, "DAILY")
                .ge(EnergyReport::getReportDate, startDate)
                .le(EnergyReport::getReportDate, endDate)
                .orderByAsc(EnergyReport::getReportDate);
        List<EnergyReport> reports = energyReportMapper.selectList(wrapper);

        double totalPeak = 0.0;
        double totalValley = 0.0;
        double totalFlat = 0.0;
        double totalEnergy = 0.0;
        double peakCost = 0.0;
        double valleyCost = 0.0;
        double flatCost = 0.0;
        double totalCost = 0.0;

        List<String> labels = new ArrayList<>();
        List<Double> peakData = new ArrayList<>();
        List<Double> valleyData = new ArrayList<>();
        List<Double> flatData = new ArrayList<>();

        for (EnergyReport report : reports) {
            double peak = safeGet(report.getPeakEnergy());
            double valley = safeGet(report.getValleyEnergy());
            double flat = safeGet(report.getFlatEnergy());

            totalPeak += peak;
            totalValley += valley;
            totalFlat += flat;
            totalEnergy += peak + valley + flat;
            peakCost += peak * PEAK_PRICE;
            valleyCost += valley * VALLEY_PRICE;
            flatCost += flat * FLAT_PRICE;
            totalCost += safeGet(report.getCost());

            labels.add(report.getReportDate());
            peakData.add(peak);
            valleyData.add(valley);
            flatData.add(flat);
        }

        Map<String, Object> energyBreakdown = new HashMap<>();
        energyBreakdown.put("peak", round2(totalPeak));
        energyBreakdown.put("valley", round2(totalValley));
        energyBreakdown.put("flat", round2(totalFlat));
        energyBreakdown.put("total", round2(totalEnergy));

        Map<String, Object> costBreakdown = new HashMap<>();
        costBreakdown.put("peak", round2(peakCost));
        costBreakdown.put("valley", round2(valleyCost));
        costBreakdown.put("flat", round2(flatCost));
        costBreakdown.put("total", round2(totalCost));

        Map<String, Object> ratioBreakdown = new HashMap<>();
        if (totalEnergy > 0) {
            ratioBreakdown.put("peak", round2(totalPeak / totalEnergy * 100));
            ratioBreakdown.put("valley", round2(totalValley / totalEnergy * 100));
            ratioBreakdown.put("flat", round2(totalFlat / totalEnergy * 100));
        } else {
            ratioBreakdown.put("peak", 0.0);
            ratioBreakdown.put("valley", 0.0);
            ratioBreakdown.put("flat", 0.0);
        }

        Map<String, Object> trend = new HashMap<>();
        trend.put("labels", labels);
        trend.put("peak", peakData);
        trend.put("valley", valleyData);
        trend.put("flat", flatData);

        Map<String, Object> result = new HashMap<>();
        result.put("energy", energyBreakdown);
        result.put("cost", costBreakdown);
        result.put("ratio", ratioBreakdown);
        result.put("trend", trend);
        return result;
    }

    // ==================== 私有方法 ====================

    /**
     * 从 Redis 获取仪表指定日期的逐小时能耗读数（共24条）
     * <p>
     * 读数以 Hash 结构存储，field 为小时编号（0-23），value 为包含
     * energyConsumption、activePower、powerFactor 的 Map。
     *
     * @param meterId 仪表ID
     * @param date    日期（yyyy-MM-dd）
     * @return 24条小时读数列表
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Double>> getHourlyReadings(Long meterId, String date) {
        String key = ENERGY_READING_KEY + meterId + ":" + date;
        Map<Object, Object> rawReadings = redisTemplate.opsForHash().entries(key);

        List<Map<String, Double>> readings = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            Map<String, Double> reading = new HashMap<>();
            Object raw = rawReadings.get(String.valueOf(hour));
            if (raw instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) raw;
                reading.put("energyConsumption", toDouble(map.get("energyConsumption")));
                reading.put("activePower", toDouble(map.get("activePower")));
                reading.put("powerFactor", toDouble(map.get("powerFactor")));
            } else {
                reading.put("energyConsumption", 0.0);
                reading.put("activePower", 0.0);
                reading.put("powerFactor", 0.0);
            }
            readings.add(reading);
        }
        return readings;
    }

    /**
     * 判断是否为尖峰时段（8-11h / 18-21h）
     */
    private boolean isPeakHour(int hour) {
        return (hour >= 8 && hour < 11) || (hour >= 18 && hour < 21);
    }

    /**
     * 判断是否为谷时段（0-7h）
     */
    private boolean isValleyHour(int hour) {
        return hour >= 0 && hour < 7;
    }

    /**
     * 计算同比（与去年同期日报表对比）
     */
    private Double calculateYoyRatio(Long meterId, LocalDate currentDate, double currentEnergy) {
        LocalDate yoyDate = currentDate.minusYears(1);
        EnergyReport yoyReport = getReportByMeterAndDate(meterId, "DAILY", yoyDate.toString());
        if (yoyReport != null && yoyReport.getTotalEnergy() != null && yoyReport.getTotalEnergy() > 0) {
            return round2((currentEnergy - yoyReport.getTotalEnergy()) / yoyReport.getTotalEnergy() * 100);
        }
        return null;
    }

    /**
     * 计算环比（与上月同日日报表对比）
     */
    private Double calculateMomRatio(Long meterId, LocalDate currentDate, double currentEnergy) {
        LocalDate momDate = currentDate.minusMonths(1);
        EnergyReport momReport = getReportByMeterAndDate(meterId, "DAILY", momDate.toString());
        if (momReport != null && momReport.getTotalEnergy() != null && momReport.getTotalEnergy() > 0) {
            return round2((currentEnergy - momReport.getTotalEnergy()) / momReport.getTotalEnergy() * 100);
        }
        return null;
    }

    /**
     * 计算月度同比（与去年同期月报表对比）
     */
    private Double calculateMonthlyYoyRatio(Long meterId, String month, double currentEnergy) {
        YearMonth ym = YearMonth.parse(month);
        String yoyMonth = ym.minusYears(1).toString();
        EnergyReport yoyReport = getReportByMeterAndDate(meterId, "MONTHLY", yoyMonth);
        if (yoyReport != null && yoyReport.getTotalEnergy() != null && yoyReport.getTotalEnergy() > 0) {
            return round2((currentEnergy - yoyReport.getTotalEnergy()) / yoyReport.getTotalEnergy() * 100);
        }
        return null;
    }

    /**
     * 计算月度环比（与上月月报表对比）
     */
    private Double calculateMonthlyMomRatio(Long meterId, String month, double currentEnergy) {
        YearMonth ym = YearMonth.parse(month);
        String momMonth = ym.minusMonths(1).toString();
        EnergyReport momReport = getReportByMeterAndDate(meterId, "MONTHLY", momMonth);
        if (momReport != null && momReport.getTotalEnergy() != null && momReport.getTotalEnergy() > 0) {
            return round2((currentEnergy - momReport.getTotalEnergy()) / momReport.getTotalEnergy() * 100);
        }
        return null;
    }

    /**
     * 根据仪表ID、报表类型及日期查询单条报表
     */
    private EnergyReport getReportByMeterAndDate(Long meterId, String reportType, String reportDate) {
        LambdaQueryWrapper<EnergyReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EnergyReport::getMeterId, meterId)
                .eq(EnergyReport::getReportType, reportType)
                .eq(EnergyReport::getReportDate, reportDate)
                .last("LIMIT 1");
        return energyReportMapper.selectOne(wrapper);
    }

    /**
     * 实体转 VO（含尖峰平谷分项电费及占比计算）
     */
    private EnergyReportVO convertToVO(EnergyReport report) {
        EnergyReportVO vo = new EnergyReportVO();
        vo.setId(report.getId());
        vo.setTenantId(report.getTenantId());
        vo.setMeterId(report.getMeterId());
        vo.setMeterCode(report.getMeterCode());
        vo.setReportType(report.getReportType());
        vo.setReportDate(report.getReportDate());
        vo.setTotalEnergy(report.getTotalEnergy());
        vo.setPeakEnergy(report.getPeakEnergy());
        vo.setValleyEnergy(report.getValleyEnergy());
        vo.setFlatEnergy(report.getFlatEnergy());
        vo.setMaxDemand(report.getMaxDemand());
        vo.setAvgPowerFactor(report.getAvgPowerFactor());
        vo.setYoyRatio(report.getYoyRatio());
        vo.setMomRatio(report.getMomRatio());
        vo.setCost(report.getCost());
        vo.setCreateTime(report.getCreateTime());
        vo.setUpdateTime(report.getUpdateTime());

        // 计算分项电费
        double peak = safeGet(report.getPeakEnergy());
        double valley = safeGet(report.getValleyEnergy());
        double flat = safeGet(report.getFlatEnergy());
        double total = peak + valley + flat;

        vo.setPeakCost(round2(peak * PEAK_PRICE));
        vo.setValleyCost(round2(valley * VALLEY_PRICE));
        vo.setFlatCost(round2(flat * FLAT_PRICE));

        if (total > 0) {
            vo.setPeakRatio(round2(peak / total * 100));
            vo.setValleyRatio(round2(valley / total * 100));
            vo.setFlatRatio(round2(flat / total * 100));
        } else {
            vo.setPeakRatio(0.0);
            vo.setValleyRatio(0.0);
            vo.setFlatRatio(0.0);
        }
        return vo;
    }

    /**
     * 安全获取 Double 值（null 视为 0）
     */
    private double safeGet(Double value) {
        return value == null ? 0.0 : value;
    }

    /**
     * 保留两位小数
     */
    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    /**
     * 将 Object 安全转为 Double
     */
    private Double toDouble(Object value) {
        if (value == null) {
            return 0.0;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
