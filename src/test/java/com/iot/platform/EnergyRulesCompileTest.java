package com.iot.platform;

import org.junit.jupiter.api.Test;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 能源告警规则 DRL 编译校验测试
 * <p>
 * 仅校验 energy-alarm-rules.drl 可被 Drools 编译器正确编译，
 * 不依赖 Spring 容器与数据库，确保规则语法合法。
 *
 * @author iot-platform
 */
class EnergyRulesCompileTest {

    @Test
    void energyAlarmRulesShouldCompile() throws Exception {
        ClassPathResource resource = new ClassPathResource("rules/energy-alarm-rules.drl");
        assertTrue(resource.exists(), "energy-alarm-rules.drl 不存在于 classpath");
        String drl = resource.getContentAsString(StandardCharsets.UTF_8);

        KieServices kieServices = KieServices.Factory.get();
        KieFileSystem kfs = kieServices.newKieFileSystem();
        kfs.write(kieServices.getResources()
                .newByteArrayResource(drl.getBytes(StandardCharsets.UTF_8))
                .setSourcePath("rules/energy-alarm-rules.drl"));

        KieBuilder kieBuilder = kieServices.newKieBuilder(kfs);
        kieBuilder.buildAll();

        assertFalse(kieBuilder.getResults().hasMessages(Message.Level.ERROR),
                "energy-alarm-rules.drl 编译失败: " + kieBuilder.getResults().getMessages());
    }
}
