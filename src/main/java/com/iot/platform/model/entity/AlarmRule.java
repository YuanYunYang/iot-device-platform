package com.iot.platform.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 告警规则实体
 * <p>
 * 对应数据库 alarm_rule 表，存储 Drools DRL 规则内容与启用状态。
 * 规则由 {@code RuleEngineService} 加载到 KieSession，在设备属性上报时评估触发告警。
 *
 * @author iot-platform
 */
@Data
@TableName("alarm_rule")
public class AlarmRule implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID（由 MyBatis-Plus 多租户插件自动注入） */
    private Long tenantId;

    /** 规则名称 */
    private String ruleName;

    /** 规则类型：THRESHOLD/COMPOSITE/TIME_WINDOW */
    private String ruleType;

    /** DRL 规则内容 */
    private String drlContent;

    /** 是否启用：0-禁用 1-启用 */
    private Integer enabled;

    /** 规则描述 */
    private String description;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标识：0-未删除 1-已删除 */
    @TableLogic
    private Integer deleted;
}
