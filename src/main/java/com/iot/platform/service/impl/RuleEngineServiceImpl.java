package com.iot.platform.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.iot.platform.common.GlobalExceptionHandler;
import com.iot.platform.common.ResultCode;
import com.iot.platform.model.dto.AlarmResult;
import com.iot.platform.model.dto.DevicePropertyFact;
import com.iot.platform.model.entity.AlarmRule;
import com.iot.platform.repository.AlarmRuleMapper;
import com.iot.platform.service.AlarmService;
import com.iot.platform.service.RuleEngineService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.io.Resource;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Drools 规则引擎服务实现
 * <p>
 * 使用 KieServices + KieFileSystem 动态编译 DRL 规则：
 * <ol>
 *     <li>启动时加载 classpath 默认规则 + 数据库启用规则到 KieContainer</li>
 *     <li>评估时创建 KieSession，注入 DevicePropertyFact 触发规则，回收 AlarmResult</li>
 *     <li>规则触发后调用 {@link AlarmService#createAlarm} 落库告警</li>
 *     <li>规则 CRUD 变更后自动重载</li>
 * </ol>
 *
 * @author iot-platform
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RuleEngineServiceImpl extends ServiceImpl<AlarmRuleMapper, AlarmRule> implements RuleEngineService {

    /** 默认规则文件路径 */
    private static final String DEFAULT_RULES_PATH = "rules/default-alarm-rules.drl";

    /** 动态规则包前缀（每条规则使用独立包，避免规则名冲突） */
    private static final String DYNAMIC_PACKAGE_PREFIX = "com.iot.platform.rules.dynamic";

    private final AlarmRuleMapper alarmRuleMapper;
    private final AlarmService alarmService;
    private final KieServices kieServices;
    private final KieContainer defaultKieContainer;

    /** 当前生效的 KieContainer（重载时整体替换） */
    private volatile KieContainer activeContainer;

    @PostConstruct
    public void init() {
        activeContainer = defaultKieContainer;
        // 启动时尝试从数据库加载自定义规则，失败则保留默认容器
        try {
            reloadRules();
        } catch (Exception e) {
            log.warn("启动时加载规则失败，使用默认规则容器: {}", e.getMessage());
        }
    }

    @Override
    public List<AlarmResult> evaluateRule(DevicePropertyFact fact) {
        return evaluate(fact, true);
    }

    @Override
    public List<AlarmResult> testRule(DevicePropertyFact fact) {
        return evaluate(fact, false);
    }

    @Override
    public synchronized void reloadRules() {
        try {
            KieFileSystem kfs = kieServices.newKieFileSystem();
            // 写入默认规则
            String defaultDrl = loadDefaultRules();
            if (StrUtil.isNotBlank(defaultDrl)) {
                kfs.write(buildResource(defaultDrl, DEFAULT_RULES_PATH));
            }
            // 加载数据库中所有启用的规则（跨租户，规则引擎为全局会话）
            List<AlarmRule> rules = alarmRuleMapper.selectAllEnabled();
            if (rules != null) {
                for (int i = 0; i < rules.size(); i++) {
                    AlarmRule rule = rules.get(i);
                    String drl = normalizeDrl(rule.getDrlContent(), i);
                    if (StrUtil.isNotBlank(drl)) {
                        String path = "src/main/resources/rules/db_rule_" + rule.getId() + ".drl";
                        kfs.write(path, buildResource(drl, path));
                    }
                }
            }
            KieBuilder kieBuilder = kieServices.newKieBuilder(kfs);
            kieBuilder.buildAll();
            if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
                log.error("规则编译失败，保留旧规则集: {}", kieBuilder.getResults().getMessages());
                return;
            }
            KieContainer newContainer = kieServices.newKieContainer(
                    kieBuilder.getKieModule().getReleaseId());
            activeContainer = newContainer;
            log.info("规则重载完成: 默认规则 + 数据库规则 {}", rules == null ? 0 : rules.size());
        } catch (Exception e) {
            log.error("重新加载规则异常: {}", e.getMessage(), e);
        }
    }

    @Override
    public AlarmRule addRule(AlarmRule rule) {
        if (rule.getEnabled() == null) {
            rule.setEnabled(1);
        }
        alarmRuleMapper.insert(rule);
        reloadRules();
        log.info("新增告警规则: id={}, name={}", rule.getId(), rule.getRuleName());
        return rule;
    }

    @Override
    public void updateRule(AlarmRule rule) {
        if (rule.getId() == null || alarmRuleMapper.selectById(rule.getId()) == null) {
            throw new GlobalExceptionHandler.BusinessException(ResultCode.NOT_FOUND, "规则不存在: " + rule.getId());
        }
        alarmRuleMapper.updateById(rule);
        reloadRules();
        log.info("更新告警规则: id={}", rule.getId());
    }

    @Override
    public void deleteRule(Long id) {
        if (alarmRuleMapper.selectById(id) == null) {
            throw new GlobalExceptionHandler.BusinessException(ResultCode.NOT_FOUND, "规则不存在: " + id);
        }
        alarmRuleMapper.deleteById(id);
        reloadRules();
        log.info("删除告警规则: id={}", id);
    }

    @Override
    public void enableRule(Long id, boolean enabled) {
        AlarmRule rule = alarmRuleMapper.selectById(id);
        if (rule == null) {
            throw new GlobalExceptionHandler.BusinessException(ResultCode.NOT_FOUND, "规则不存在: " + id);
        }
        AlarmRule update = new AlarmRule();
        update.setId(id);
        update.setEnabled(enabled ? 1 : 0);
        alarmRuleMapper.updateById(update);
        reloadRules();
        log.info("{} 告警规则: id={}", enabled ? "启用" : "禁用", id);
    }

    @Override
    public List<AlarmRule> listRules(String ruleType) {
        LambdaQueryWrapper<AlarmRule> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(ruleType)) {
            wrapper.eq(AlarmRule::getRuleType, ruleType);
        }
        wrapper.orderByDesc(AlarmRule::getCreateTime);
        return alarmRuleMapper.selectList(wrapper);
    }

    // ==================== 私有方法 ====================

    /**
     * 执行规则评估
     *
     * @param fact    设备属性事实
     * @param persist 是否落库告警
     * @return 触发的告警结果列表
     */
    private List<AlarmResult> evaluate(DevicePropertyFact fact, boolean persist) {
        List<AlarmResult> results = new ArrayList<>();
        if (fact == null) {
            return results;
        }
        // 重置结果集合
        fact.ensureResults();
        fact.getResults().clear();

        KieContainer container = activeContainer;
        if (container == null) {
            log.warn("KieContainer 未初始化，跳过规则评估");
            return results;
        }

        KieSession session = container.newKieSession();
        try {
            session.insert(fact);
            session.fireAllRules();
        } catch (Exception e) {
            log.error("规则评估异常: deviceId={}, error={}", fact.getDeviceId(), e.getMessage(), e);
        } finally {
            session.dispose();
        }

        if (fact.getResults() != null) {
            results.addAll(fact.getResults());
        }

        // 持久化告警
        if (persist) {
            for (AlarmResult r : results) {
                try {
                    alarmService.createAlarm(r.getDeviceId(), fact.getProductId(),
                            r.getLevel(), r.getType(), r.getTitle(), r.getContent(),
                            r.getPropertyIdentifier(), r.getAlarmValue());
                } catch (Exception e) {
                    log.error("生成告警失败: deviceId={}, error={}", r.getDeviceId(), e.getMessage(), e);
                }
            }
        }
        return results;
    }

    /**
     * 构造 Kie 资源
     */
    private Resource buildResource(String content, String path) {
        return kieServices.getResources()
                .newByteArrayResource(content.getBytes(StandardCharsets.UTF_8))
                .setSourcePath(path);
    }

    /**
     * 规整 DRL：若无 package 声明，补全包名与 import，避免编译失败
     */
    private String normalizeDrl(String drl, int index) {
        if (StrUtil.isBlank(drl)) {
            return null;
        }
        String trimmed = drl.trim();
        if (trimmed.toLowerCase().startsWith("package ")) {
            return drl;
        }
        return "package " + DYNAMIC_PACKAGE_PREFIX + index + ";\n" +
                "import com.iot.platform.model.dto.DevicePropertyFact;\n" +
                "import com.iot.platform.model.dto.AlarmResult;\n\n" +
                drl;
    }

    /**
     * 从 classpath 读取默认规则文件
     */
    private String loadDefaultRules() {
        try {
            ClassPathResource resource = new ClassPathResource(DEFAULT_RULES_PATH);
            if (!resource.exists()) {
                return null;
            }
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("加载默认规则文件失败: {}", e.getMessage());
            return null;
        }
    }
}
