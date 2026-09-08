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
 * OTA 升级任务实体
 * <p>
 * 对应数据库 ota_task 表，描述一次固件升级任务的目标范围、下发策略与执行统计。
 * 任务创建后批量生成设备升级进度记录（ota_device_progress），并通过 MQTT 下发升级指令。
 *
 * @author iot-platform
 */
@Data
@TableName("ota_task")
public class OtaTask implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID（由 MyBatis-Plus 多租户插件自动注入） */
    private Long tenantId;

    /** 关联固件ID */
    private Long firmwareId;

    /** 任务名称 */
    private String taskName;

    /** 目标范围：ALL/SELECTED/BY_PRODUCT */
    private String targetType;

    /** 目标产品标识（targetType=BY_PRODUCT 时使用） */
    private String targetProductKey;

    /** 目标设备ID列表（逗号分隔，targetType=SELECTED 时使用） */
    private String targetDeviceIds;

    /** 下发策略：IMMEDIATE/SCHEDULED */
    private String strategy;

    /** 定时执行时间（strategy=SCHEDULED 时使用） */
    private LocalDateTime scheduledTime;

    /** 任务状态：PENDING/RUNNING/COMPLETED/CANCELLED */
    private String status;

    /** 设备总数 */
    private Integer totalCount;

    /** 成功数 */
    private Integer successCount;

    /** 失败数 */
    private Integer failCount;

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
