package com.iot.platform.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 监控数据 VO
 * <p>
 * 返回给前端（或通过 WebSocket 推送）的设备实时监控数据，
 * 数据来源为 InfluxDB 时序存储。
 *
 * @author iot-platform
 */
@Data
@Schema(description = "设备监控数据视图")
public class MonitorDataVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "设备ID")
    private String deviceId;

    @Schema(description = "属性标识，如 temperature")
    private String identifier;

    @Schema(description = "属性名称，如 温度")
    private String name;

    @Schema(description = "属性值")
    private Object value;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "数据上报时间")
    private LocalDateTime time;

    @Schema(description = "完整属性快照（一次上报的多个属性）")
    private Map<String, Object> properties;

    @Schema(description = "数据来源：realtime/history")
    private String source;
}
