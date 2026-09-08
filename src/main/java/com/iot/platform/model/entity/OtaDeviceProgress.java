package com.iot.platform.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * OTA 设备升级进度实体
 * <p>
 * 对应数据库 ota_device_progress 表，记录单台设备在某升级任务中的执行进度。
 * 该表为跨租户的任务执行明细（按 taskId 维度组织），不含 tenant_id 字段，
 * 其 Mapper 通过 {@code @InterceptorIgnore} 关闭多租户过滤。
 *
 * @author iot-platform
 */
@Data
@TableName("ota_device_progress")
public class OtaDeviceProgress implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 升级任务ID */
    private Long taskId;

    /** 设备ID */
    private String deviceId;

    /** 升级状态：PENDING/DOWNLOADING/INSTALLING/SUCCESS/FAILED */
    private String status;

    /** 升级前版本 */
    private String currentVersion;

    /** 目标版本 */
    private String targetVersion;

    /** 失败原因 */
    private String errorMsg;

    /** 升级完成时间 */
    private LocalDateTime upgradeTime;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
