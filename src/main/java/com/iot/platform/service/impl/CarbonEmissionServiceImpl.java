package com.iot.platform.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.iot.platform.model.dto.CarbonEmissionQueryDTO;
import com.iot.platform.model.entity.CarbonEmission;
import com.iot.platform.model.entity.EmissionFactor;
import com.iot.platform.model.entity.EnergyReport;
import com.iot.platform.model.vo.CarbonEmissionVO;
import com.iot.platform.repository.CarbonEmissionMapper;
import com.iot.platform.repository.EmissionFactorMapper;
import com.iot.platform.repository.EnergyReportMapper;
import com.iot.platform.service.CarbonEmissionService;
import com.iot.platform.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 碳排放核算服务实现
 * <p>
 * 基于能耗报表中的电力消耗数据，乘以中国电网平均排放因子（0.5810 kgCO2/kWh）
 * 计算 Scope 2 间接碳排放。同时提供排放因子管理及默认因子初始化能力。
 * <p>
 * 内置默认排放因子（中国电网及常见燃料）：
 * <ul>
 *     <li>电力（Scope 2）：0.5810 kgCO2/kWh</li>
 *     <li>天然气（Scope 1）：2.1622 kgCO2/m³</li>
 *     <li>柴油（Scope 1）：2.7301 kgCO2/L</li>
 *     <li>煤炭（Scope 1）：1.9776 kgCO2/kg</li>
 * </ul>
 *
 * @author iot-platform
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CarbonEmissionServiceImpl extends ServiceImpl<CarbonEmissionMapper, CarbonEmission>
        implements CarbonEmissionService {

    /** 中国电网平均排放因子（kgCO2/kWh） */
    private static final double ELECTRICITY_FACTOR = 0.5810;

    /** 天然气排放因子（kgCO2/m³） */
    private static final double NATURAL_GAS_FACTOR = 2.1622;

    /** 柴油排放因子（kgCO2/L） */
    private static final double DIESEL_FACTOR = 2.7301;

    /** 煤炭排放因子（kgCO2/kg） */
    private static final double COAL_FACTOR = 1.9776;

    private final CarbonEmissionMapper carbonEmissionMapper;
    private final EmissionFactorMapper emissionFactorMapper;
    private final EnergyReportMapper energyReportMapper;

    // ==================== 碳排放计算 ====================

    @Override
    public CarbonEmissionVO calculateEmission(String startDate, String endDate) {
        log.info("计算碳排放: {} ~ {}", startDate, endDate);

        // 1. 查询日期范围内所有能耗报表
        LambdaQueryWrapper<EnergyReport> reportWrapper = new LambdaQueryWrapper<>();
        reportWrapper.ge(EnergyReport::getReportDate, startDate)
                .le(EnergyReport::getReportDate, endDate)
                .orderByAsc(EnergyReport::getReportDate);
        List<EnergyReport> reports = energyReportMapper.selectList(reportWrapper);

        // 2. 查询电力排放因子（优先从因子表获取，回退到默认值）
        double electricityFactor = getEmissionFactorValue("ELECTRICITY", ELECTRICITY_FACTOR);

        // 3. 为每条能耗报表生成碳排放记录（Scope 2 - 电力）
        List<CarbonEmission> emissionList = new ArrayList<>();
        double scope2Total = 0.0;

        for (EnergyReport report : reports) {
            double energy = report.getTotalEnergy() == null ? 0.0 : report.getTotalEnergy();
            double co2 = round2(energy * electricityFactor);
            if (co2 <= 0) {
                continue;
            }

            // 尝试解析报表日期作为核算日期，解析失败则使用起始日期
            LocalDate calcDate;
            try {
                calcDate = LocalDate.parse(report.getReportDate());
            } catch (Exception e) {
                calcDate = LocalDate.parse(startDate);
            }

            CarbonEmission emission = new CarbonEmission();
            emission.setTenantId(TenantContext.getTenantId());
            emission.setScope("SCOPE_2");
            emission.setSourceType("ELECTRICITY");
            emission.setSourceName("电网用电 - " + report.getMeterCode());
            emission.setConsumption(round2(energy));
            emission.setConsumptionUnit("kWh");
            emission.setEmissionFactor(electricityFactor);
            emission.setCo2Emission(co2);
            emission.setCalculationDate(calcDate);

            carbonEmissionMapper.insert(emission);
            emissionList.add(emission);
            scope2Total += co2;
        }

        // 4. 汇总
        double totalEmission = scope2Total;
        double scope1Emission = 0.0;
        double scope2Emission = round2(scope2Total);

        // 按排放源类型分组
        Map<String, Double> emissionByType = new HashMap<>();
        for (CarbonEmission emission : emissionList) {
            String type = emission.getSourceType();
            emissionByType.merge(type, emission.getCo2Emission(), Double::sum);
        }
        // 格式化分组值
        Map<String, Double> formattedByType = emissionByType.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> round2(e.getValue())));

        // 5. 构建 VO
        CarbonEmissionVO vo = new CarbonEmissionVO();
        vo.setTotalEmission(round2(totalEmission));
        vo.setScope1Emission(round2(scope1Emission));
        vo.setScope2Emission(scope2Emission);
        vo.setEmissionList(emissionList);
        vo.setEmissionByType(formattedByType);

        log.info("碳排放计算完成: 总排放 {} kgCO2, Scope2 {} kgCO2, 记录 {} 条",
                vo.getTotalEmission(), vo.getScope2Emission(), emissionList.size());
        return vo;
    }

    // ==================== 排放记录查询 ====================

    @Override
    public List<CarbonEmission> getEmissionRecords(CarbonEmissionQueryDTO query) {
        LambdaQueryWrapper<CarbonEmission> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(query.getScope())) {
            wrapper.eq(CarbonEmission::getScope, query.getScope());
        }
        if (StrUtil.isNotBlank(query.getSourceType())) {
            wrapper.eq(CarbonEmission::getSourceType, query.getSourceType());
        }
        if (StrUtil.isNotBlank(query.getStartDate())) {
            wrapper.ge(CarbonEmission::getCalculationDate, LocalDate.parse(query.getStartDate()));
        }
        if (StrUtil.isNotBlank(query.getEndDate())) {
            wrapper.le(CarbonEmission::getCalculationDate, LocalDate.parse(query.getEndDate()));
        }
        wrapper.orderByDesc(CarbonEmission::getCalculationDate);
        return carbonEmissionMapper.selectList(wrapper);
    }

    // ==================== 排放因子管理 ====================

    @Override
    public void initDefaultFactors() {
        log.info("初始化默认排放因子");
        Long tenantId = TenantContext.getTenantId();

        // 检查是否已初始化（按租户+源类型去重）
        if (hasExistingFactor(tenantId, "ELECTRICITY")) {
            log.info("排放因子已存在，跳过初始化");
            return;
        }

        saveFactor(tenantId, "ELECTRICITY", "中国电网平均用电", ELECTRICITY_FACTOR, "kgCO2/kWh", "Scope 2 - 电网平均排放因子");
        saveFactor(tenantId, "NATURAL_GAS", "天然气", NATURAL_GAS_FACTOR, "kgCO2/m³", "Scope 1 - 天然气燃烧排放因子");
        saveFactor(tenantId, "DIESEL", "柴油", DIESEL_FACTOR, "kgCO2/L", "Scope 1 - 柴油燃烧排放因子");
        saveFactor(tenantId, "COAL", "煤炭", COAL_FACTOR, "kgCO2/kg", "Scope 1 - 煤炭燃烧排放因子");

        log.info("默认排放因子初始化完成: 共 4 条");
    }

    @Override
    public EmissionFactor addEmissionFactor(EmissionFactor factor) {
        factor.setTenantId(TenantContext.getTenantId());
        emissionFactorMapper.insert(factor);
        log.info("排放因子新增成功: sourceType={}, factorValue={}", factor.getSourceType(), factor.getFactorValue());
        return factor;
    }

    @Override
    public List<EmissionFactor> listEmissionFactors() {
        LambdaQueryWrapper<EmissionFactor> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(EmissionFactor::getSourceType);
        return emissionFactorMapper.selectList(wrapper);
    }

    // ==================== 私有方法 ====================

    /**
     * 从因子表查询指定源类型的排放因子值，不存在则返回默认值
     */
    private double getEmissionFactorValue(String sourceType, double defaultValue) {
        Long tenantId = TenantContext.getTenantId();
        LambdaQueryWrapper<EmissionFactor> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EmissionFactor::getTenantId, tenantId)
                .eq(EmissionFactor::getSourceType, sourceType)
                .last("LIMIT 1");
        EmissionFactor factor = emissionFactorMapper.selectOne(wrapper);
        if (factor != null && factor.getFactorValue() != null) {
            return factor.getFactorValue();
        }
        return defaultValue;
    }

    /**
     * 检查指定租户是否已存在某源类型的排放因子
     */
    private boolean hasExistingFactor(Long tenantId, String sourceType) {
        LambdaQueryWrapper<EmissionFactor> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EmissionFactor::getTenantId, tenantId)
                .eq(EmissionFactor::getSourceType, sourceType)
                .last("LIMIT 1");
        return emissionFactorMapper.selectOne(wrapper) != null;
    }

    /**
     * 构建并保存排放因子
     */
    private void saveFactor(Long tenantId, String sourceType, String sourceName,
                            double factorValue, String unit, String description) {
        EmissionFactor factor = new EmissionFactor();
        factor.setTenantId(tenantId);
        factor.setSourceType(sourceType);
        factor.setSourceName(sourceName);
        factor.setFactorValue(factorValue);
        factor.setUnit(unit);
        factor.setDescription(description);
        emissionFactorMapper.insert(factor);
    }

    /**
     * 保留两位小数
     */
    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
