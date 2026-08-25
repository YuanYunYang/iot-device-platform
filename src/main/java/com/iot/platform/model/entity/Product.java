package com.iot.platform.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 产品实体
 * <p>
 * 对应数据库 product 表，定义设备类型（如"温湿度传感器"、"智能开关"），
 * 是物模型（ThingModel）的载体。设备注册时关联产品并继承物模型定义。
 *
 * @author iot-platform
 */
@Data
@TableName("product")
public class Product implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 产品标识（唯一） */
    private String productKey;

    /** 产品名称 */
    private String productName;

    /** 产品类型：sensor/switch/gateway 等 */
    private String productType;

    /** 节点类型：DEVICE/GATEWAY */
    private String nodeType;

    /** 联网方式：MQTT/WIFI/CELLULAR */
    private String netType;

    /** 数据格式：JSON/CUSTOM */
    private String dataFormat;

    /** 产品描述 */
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
