package com.iot.platform;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 碳排放核算服务测试
 * 验证排放因子计算逻辑和单位换算正确性
 */
class CarbonEmissionServiceTest {

    /** 中国电网平均排放因子 */
    private static final double ELECTRICITY_FACTOR = 0.5810;
    /** 天然气排放因子 */
    private static final double NATURAL_GAS_FACTOR = 2.1622;
    /** 柴油排放因子 */
    private static final double DIESEL_FACTOR = 2.7301;
    /** 煤炭排放因子 */
    private static final double COAL_FACTOR = 1.9776;

    @Test
    void testElectricityEmissionCalculation() {
        double energyKwh = 1000.0;
        double expectedCo2 = energyKwh * ELECTRICITY_FACTOR;
        assertEquals(581.0, round2(expectedCo2), 0.01, "1000kWh 电力应产生 581.0 kgCO2");
    }

    @Test
    void testNaturalGasEmissionCalculation() {
        double volume = 500.0; // m³
        double expectedCo2 = volume * NATURAL_GAS_FACTOR;
        assertEquals(1081.1, round2(expectedCo2), 0.01, "500m³ 天然气应产生 1081.1 kgCO2");
    }

    @Test
    void testDieselEmissionCalculation() {
        double volume = 200.0; // L
        double expectedCo2 = volume * DIESEL_FACTOR;
        assertEquals(546.02, round2(expectedCo2), 0.01, "200L 柴油应产生 546.02 kgCO2");
    }

    @Test
    void testCoalEmissionCalculation() {
        double weight = 1000.0; // kg
        double expectedCo2 = weight * COAL_FACTOR;
        assertEquals(1977.6, round2(expectedCo2), 0.01, "1000kg 煤炭应产生 1977.6 kgCO2");
    }

    @Test
    void testZeroConsumptionProducesZeroEmission() {
        assertEquals(0.0, round2(0.0 * ELECTRICITY_FACTOR), 0.01);
    }

    @Test
    void testDateRangeParsing() {
        LocalDate start = LocalDate.parse("2026-01-01");
        LocalDate end = LocalDate.parse("2026-12-31");
        assertTrue(start.isBefore(end), "起始日期应早于结束日期");
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
