package com.iot.platform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.iot.platform.model.dto.AlarmResult;
import com.iot.platform.model.dto.DevicePropertyFact;
import com.iot.platform.model.entity.AlarmRule;

import java.util.List;

/**
 * Drools 规则引擎服务接口
 * <p>
 * 提供告警规则的 CRUD、动态加载与评估能力：
 * <ul>
 *     <li>评估：注入 {@link DevicePropertyFact} 触发规则，回收告警结果并落库</li>
 *     <li>重载：从数据库重新编译 DRL 规则到 KieContainer</li>
 *     <li>CRUD：管理规则，变更后自动重载</li>
 * </ul>
 *
 * @author iot-platform
 */
public interface RuleEngineService extends IService<AlarmRule> {

    /**
     * 评估规则（生产模式）：触发规则并调用 AlarmService 生成告警
     *
     * @param fact 设备属性事实
     * @return 触发的告警结果列表
     */
    List<AlarmResult> evaluateRule(DevicePropertyFact fact);

    /**
     * 测试评估（干运行）：仅返回评估结果，不落库告警
     *
     * @param fact 模拟的设备属性事实
     * @return 触发的告警结果列表
     */
    List<AlarmResult> testRule(DevicePropertyFact fact);

    /**
     * 从数据库重新加载全部启用规则到 KieContainer
     */
    void reloadRules();

    /**
     * 新增规则并重载
     *
     * @param rule 规则实体
     * @return 持久化后的规则
     */
    AlarmRule addRule(AlarmRule rule);

    /**
     * 更新规则并重载
     *
     * @param rule 规则实体
     */
    void updateRule(AlarmRule rule);

    /**
     * 删除规则并重载
     *
     * @param id 规则ID
     */
    void deleteRule(Long id);

    /**
     * 启用/禁用规则并重载
     *
     * @param id      规则ID
     * @param enabled 是否启用
     */
    void enableRule(Long id, boolean enabled);

    /**
     * 查询规则列表（受多租户过滤）
     *
     * @param ruleType 规则类型过滤（可空）
     * @return 规则列表
     */
    List<AlarmRule> listRules(String ruleType);
}
