-- ============================================================
-- IoT 设备接入管控平台 - SaaS 化数据库初始化脚本
-- 数据库: iot_platform
-- 字符集: utf8mb4
-- 多租户架构: 所有业务表包含 tenant_id 字段
-- ============================================================

CREATE DATABASE IF NOT EXISTS `iot_platform` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `iot_platform`;

-- ==================== 租户表（SaaS 多租户核心） ====================
DROP TABLE IF EXISTS `tenant`;
CREATE TABLE `tenant` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_code`   VARCHAR(64)  NOT NULL COMMENT '租户编码（唯一）',
    `tenant_name`   VARCHAR(128) NOT NULL COMMENT '租户名称',
    `tenant_type`   VARCHAR(32)  DEFAULT 'STANDARD' COMMENT '租户类型：TRIAL/STANDARD/ENTERPRISE',
    `status`        TINYINT      DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    `device_limit`  INT          DEFAULT 1000 COMMENT '设备数量上限',
    `user_limit`    INT          DEFAULT 50 COMMENT '用户数量上限',
    `contact_name`  VARCHAR(64)  DEFAULT NULL COMMENT '联系人',
    `contact_phone` VARCHAR(32)  DEFAULT NULL COMMENT '联系电话',
    `expire_time`   DATETIME     DEFAULT NULL COMMENT '到期时间',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       TINYINT      DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_code` (`tenant_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户表';

-- ==================== 产品表 ====================
DROP TABLE IF EXISTS `product`;
CREATE TABLE `product` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id`       BIGINT       NOT NULL DEFAULT 0 COMMENT '租户ID',
    `product_key`     VARCHAR(64)  NOT NULL COMMENT '产品标识',
    `product_name`    VARCHAR(128) NOT NULL COMMENT '产品名称',
    `product_type`    VARCHAR(64)  DEFAULT NULL COMMENT '产品类型',
    `node_type`       VARCHAR(32)  DEFAULT 'DEVICE' COMMENT '节点类型',
    `net_type`        VARCHAR(32)  DEFAULT 'MQTT' COMMENT '联网方式',
    `data_format`     VARCHAR(32)  DEFAULT 'JSON' COMMENT '数据格式',
    `description`     VARCHAR(512) DEFAULT NULL COMMENT '产品描述',
    `create_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         TINYINT      DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_product_key` (`tenant_id`, `product_key`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品表';

-- ==================== 物模型表 ====================
DROP TABLE IF EXISTS `thing_model`;
CREATE TABLE `thing_model` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id`      BIGINT       NOT NULL DEFAULT 0 COMMENT '租户ID',
    `product_id`     BIGINT       NOT NULL COMMENT '关联产品ID',
    `identifier`     VARCHAR(64)  NOT NULL COMMENT '属性标识符',
    `name`           VARCHAR(128) NOT NULL COMMENT '属性名称',
    `type`           VARCHAR(32)  NOT NULL DEFAULT 'property' COMMENT '功能类型',
    `data_type`      VARCHAR(32)  DEFAULT 'float' COMMENT '数据类型',
    `unit`           VARCHAR(32)  DEFAULT NULL COMMENT '单位',
    `min_value`      DOUBLE       DEFAULT NULL COMMENT '最小值',
    `max_value`      DOUBLE       DEFAULT NULL COMMENT '最大值',
    `alarm_level`    VARCHAR(32)  DEFAULT NULL COMMENT '告警等级',
    `description`    VARCHAR(512) DEFAULT NULL COMMENT '描述',
    `create_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_product` (`tenant_id`, `product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物模型定义表';

-- ==================== 设备表（大设备量优化索引） ====================
DROP TABLE IF EXISTS `device`;
CREATE TABLE `device` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id`      BIGINT       NOT NULL DEFAULT 0 COMMENT '租户ID',
    `device_id`      VARCHAR(64)  NOT NULL COMMENT '设备唯一标识',
    `device_name`    VARCHAR(128) DEFAULT NULL COMMENT '设备名称',
    `product_id`     BIGINT       DEFAULT NULL COMMENT '关联产品ID',
    `product_key`    VARCHAR(64)  DEFAULT NULL COMMENT '冗余产品标识',
    `device_secret`  VARCHAR(128) DEFAULT NULL COMMENT '设备密钥',
    `status`         VARCHAR(16)  DEFAULT 'UNKNOWN' COMMENT '设备状态',
    `firmware_version` VARCHAR(64) DEFAULT NULL COMMENT '固件版本',
    `ip_address`     VARCHAR(64)  DEFAULT NULL COMMENT '最后连接IP',
    `last_online_time`  DATETIME DEFAULT NULL COMMENT '最后上线时间',
    `last_offline_time` DATETIME DEFAULT NULL COMMENT '最后离线时间',
    `last_heartbeat_time` DATETIME DEFAULT NULL COMMENT '最后心跳时间',
    `location`       VARCHAR(128) DEFAULT NULL COMMENT '设备位置',
    `remark`         VARCHAR(256) DEFAULT NULL COMMENT '备注',
    `create_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`        TINYINT      DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_device_id` (`tenant_id`, `device_id`),
    KEY `idx_tenant_status` (`tenant_id`, `status`),
    KEY `idx_tenant_product` (`tenant_id`, `product_id`),
    KEY `idx_heartbeat` (`last_heartbeat_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备表';

-- ==================== 告警表 ====================
DROP TABLE IF EXISTS `alarm`;
CREATE TABLE `alarm` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id`      BIGINT       NOT NULL DEFAULT 0 COMMENT '租户ID',
    `device_id`      VARCHAR(64)  NOT NULL COMMENT '设备ID',
    `product_id`     BIGINT        DEFAULT NULL COMMENT '产品ID',
    `alarm_level`    VARCHAR(32)  NOT NULL DEFAULT 'WARNING' COMMENT '告警等级',
    `alarm_type`     VARCHAR(64)  DEFAULT NULL COMMENT '告警类型',
    `alarm_title`    VARCHAR(256) DEFAULT NULL COMMENT '告警标题',
    `alarm_content`  TEXT         DEFAULT NULL COMMENT '告警内容',
    `property_identifier` VARCHAR(64) DEFAULT NULL COMMENT '属性标识',
    `alarm_value`    DOUBLE       DEFAULT NULL COMMENT '触发数值',
    `status`         VARCHAR(16)  DEFAULT 'ACTIVE' COMMENT '告警状态',
    `alarm_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '告警时间',
    `resolved_time`  DATETIME     DEFAULT NULL COMMENT '解除时间',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_device` (`tenant_id`, `device_id`),
    KEY `idx_tenant_level` (`tenant_id`, `alarm_level`),
    KEY `idx_tenant_time` (`tenant_id`, `alarm_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警记录表';

-- ==================== 系统用户表 ====================
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id`   BIGINT       NOT NULL DEFAULT 0 COMMENT '租户ID（超管为0）',
    `username`    VARCHAR(64)  NOT NULL COMMENT '用户名',
    `password`    VARCHAR(128) NOT NULL COMMENT '密码（BCrypt加密）',
    `role`        VARCHAR(32)  NOT NULL DEFAULT 'USER' COMMENT '角色',
    `status`      TINYINT      DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT      DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_username` (`tenant_id`, `username`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- ============================================================
-- 初始数据
-- ============================================================

-- 默认租户（ID=1）
INSERT INTO `tenant` (`id`, `tenant_code`, `tenant_name`, `tenant_type`, `device_limit`, `user_limit`, `contact_name`, `contact_phone`, `expire_time`)
VALUES (1, 'default', '默认租户', 'ENTERPRISE', 100000, 200, '管理员', '13800000000', '2027-12-31 23:59:59');

-- 默认超级管理员（tenant_id=0 表示跨租户超管，用户名: admin，密码: admin123）
INSERT INTO `sys_user` (`tenant_id`, `username`, `password`, `role`, `status`)
VALUES (0, 'admin', '$2a$10$N.ZOn9G6/YLFixAgoMn6SOaGMbWVn0r3Q8J8zv3T3T3oEMrAfK8m', 'SUPER_ADMIN', 1);

-- 默认租户管理员（tenant_id=1）
INSERT INTO `sys_user` (`tenant_id`, `username`, `password`, `role`, `status`)
VALUES (1, 'tenant_admin', '$2a$10$N.ZOn9G6/YLFixAgoMn6SOaGMbWVn0r3Q8J8zv3T3T3oEMrAfK8m', 'SYSTEM_ADMIN', 1);

-- 演示产品（tenant_id=1）
INSERT INTO `product` (`tenant_id`, `product_key`, `product_name`, `product_type`, `node_type`, `net_type`, `data_format`, `description`)
VALUES
(1, 'temp_hum_sensor', '温湿度传感器', 'sensor', 'DEVICE', 'MQTT', 'JSON', '采集环境温度与湿度'),
(1, 'smart_switch', '智能开关', 'switch', 'DEVICE', 'MQTT', 'JSON', '支持远程开关控制');

-- 物模型（tenant_id=1）
INSERT INTO `thing_model` (`tenant_id`, `product_id`, `identifier`, `name`, `type`, `data_type`, `unit`, `min_value`, `max_value`, `alarm_level`, `description`)
VALUES
(1, 1, 'temperature', '温度', 'property', 'float', '℃', -20, 40, 'CRITICAL', '环境温度'),
(1, 1, 'humidity', '湿度', 'property', 'float', '%', 0, 100, 'WARNING', '环境湿度'),
(1, 1, 'battery', '电量', 'property', 'int', '%', 0, 100, 'WARNING', '电池电量'),
(1, 2, 'switch', '开关状态', 'property', 'bool', NULL, NULL, NULL, NULL, '开关状态'),
(1, 2, 'power', '功率', 'property', 'float', 'W', 0, 5000, 'CRITICAL', '当前功率');

-- 演示设备（tenant_id=1）
INSERT INTO `device` (`tenant_id`, `device_id`, `device_name`, `product_id`, `product_key`, `device_secret`, `status`, `location`, `remark`)
VALUES
(1, 'device_001', '会议室温湿度传感器', 1, 'temp_hum_sensor', 'secret_001', 'UNKNOWN', 'A栋3楼会议室', '演示设备'),
(1, 'device_002', '大厅智能开关', 2, 'smart_switch', 'secret_002', 'UNKNOWN', 'A栋1楼大厅', '演示设备');

-- ============================================================
-- 扩展模块表（OTA 固件升级 / Drools 告警规则引擎）
-- ============================================================

-- ==================== 固件包表 ====================
DROP TABLE IF EXISTS `firmware`;
CREATE TABLE `firmware` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `tenant_id`     BIGINT       NOT NULL DEFAULT 0,
    `product_id`    BIGINT       DEFAULT NULL,
    `version`       VARCHAR(64)  NOT NULL,
    `file_url`      VARCHAR(512) NOT NULL,
    `file_size`     BIGINT       DEFAULT 0,
    `checksum_md5`  VARCHAR(64)  DEFAULT NULL,
    `checksum_sha256` VARCHAR(128) DEFAULT NULL,
    `status`        VARCHAR(16)  DEFAULT 'DRAFT',
    `description`   VARCHAR(512) DEFAULT NULL,
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT      DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_tenant_product` (`tenant_id`, `product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='固件包表';

-- ==================== OTA升级任务表 ====================
DROP TABLE IF EXISTS `ota_task`;
CREATE TABLE `ota_task` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT,
    `tenant_id`         BIGINT       NOT NULL DEFAULT 0,
    `firmware_id`       BIGINT       NOT NULL,
    `task_name`         VARCHAR(128) NOT NULL,
    `target_type`       VARCHAR(16)  DEFAULT 'ALL',
    `target_product_key` VARCHAR(64) DEFAULT NULL,
    `target_device_ids` TEXT         DEFAULT NULL,
    `strategy`          VARCHAR(16)  DEFAULT 'IMMEDIATE',
    `scheduled_time`    DATETIME     DEFAULT NULL,
    `status`            VARCHAR(16)  DEFAULT 'PENDING',
    `total_count`       INT          DEFAULT 0,
    `success_count`     INT          DEFAULT 0,
    `fail_count`        INT          DEFAULT 0,
    `create_time`       DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `update_time`       DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`           TINYINT      DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_tenant_firmware` (`tenant_id`, `firmware_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OTA升级任务表';

-- ==================== OTA设备升级进度表 ====================
DROP TABLE IF EXISTS `ota_device_progress`;
CREATE TABLE `ota_device_progress` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `task_id`         BIGINT       NOT NULL,
    `device_id`       VARCHAR(64)  NOT NULL,
    `status`          VARCHAR(16)  DEFAULT 'PENDING',
    `current_version` VARCHAR(64) DEFAULT NULL,
    `target_version`  VARCHAR(64) DEFAULT NULL,
    `error_msg`       VARCHAR(512) DEFAULT NULL,
    `upgrade_time`    DATETIME     DEFAULT NULL,
    `create_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_task_device` (`task_id`, `device_id`),
    KEY `idx_device_id` (`device_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OTA设备升级进度表';

-- ==================== 告警规则表 ====================
DROP TABLE IF EXISTS `alarm_rule`;
CREATE TABLE `alarm_rule` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `tenant_id`   BIGINT       NOT NULL DEFAULT 0,
    `rule_name`   VARCHAR(128) NOT NULL,
    `rule_type`   VARCHAR(32)  DEFAULT 'THRESHOLD',
    `drl_content` TEXT         NOT NULL,
    `enabled`     TINYINT      DEFAULT 1,
    `description` VARCHAR(512) DEFAULT NULL,
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT      DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警规则表';

-- ============================================================
-- 能源管理模块表（能耗采集 / 碳排放追踪）
-- ============================================================

-- ==================== 能源表 ====================
DROP TABLE IF EXISTS `energy_meter`;
CREATE TABLE `energy_meter` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id`     BIGINT       NOT NULL DEFAULT 0 COMMENT '租户ID',
    `meter_code`    VARCHAR(64)  NOT NULL COMMENT '能源表编号（唯一）',
    `meter_name`    VARCHAR(128) DEFAULT NULL COMMENT '能源表名称',
    `meter_type`    VARCHAR(32)  NOT NULL DEFAULT 'ELECTRICITY' COMMENT '能源表类型：ELECTRICITY/WATER/GAS',
    `protocol`      VARCHAR(16)  DEFAULT 'MQTT' COMMENT '通信协议：MODBUS/MQTT',
    `location`      VARCHAR(128) DEFAULT NULL COMMENT '安装位置',
    `product_id`    BIGINT       DEFAULT NULL COMMENT '关联产品ID',
    `device_id`     VARCHAR(64)  DEFAULT NULL COMMENT '关联设备ID',
    `modbus_addr`   INT          DEFAULT NULL COMMENT 'Modbus通信地址',
    `modbus_port`   INT          DEFAULT NULL COMMENT 'Modbus通信端口',
    `ct_ratio`      DOUBLE       DEFAULT NULL COMMENT '电流互感器变比',
    `pt_ratio`      DOUBLE       DEFAULT NULL COMMENT '电压互感器变比',
    `rated_power`   DOUBLE       DEFAULT NULL COMMENT '额定容量（kW）',
    `status`        TINYINT      DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    `remark`        VARCHAR(256) DEFAULT NULL COMMENT '备注',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       TINYINT      DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_meter_code` (`tenant_id`, `meter_code`),
    KEY `idx_tenant_type` (`tenant_id`, `meter_type`),
    KEY `idx_device_id` (`device_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='能源表';

-- ==================== 能源读数表（时序数据，InfluxDB 的 MySQL 备份） ====================
DROP TABLE IF EXISTS `energy_reading`;
CREATE TABLE `energy_reading` (
    `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id`           BIGINT       NOT NULL DEFAULT 0 COMMENT '租户ID',
    `meter_id`            BIGINT       NOT NULL COMMENT '能源表ID',
    `meter_code`          VARCHAR(64)  NOT NULL COMMENT '能源表编号',
    `meter_type`          VARCHAR(32)  DEFAULT 'ELECTRICITY' COMMENT '能源表类型',
    `reading_time`        BIGINT       NOT NULL COMMENT '采集时间戳（epoch毫秒）',
    `voltage`             DOUBLE       DEFAULT NULL COMMENT '电压（V）',
    `current`             DOUBLE       DEFAULT NULL COMMENT '电流（A）',
    `active_power`        DOUBLE       DEFAULT NULL COMMENT '有功功率（kW）',
    `reactive_power`      DOUBLE       DEFAULT NULL COMMENT '无功功率（kVar）',
    `power_factor`        DOUBLE       DEFAULT NULL COMMENT '功率因数',
    `frequency`           DOUBLE       DEFAULT NULL COMMENT '频率（Hz）',
    `energy_consumption`  DOUBLE       DEFAULT NULL COMMENT '本周期用电量（kWh）',
    `cumulative_energy`   DOUBLE       DEFAULT NULL COMMENT '累计用电量（kWh）',
    `peak_flag`           TINYINT      DEFAULT 0 COMMENT '峰谷标志：0-平 1-峰 2-谷',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_meter_time` (`tenant_id`, `meter_code`, `reading_time`),
    KEY `idx_meter_id` (`meter_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='能源读数表';

-- ==================== 排放因子表 ====================
DROP TABLE IF EXISTS `emission_factor`;
CREATE TABLE `emission_factor` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id`     BIGINT       NOT NULL DEFAULT 0 COMMENT '租户ID',
    `source_type`   VARCHAR(32)  NOT NULL COMMENT '排放源类型：ELECTRICITY/NATURAL_GAS/DIESEL/COAL',
    `source_name`   VARCHAR(128) DEFAULT NULL COMMENT '排放源名称',
    `factor_value`  DOUBLE       NOT NULL COMMENT '排放因子值',
    `unit`          VARCHAR(64)  DEFAULT NULL COMMENT '因子单位，如 kgCO2/kWh',
    `description`   VARCHAR(512) DEFAULT NULL COMMENT '描述',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_source` (`tenant_id`, `source_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='排放因子表';

-- ==================== 碳排放记录表 ====================
DROP TABLE IF EXISTS `carbon_emission`;
CREATE TABLE `carbon_emission` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id`         BIGINT       NOT NULL DEFAULT 0 COMMENT '租户ID',
    `scope`             VARCHAR(16)  NOT NULL DEFAULT 'SCOPE_2' COMMENT '排放范围：SCOPE_1/SCOPE_2',
    `source_type`       VARCHAR(32)  NOT NULL COMMENT '排放源类型：ELECTRICITY/NATURAL_GAS/DIESEL/COAL',
    `source_name`       VARCHAR(128) DEFAULT NULL COMMENT '排放源名称',
    `consumption`       DOUBLE       NOT NULL COMMENT '消耗量',
    `consumption_unit`  VARCHAR(32)  DEFAULT NULL COMMENT '消耗量单位',
    `emission_factor`   DOUBLE       NOT NULL COMMENT '使用的排放因子',
    `co2_emission`      DOUBLE       NOT NULL COMMENT '二氧化碳排放量（kgCO2）',
    `calculation_date`  DATE         NOT NULL COMMENT '计算日期',
    `create_time`       DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_scope_date` (`tenant_id`, `scope`, `calculation_date`),
    KEY `idx_tenant_source` (`tenant_id`, `source_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='碳排放记录表';

-- ==================== 能源管理模块初始数据 ====================

-- 默认排放因子（tenant_id=1）
INSERT INTO `emission_factor` (`tenant_id`, `source_type`, `factor_value`, `unit`) VALUES
(1, 'ELECTRICITY', 0.5810, 'kgCO2/kWh'),
(1, 'NATURAL_GAS', 2.1622, 'kgCO2/m³'),
(1, 'DIESEL', 2.7301, 'kgCO2/L'),
(1, 'COAL', 1.9770, 'kgCO2/kg');

-- 演示能源表（tenant_id=1）
INSERT INTO `energy_meter` (`tenant_id`, `meter_code`, `meter_name`, `meter_type`, `protocol`, `location`, `device_id`, `ct_ratio`, `pt_ratio`, `status`) VALUES
(1, 'EM_001', '1号变压器总表', 'ELECTRICITY', 'MQTT', 'A栋配电室', 'device_001', 150.0, 1.0, 1),
(1, 'EM_002', 'B栋照明电表', 'ELECTRICITY', 'MODBUS', 'B栋1楼', NULL, 100.0, 1.0, 1),
(1, 'WM_001', '厂区总水表', 'WATER', 'MODBUS', '厂区入口', NULL, NULL, NULL, 1);

-- ============================================================
-- 能耗分析报表（能源管理模块扩展表）
-- 注：energy_meter、energy_reading、emission_factor、carbon_emission 表
-- 已在上方「能源管理模块表」中定义，此处不再重复创建。
-- ============================================================

-- ==================== 能耗报表表 ====================
DROP TABLE IF EXISTS `energy_report`;
CREATE TABLE `energy_report` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id`       BIGINT       NOT NULL DEFAULT 0 COMMENT '租户ID',
    `meter_id`        BIGINT       NOT NULL COMMENT '仪表ID',
    `meter_code`      VARCHAR(64)  DEFAULT NULL COMMENT '仪表编号（冗余）',
    `report_type`     VARCHAR(16)  NOT NULL DEFAULT 'DAILY' COMMENT '报表类型：DAILY/WEEKLY/MONTHLY',
    `report_date`     VARCHAR(10)  NOT NULL COMMENT '报表日期（日: yyyy-MM-dd / 月: yyyy-MM）',
    `total_energy`    DOUBLE       DEFAULT 0 COMMENT '总能耗（kWh）',
    `peak_energy`     DOUBLE       DEFAULT 0 COMMENT '尖峰能耗（kWh，8-11h/18-21h）',
    `valley_energy`   DOUBLE       DEFAULT 0 COMMENT '谷时段能耗（kWh，0-7h）',
    `flat_energy`     DOUBLE       DEFAULT 0 COMMENT '平时段能耗（kWh）',
    `max_demand`      DOUBLE       DEFAULT 0 COMMENT '最大需量（kW）',
    `avg_power_factor` DOUBLE      DEFAULT 0 COMMENT '平均功率因数',
    `yoy_ratio`       DOUBLE       DEFAULT NULL COMMENT '同比（%）',
    `mom_ratio`       DOUBLE       DEFAULT NULL COMMENT '环比（%）',
    `cost`            DOUBLE       DEFAULT 0 COMMENT '电费（元）',
    `create_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_meter` (`tenant_id`, `meter_id`),
    KEY `idx_tenant_type_date` (`tenant_id`, `report_type`, `report_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='能耗分析报表表';

-- 注：carbon_emission 与 emission_factor 表已在上方「能源管理模块表」中定义。

-- ============================================================
-- P1 能源管理模块表（负荷调度 / 碳排放 / 能耗报表）
-- ============================================================

-- ==================== 负荷调度策略表 ====================
DROP TABLE IF EXISTS `load_dispatch_strategy`;
CREATE TABLE `load_dispatch_strategy` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id`         BIGINT       NOT NULL DEFAULT 0 COMMENT '租户ID',
    `strategy_name`     VARCHAR(128) NOT NULL COMMENT '策略名称',
    `strategy_type`     VARCHAR(32)  NOT NULL DEFAULT 'PEAK_SHAVING' COMMENT '策略类型：PEAK_SHAVING(削峰)/VALLEY_FILLING(填谷)/DEMAND_RESPONSE(需求响应)',
    `target_device_id`  VARCHAR(64)  NOT NULL COMMENT '目标设备ID',
    `action_type`       VARCHAR(32)  NOT NULL DEFAULT 'DEVICE_SHUTDOWN' COMMENT '动作类型：AC_TEMP_ADJUST/EV_CHARGE_LIMIT/LIGHT_DIM/DEVICE_SHUTDOWN',
    `action_params`     TEXT         DEFAULT NULL COMMENT '动作参数（JSON），如 {"temp":26} 或 {"powerLimit":30}',
    `trigger_condition` TEXT         DEFAULT NULL COMMENT '触发条件（JSON），如 {"activePower":">800","peakFlag":"==1"}',
    `enabled`           TINYINT      DEFAULT 1 COMMENT '是否启用：0-禁用 1-启用',
    `priority`          INT          DEFAULT 0 COMMENT '优先级（数值越大优先级越高）',
    `last_execute_time` DATETIME    DEFAULT NULL COMMENT '最后执行时间',
    `create_time`       DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_enabled` (`tenant_id`, `enabled`),
    KEY `idx_tenant_type` (`tenant_id`, `strategy_type`),
    KEY `idx_tenant_device` (`tenant_id`, `target_device_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='负荷调度策略表';

-- 注：carbon_emission 与 energy_report 表结构已在上方能源管理模块中定义，
-- 此处不再重复创建，确保实体类与表结构一致。
