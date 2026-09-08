package com.iot.platform.config;

import lombok.extern.slf4j.Slf4j;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.builder.Message;
import org.kie.api.builder.ReleaseId;
import org.kie.api.io.Resource;
import org.kie.api.runtime.KieContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

/**
 * Drools 规则引擎配置
 * <p>
 * 初始化 KieServices 与默认 KieContainer（加载 classpath:rules/default-alarm-rules.drl）。
 * 动态规则由 {@code RuleEngineService} 在运行时通过 KieFileSystem 重新编译并替换 KieContainer。
 *
 * @author iot-platform
 */
@Slf4j
@Configuration
public class DroolsConfig {

    /** 默认规则文件路径（classpath 相对路径） */
    public static final String DEFAULT_RULES_PATH = "rules/default-alarm-rules.drl";

    /**
     * KieServices 单例
     */
    @Bean
    public KieServices kieServices() {
        return KieServices.Factory.get();
    }

    /**
     * 默认 KieContainer（仅含内置默认规则）
     */
    @Bean
    public KieContainer kieContainer(KieServices kieServices) {
        KieFileSystem kfs = kieServices.newKieFileSystem();
        String defaultDrl = loadDefaultRules();
        if (defaultDrl != null && !defaultDrl.isBlank()) {
            // 将默认规则写入 KieFileSystem
            Resource resource = kieServices.getResources()
                    .newByteArrayResource(defaultDrl.getBytes(StandardCharsets.UTF_8))
                    .setSourcePath(DEFAULT_RULES_PATH);
            kfs.write(resource);
        }
        KieBuilder kieBuilder = kieServices.newKieBuilder(kfs);
        kieBuilder.buildAll();
        if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
            log.error("默认告警规则编译失败: {}", kieBuilder.getResults().getMessages());
        }
        KieModule kieModule = kieBuilder.getKieModule();
        ReleaseId releaseId = kieModule.getReleaseId();
        log.info("Drools 默认 KieContainer 初始化完成: releaseId={}", releaseId);
        return kieServices.newKieContainer(releaseId);
    }

    /**
     * 从 classpath 读取默认规则文件内容
     */
    private String loadDefaultRules() {
        try {
            ClassPathResource resource = new ClassPathResource(DEFAULT_RULES_PATH);
            if (!resource.exists()) {
                log.warn("默认规则文件不存在: {}", DEFAULT_RULES_PATH);
                return null;
            }
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("加载默认规则文件失败: {}", e.getMessage(), e);
            return null;
        }
    }
}
