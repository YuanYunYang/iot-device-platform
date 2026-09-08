package com.iot.platform.model.vo;

import com.iot.platform.model.entity.OtaDeviceProgress;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * OTA 升级任务视图（含进度统计）
 *
 * @author iot-platform
 */
@Data
public class OtaTaskVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 任务ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 关联固件ID */
    private Long firmwareId;

    /** 固件目标版本 */
    private String targetVersion;

    /** 任务名称 */
    private String taskName;

    /** 目标范围：ALL/SELECTED/BY_PRODUCT */
    private String targetType;

    /** 目标产品标识 */
    private String targetProductKey;

    /** 目标设备ID列表（逗号分隔） */
    private String targetDeviceIds;

    /** 下发策略：IMMEDIATE/SCHEDULED */
    private String strategy;

    /** 定时执行时间 */
    private LocalDateTime scheduledTime;

    /** 任务状态：PENDING/RUNNING/COMPLETED/CANCELLED */
    private String status;

    /** 设备总数 */
    private Integer totalCount;

    /** 成功数 */
    private Integer successCount;

    /** 失败数 */
    private Integer failCount;

    /** 待升级数（PENDING+DOWNLOADING+INSTALLING） */
    private Integer pendingCount;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 设备进度明细（详情接口返回） */
    private List<OtaDeviceProgress> progressList;
}
