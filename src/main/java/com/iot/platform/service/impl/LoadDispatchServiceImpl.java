package com.iot.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.platform.mqtt.MqttClientManager;
import com.iot.platform.model.dto.LoadDispatchStrategyCreateDTO;
import com.iot.platform.model.entity.LoadDispatchStrategy;
import com.iot.platform.repository.LoadDispatchStrategyMapper;
import com.iot.platform.service.LoadDispatchService;
import com.iot.platform.tenant.TenantContext;
import com.iot.platform.model.vo.LoadDispatchVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 负荷调度服务实现
 * <p>
 * 管理负荷调度策略生命周期，并在满足触发条件时通过 MQTT 向目标设备
 * 下发控制指令（空调调温、充电限功率、照明调光、设备关停）。
 * 执行记录写入 Redis 列表，供历史查询使用。
 *
 * @author iot-platform
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoadDispatchServiceImpl extends ServiceImpl<LoadDispatchStrategyMapper, LoadDispatchStrategy>
        implements LoadDispatchService {

    private final LoadDispatchStrategyMapper loadDispatchStrategyMapper;
    private final MqttClientManager mqttClientManager;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Redis 执行历史 Key 前缀：iot:dispatch:history:{strategyId} */
    private static final String HISTORY_KEY_PREFIX = "iot:dispatch:history:";

    /** Redis 实时功率数据 Key 前缀：iot:device:power:{deviceId} */
    private static final String DEVICE_POWER_KEY_PREFIX = "iot:device:power:";

    @Override
    public LoadDispatchStrategy createStrategy(LoadDispatchStrategyCreateDTO dto) {
        LoadDispatchStrategy strategy = new LoadDispatchStrategy();
        strategy.setTenantId(TenantContext.getTenantId());
        strategy.setStrategyName(dto.getStrategyName());
        strategy.setStrategyType(dto.getStrategyType());
        strategy.setTargetDeviceId(dto.getTargetDeviceId());
        strategy.setActionType(dto.getActionType());
        strategy.setActionParams(dto.getActionParams());
        strategy.setTriggerCondition(dto.getTriggerCondition());
        strategy.setEnabled(1);
        strategy.setPriority(dto.getPriority() == null ? 0 : dto.getPriority());

        loadDispatchStrategyMapper.insert(strategy);
        log.info("创建负荷调度策略: id={}, name={}, type={}, deviceId={}",
                strategy.getId(), strategy.getStrategyName(), strategy.getStrategyType(), strategy.getTargetDeviceId());
        return strategy;
    }

    @Override
    public void executeStrategy(Long strategyId) {
        LoadDispatchStrategy strategy = loadDispatchStrategyMapper.selectById(strategyId);
        if (strategy == null) {
            throw new IllegalArgumentException("调度策略不存在: " + strategyId);
        }
        if (strategy.getEnabled() == null || strategy.getEnabled() == 0) {
            log.warn("策略已禁用，跳过执行: strategyId={}", strategyId);
            return;
        }

        String targetDeviceId = strategy.getTargetDeviceId();
        String actionType = strategy.getActionType();
        String actionParams = strategy.getActionParams();

        // 构建 MQTT 控制指令 payload
        Map<String, Object> command = new HashMap<>();
        command.put("command", actionType);
        command.put("actionType", actionType);
        command.put("strategyId", strategy.getId());
        command.put("strategyName", strategy.getStrategyName());
        command.put("reason", "负荷调度策略自动触发");
        if (actionParams != null && !actionParams.isBlank()) {
            try {
                command.put("params", objectMapper.readTree(actionParams));
            } catch (Exception e) {
                // 参数非合法 JSON 时以原始字符串透传
                command.put("params", actionParams);
                log.warn("策略 actionParams 非合法 JSON，按原始字符串下发: strategyId={}", strategyId);
            }
        }

        String topic = "iot/device/" + targetDeviceId + "/command";
        String payload;
        try {
            payload = objectMapper.writeValueAsString(command);
        } catch (Exception e) {
            log.error("序列化调度指令失败: strategyId={}", strategyId, e);
            throw new RuntimeException("调度指令序列化失败", e);
        }

        // 下发 MQTT 控制指令
        mqttClientManager.publish(topic, payload);
        log.info("负荷调度指令已下发: strategyId={}, topic={}, actionType={}",
                strategyId, topic, actionType);

        // 更新策略最后执行时间
        strategy.setLastExecuteTime(LocalDateTime.now());
        loadDispatchStrategyMapper.updateById(strategy);

        // 执行记录写入 Redis 列表
        Map<String, Object> history = new HashMap<>();
        history.put("strategyId", strategy.getId());
        history.put("strategyName", strategy.getStrategyName());
        history.put("deviceId", targetDeviceId);
        history.put("actionType", actionType);
        history.put("actionParams", actionParams);
        history.put("executeTime", LocalDateTime.now().toString());
        history.put("topic", topic);
        history.put("payload", payload);
        String historyKey = HISTORY_KEY_PREFIX + strategyId;
        redisTemplate.opsForList().rightPush(historyKey, history);
        log.debug("调度执行记录已写入 Redis: key={}", historyKey);
    }

    @Override
    public void executeAutoDispatch() {
        // 查询所有已启用策略，按优先级降序
        LambdaQueryWrapper<LoadDispatchStrategy> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LoadDispatchStrategy::getEnabled, 1);
        wrapper.orderByDesc(LoadDispatchStrategy::getPriority);
        List<LoadDispatchStrategy> strategies = loadDispatchStrategyMapper.selectList(wrapper);

        if (strategies.isEmpty()) {
            log.debug("无可执行的负荷调度策略");
            return;
        }

        log.info("开始自动负荷调度检查，待评估策略数: {}", strategies.size());
        int triggered = 0;
        for (LoadDispatchStrategy strategy : strategies) {
            try {
                if (isTriggered(strategy)) {
                    executeStrategy(strategy.getId());
                    triggered++;
                }
            } catch (Exception e) {
                log.error("策略执行异常: strategyId={}, error={}", strategy.getId(), e.getMessage(), e);
            }
        }
        log.info("自动负荷调度检查完成，触发执行策略数: {}/{}", triggered, strategies.size());
    }

    /**
     * 校验策略触发条件是否满足（简化版：从 Redis 读取目标设备实时功率数据）
     *
     * @param strategy 调度策略
     * @return 是否触发
     */
    @SuppressWarnings("unchecked")
    private boolean isTriggered(LoadDispatchStrategy strategy) {
        String condition = strategy.getTriggerCondition();
        if (condition == null || condition.isBlank()) {
            // 无触发条件视为始终触发
            return true;
        }

        // 从 Redis 读取目标设备实时功率数据
        Object raw = redisTemplate.opsForValue().get(DEVICE_POWER_KEY_PREFIX + strategy.getTargetDeviceId());
        if (raw == null) {
            log.debug("目标设备无实时功率数据，跳过策略: deviceId={}", strategy.getTargetDeviceId());
            return false;
        }

        Map<String, Object> deviceData;
        try {
            if (raw instanceof Map) {
                deviceData = (Map<String, Object>) raw;
            } else {
                deviceData = objectMapper.readValue(raw.toString(), Map.class);
            }
        } catch (Exception e) {
            log.warn("解析设备实时功率数据失败: deviceId={}", strategy.getTargetDeviceId());
            return false;
        }

        // 简化触发条件评估：解析 JSON 条件中的阈值键值，与设备数据比对
        // 条件格式示例：{"activePower":">800","peakFlag":"==1"}
        try {
            Map<String, String> conditions = objectMapper.readValue(condition, Map.class);
            for (Map.Entry<String, String> entry : conditions.entrySet()) {
                String key = entry.getKey();
                String expr = entry.getValue();
                Object val = deviceData.get(key);
                if (val == null) {
                    return false;
                }
                double actual = Double.parseDouble(val.toString());
                if (!matchExpression(actual, expr)) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.warn("解析触发条件失败，按始终不触发处理: strategyId={}, condition={}",
                    strategy.getId(), condition);
            return false;
        }
    }

    /**
     * 表达式匹配，支持 >、>=、<、<=、==、!= 前缀
     */
    private boolean matchExpression(double actual, String expr) {
        if (expr == null || expr.isBlank()) {
            return true;
        }
        String e = expr.trim();
        try {
            if (e.startsWith(">=")) {
                return actual >= Double.parseDouble(e.substring(2).trim());
            } else if (e.startsWith("<=")) {
                return actual <= Double.parseDouble(e.substring(2).trim());
            } else if (e.startsWith("==")) {
                return actual == Double.parseDouble(e.substring(2).trim());
            } else if (e.startsWith("!=")) {
                return actual != Double.parseDouble(e.substring(2).trim());
            } else if (e.startsWith(">")) {
                return actual > Double.parseDouble(e.substring(1).trim());
            } else if (e.startsWith("<")) {
                return actual < Double.parseDouble(e.substring(1).trim());
            }
            return actual == Double.parseDouble(e.trim());
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    @Override
    public List<LoadDispatchVO> listStrategies(String strategyType, int page, int size) {
        LambdaQueryWrapper<LoadDispatchStrategy> wrapper = new LambdaQueryWrapper<>();
        if (strategyType != null && !strategyType.isBlank()) {
            wrapper.eq(LoadDispatchStrategy::getStrategyType, strategyType);
        }
        wrapper.orderByDesc(LoadDispatchStrategy::getPriority);
        wrapper.orderByDesc(LoadDispatchStrategy::getCreateTime);

        Page<LoadDispatchStrategy> pageResult = loadDispatchStrategyMapper.selectPage(
                new Page<>(page, size), wrapper);
        return convertToVOList(pageResult.getRecords());
    }

    @Override
    public void enableStrategy(Long id, boolean enabled) {
        LoadDispatchStrategy strategy = loadDispatchStrategyMapper.selectById(id);
        if (strategy == null) {
            throw new IllegalArgumentException("调度策略不存在: " + id);
        }
        strategy.setEnabled(enabled ? 1 : 0);
        loadDispatchStrategyMapper.updateById(strategy);
        log.info("更新策略启停状态: id={}, enabled={}", id, enabled);
    }

    @Override
    public void deleteStrategy(Long id) {
        loadDispatchStrategyMapper.deleteById(id);
        log.info("删除负荷调度策略: id={}", id);
    }

    @Override
    public List<LoadDispatchVO> getExecutionHistory(int page, int size) {
        // 查询所有策略，按最后执行时间降序，再补充 Redis 中的执行次数
        LambdaQueryWrapper<LoadDispatchStrategy> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(LoadDispatchStrategy::getLastExecuteTime);
        Page<LoadDispatchStrategy> pageResult = loadDispatchStrategyMapper.selectPage(
                new Page<>(page, size), wrapper);
        return convertToVOList(pageResult.getRecords());
    }

    /**
     * 将策略实体列表转换为视图列表，并补充 Redis 中的累计执行次数
     */
    private List<LoadDispatchVO> convertToVOList(List<LoadDispatchStrategy> strategies) {
        List<LoadDispatchVO> voList = new ArrayList<>();
        for (LoadDispatchStrategy strategy : strategies) {
            LoadDispatchVO vo = new LoadDispatchVO();
            vo.setId(strategy.getId());
            vo.setStrategyName(strategy.getStrategyName());
            vo.setStrategyType(strategy.getStrategyType());
            vo.setTargetDeviceId(strategy.getTargetDeviceId());
            vo.setActionType(strategy.getActionType());
            vo.setActionParams(strategy.getActionParams());
            vo.setTriggerCondition(strategy.getTriggerCondition());
            vo.setEnabled(strategy.getEnabled());
            vo.setPriority(strategy.getPriority());
            vo.setLastExecuteTime(strategy.getLastExecuteTime());

            // 从 Redis 获取累计执行次数
            Long count = redisTemplate.opsForList().size(HISTORY_KEY_PREFIX + strategy.getId());
            vo.setExecuteCount(count == null ? 0 : count.intValue());
            voList.add(vo);
        }
        return voList;
    }
}
