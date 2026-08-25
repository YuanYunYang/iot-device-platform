-- ============================================================
-- IoT 设备接入管控平台 - 数据库初始化脚本
-- 数据库: iot_platform
-- 字符集: utf8mb4
-- ============================================================

CREATE DATABASE IF NOT EXISTS `iot_platform` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `iot_platform`;

-- ==================== 产品表（物模型载体） ====================
DROP TABLE IF EXISTS `product`;
CREATE TABLE `product` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `product_key`     VARCHAR(64)  NOT NULL COMMENT '产品标识（唯一）',
    `product_name`    VARCHAR(128) NOT NULL COMMENT '产品名称',
    `product_type`    VARCHAR(64)  DEFAULT NULL COMMENT '产品类型：sensor/switch/gateway 等',
    `node_type`       VARCHAR(32)  DEFAULT 'DEVICE' COMMENT '节点类型：DEVICE/GATEWAY',
    `net_type`        VARCHAR(32)  DEFAULT 'MQTT' COMMENT '联网方式：MQTT/WIFI/CELLULAR',
    `data_format`     VARCHAR(32)  DEFAULT 'JSON' COMMENT '数据格式：JSON/CUSTOM',
    `description`     VARCHAR(512) DEFAULT NULL COMMENT '产品描述',
    `create_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         TINYINT      DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_product_key` (`product_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品表（定义设备类型）';

-- ==================== 物模型表（产品属性/事件/服务定义） ====================
DROP TABLE IF EXISTS `thing_model`;
CREATE TABLE `thing_model` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `product_id`     BIGINT       NOT NULL COMMENT '关联产品ID',
    `identifier`     VARCHAR(64)  NOT NULL COMMENT '属性标识符，如 temperature',
    `name`           VARCHAR(128) NOT NULL COMMENT '属性名称，如 温度',
    `type`           VARCHAR(32)  NOT NULL DEFAULT 'property' COMMENT '功能类型：property/event/service',
    `data_type`      VARCHAR(32)  DEFAULT 'float' COMMENT '数据类型：int/float/string/bool/enum',
    `unit`           VARCHAR(32)  DEFAULT NULL COMMENT '单位，如 ℃、%',
    `min_value`      DOUBLE       DEFAULT NULL COMMENT '最小值（阈值校验下限）',
    `max_value`      DOUBLE       DEFAULT NULL COMMENT '最大值（阈值告警上限）',
    `alarm_level`    VARCHAR(32)  DEFAULT NULL COMMENT '超阈值告警等级：INFO/WARNING/CRITICAL',
    `description`    VARCHAR(512) DEFAULT NULL COMMENT '属性描述',
    `create_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物模型定义表';

-- ==================== 设备表 ====================
DROP TABLE IF EXISTS `device`;
CREATE TABLE `device` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `device_id`      VARCHAR(64)  NOT NULL COMMENT '设备唯一标识（业务ID）',
    `device_name`    VARCHAR(128) DEFAULT NULL COMMENT '设备名称',
    `product_id`     BIGINT       DEFAULT NULL COMMENT '关联产品ID',
    `product_key`    VARCHAR(64)  DEFAULT NULL COMMENT '冗余产品标识，便于MQTT路由',
    `device_secret`  VARCHAR(128) DEFAULT NULL COMMENT '设备密钥（鉴权用）',
    `status`         VARCHAR(16)  DEFAULT 'UNKNOWN' COMMENT '设备状态：ONLINE/OFFLINE/UNKNOWN',
    `firmware_version` VARCHAR(64) DEFAULT NULL COMMENT '固件版本',
    `ip_address`     VARCHAR(64)  DEFAULT NULL COMMENT '最后连接IP',
    `last_online_time`  DATETIME DEFAULT NULL COMMENT '最后上线时间',
    `last_offline_time` DATETIME DEFAULT NULL COMMENT '最后离线时间',
    `last_heartbeat_time` DATETIME DEFAULT NULL COMMENT '最后心跳时间',
    `location`       VARCHAR(128) DEFAULT NULL COMMENT '设备位置描述',
    `remark`         VARCHAR(256) DEFAULT NULL COMMENT '备注',
    `create_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`        TINYINT      DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_device_id` (`device_id`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备表';

-- ==================== 告警表 ====================
DROP TABLE IF EXISTS `alarm`;
CREATE TABLE `alarm` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `device_id`      VARCHAR(64)  NOT NULL COMMENT '设备ID',
    `product_id`     BIGINT        DEFAULT NULL COMMENT '产品ID',
    `alarm_level`    VARCHAR(32)  NOT NULL DEFAULT 'WARNING' COMMENT '告警等级：INFO/WARNING/CRITICAL',
    `alarm_type`     VARCHAR(64)  DEFAULT NULL COMMENT '告警类型：THRESHOLD/LIFECYCLE/CUSTOM',
    `alarm_title`    VARCHAR(256) DEFAULT NULL COMMENT '告警标题',
    `alarm_content`  TEXT         DEFAULT NULL COMMENT '告警内容',
    `property_identifier` VARCHAR(64) DEFAULT NULL COMMENT '触发告警的属性标识',
    `alarm_value`    DOUBLE       DEFAULT NULL COMMENT '触发告警的数值',
    `status`         VARCHAR(16)  DEFAULT 'ACTIVE' COMMENT '告警状态：ACTIVE/RESOLVED',
    `alarm_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '告警发生时间',
    `resolved_time`  DATETIME     DEFAULT NULL COMMENT '告警解除时间',
    PRIMARY KEY (`id`),
    KEY `idx_device_id` (`device_id`),
    KEY `idx_alarm_level` (`alarm_level`),
    KEY `idx_alarm_time` (`alarm_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警记录表';

-- ============================================================
-- 初始演示数据
-- ============================================================

-- 产品1：温湿度传感器
INSERT INTO `product` (`product_key`, `product_name`, `product_type`, `node_type`, `net_type`, `data_format`, `description`)
VALUES ('temp_hum_sensor', '温湿度传感器', 'sensor', 'DEVICE', 'MQTT', 'JSON', '采集环境温度与湿度，支持阈值告警');

-- 产品2：智能开关
INSERT INTO `product` (`product_key`, `product_name`, `product_type`, `node_type`, `net_type`, `data_format`, `description`)
VALUES ('smart_switch', '智能开关', 'switch', 'DEVICE', 'MQTT', 'JSON', '支持远程开关控制与状态上报');

-- 物模型 - 温湿度传感器属性（含阈值告警配置）
INSERT INTO `thing_model` (`product_id`, `identifier`, `name`, `type`, `data_type`, `unit`, `min_value`, `max_value`, `alarm_level`, `description`)
VALUES
(1, 'temperature', '温度', 'property', 'float', '℃', -20, 40, 'CRITICAL', '环境温度，超过40℃触发严重告警'),
(1, 'humidity', '湿度', 'property', 'float', '%', 0, 100, 'WARNING', '环境湿度，超过100%触发警告'),
(1, 'battery', '电量', 'property', 'int', '%', 0, 100, 'WARNING', '电池电量百分比，低于20%触发告警（此处min作为告警下限）');

-- 物模型 - 智能开关属性
INSERT INTO `thing_model` (`product_id`, `identifier`, `name`, `type`, `data_type`, `unit`, `min_value`, `max_value`, `alarm_level`, `description`)
VALUES
(2, 'switch', '开关状态', 'property', 'bool', NULL, NULL, NULL, NULL, '开关状态：true开/false关'),
(2, 'power', '功率', 'property', 'float', 'W', 0, 5000, 'CRITICAL', '当前功率，超过5000W触发严重告警');

-- 演示设备
INSERT INTO `device` (`device_id`, `device_name`, `product_id`, `product_key`, `device_secret`, `status`, `location`, `remark`)
VALUES
('device_001', '会议室温湿度传感器', 1, 'temp_hum_sensor', 'secret_001', 'UNKNOWN', 'A栋3楼会议室', '演示设备'),
('device_002', '大厅智能开关', 2, 'smart_switch', 'secret_002', 'UNKNOWN', 'A栋1楼大厅', '演示设备');
