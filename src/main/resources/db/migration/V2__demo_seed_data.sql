-- ============================================================
-- V2 客户演示种子数据（丰富可观数据）
-- 幂等：使用 INSERT IGNORE / 条件插入，可重复执行于已有环境库
-- ============================================================

-- ---------- 补充产品 ----------
INSERT IGNORE INTO `product` (`id`,`tenant_id`,`product_key`,`product_name`,`product_type`,`node_type`,`net_type`,`data_format`,`description`)
VALUES
(3,1,'energy_gateway','能源网关','gateway','GATEWAY','MQTT','JSON','厂区能耗采集网关'),
(4,1,'smart_ac','智能空调','hvac','DEVICE','MQTT','JSON','支持远程调温与模式切换'),
(5,1,'ev_charger','充电桩','charger','DEVICE','MQTT','JSON','支持功率限制与启停');

INSERT IGNORE INTO `thing_model` (`tenant_id`,`product_id`,`identifier`,`name`,`type`,`data_type`,`unit`,`min_value`,`max_value`,`alarm_level`,`description`)
VALUES
(1,3,'active_power','有功功率','property','float','kW',0,2000,'CRITICAL','总有功功率'),
(1,3,'energy','累计电量','property','float','kWh',0,NULL,NULL,'累计用电'),
(1,4,'temperature','设定温度','property','float','℃',16,30,NULL,'目标温度'),
(1,4,'room_temp','室内温度','property','float','℃',0,50,'WARNING','室内温度'),
(1,4,'mode','运行模式','property','string',NULL,NULL,NULL,NULL,'cool/heat/fan'),
(1,5,'charge_power','充电功率','property','float','kW',0,120,'WARNING','当前充电功率'),
(1,5,'soc','电池SOC','property','float','%',0,100,NULL,'车辆电量');

-- ---------- 更新/补充设备（含在线状态） ----------
UPDATE `device` SET `status`='ONLINE', `firmware_version`='1.2.0',
  `last_online_time`=NOW(), `last_heartbeat_time`=NOW(), `ip_address`='10.0.1.11'
WHERE `device_id`='device_001' AND `tenant_id`=1;
UPDATE `device` SET `status`='ONLINE', `firmware_version`='2.0.1',
  `last_online_time`=NOW(), `last_heartbeat_time`=NOW(), `ip_address`='10.0.1.12'
WHERE `device_id`='device_002' AND `tenant_id`=1;

INSERT INTO `device` (`tenant_id`,`device_id`,`device_name`,`product_id`,`product_key`,`device_secret`,`status`,`firmware_version`,`ip_address`,`last_online_time`,`last_heartbeat_time`,`location`,`remark`)
SELECT * FROM (
  SELECT 1 AS t,'device_003','A栋配电室能源网关',3,'energy_gateway','secret_003','ONLINE','1.0.5','10.0.1.21',NOW(),NOW(),'A栋配电室','演示-主站' UNION ALL
  SELECT 1,'device_004','B栋配电室能源网关',3,'energy_gateway','secret_004','ONLINE','1.0.5','10.0.1.22',NOW(),NOW(),'B栋配电室','演示' UNION ALL
  SELECT 1,'device_005','研发中心空调-01',4,'smart_ac','secret_005','ONLINE','3.1.0','10.0.2.31',NOW(),NOW(),'研发中心3F','演示' UNION ALL
  SELECT 1,'device_006','研发中心空调-02',4,'smart_ac','secret_006','OFFLINE','3.0.2','10.0.2.32',DATE_SUB(NOW(), INTERVAL 2 HOUR),DATE_SUB(NOW(), INTERVAL 2 HOUR),'研发中心3F','演示-离线' UNION ALL
  SELECT 1,'device_007','地下车库充电桩-A1',5,'ev_charger','secret_007','ONLINE','1.4.0','10.0.3.41',NOW(),NOW(),'地下车库A区','演示' UNION ALL
  SELECT 1,'device_008','地下车库充电桩-A2',5,'ev_charger','secret_008','ONLINE','1.4.0','10.0.3.42',NOW(),NOW(),'地下车库A区','演示' UNION ALL
  SELECT 1,'device_009','展厅温湿度传感器',1,'temp_hum_sensor','secret_009','ONLINE','1.2.0','10.0.1.19',NOW(),NOW(),'一楼展厅','演示' UNION ALL
  SELECT 1,'device_010','机房温湿度传感器',1,'temp_hum_sensor','secret_010','ONLINE','1.1.8','10.0.1.18',NOW(),NOW(),'机房','演示' UNION ALL
  SELECT 1,'device_011','仓储区智能开关',2,'smart_switch','secret_011','OFFLINE','2.0.0','10.0.1.50',DATE_SUB(NOW(), INTERVAL 1 DAY),DATE_SUB(NOW(), INTERVAL 1 DAY),'仓储区','演示-离线' UNION ALL
  SELECT 1,'device_012','访客区智能开关',2,'smart_switch','secret_012','ONLINE','2.0.1','10.0.1.51',NOW(),NOW(),'访客区','演示'
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM `device` d WHERE d.device_id=tmp.device_id AND d.tenant_id=1);

-- ---------- 告警 ----------
DELETE FROM `alarm` WHERE `tenant_id`=1 AND `alarm_title` LIKE '【演示】%';
INSERT INTO `alarm` (`tenant_id`,`device_id`,`product_id`,`alarm_level`,`alarm_type`,`alarm_title`,`alarm_content`,`property_identifier`,`alarm_value`,`status`,`alarm_time`,`resolved_time`) VALUES
(1,'device_001',1,'CRITICAL','THRESHOLD','【演示】会议室温湿度过高','温度连续5分钟超过阈值','temperature',42.5,'ACTIVE','2026-09-28 09:48:00',NULL),
(1,'device_010',1,'CRITICAL','THRESHOLD','【演示】机房温度告警','机房温度达到 38.2℃','temperature',38.2,'ACTIVE','2026-09-28 09:00:00',NULL),
(1,'device_006',4,'WARNING','LIFECYCLE','【演示】空调设备离线','超过90秒未收到心跳',NULL,NULL,'ACTIVE','2026-09-28 08:00:00',NULL),
(1,'device_011',2,'WARNING','LIFECYCLE','【演示】仓储开关离线','设备离线超过24小时',NULL,NULL,'ACTIVE','2026-09-27 14:00:00',NULL),
(1,'device_007',5,'WARNING','THRESHOLD','【演示】充电功率越限','充电功率超过设定上限','charge_power',95.0,'RESOLVED','2026-09-28 05:00:00','2026-09-28 06:00:00'),
(1,'device_003',3,'CRITICAL','THRESHOLD','【演示】配电室功率突增','有功功率突增至 860kW','active_power',860.0,'RESOLVED','2026-09-28 02:00:00','2026-09-28 03:00:00'),
(1,'device_009',1,'INFO','THRESHOLD','【演示】展厅湿度偏低','湿度低于舒适区间','humidity',28.0,'RESOLVED','2026-09-27 22:00:00','2026-09-28 00:00:00'),
(1,'device_002',2,'WARNING','THRESHOLD','【演示】大厅开关功率异常','功率读数异常偏高','power',3200.0,'RESOLVED','2026-09-27 04:00:00','2026-09-27 06:00:00'),
(1,'device_005',4,'INFO','CUSTOM','【演示】空调滤网清洁提醒','累计运行达到清洁周期',NULL,NULL,'ACTIVE','2026-09-28 07:00:00',NULL),
(1,'device_008',5,'INFO','THRESHOLD','【演示】充电桩通信质量下降','信号质量低于阈值',NULL,NULL,'RESOLVED','2026-09-27 19:00:00','2026-09-27 20:00:00'),
(1,'device_004',3,'WARNING','THRESHOLD','【演示】B栋功率因数偏低','功率因数低于0.85','power_factor',0.78,'ACTIVE','2026-09-28 09:30:00',NULL),
(1,'device_001',1,'WARNING','THRESHOLD','【演示】传感器电量偏低','电池电量剩余 15%','battery',15.0,'RESOLVED','2026-09-26 10:00:00','2026-09-26 18:00:00');

-- ---------- 固件 / OTA ----------
INSERT INTO `firmware` (`tenant_id`,`product_id`,`version`,`file_url`,`file_size`,`checksum_md5`,`checksum_sha256`,`status`,`description`)
SELECT 1,1,'1.3.0','https://cdn.example.com/fw/temp_hum_1.3.0.bin',1048576,'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa','bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb','PUBLISHED','温湿度传感器稳定版'
WHERE NOT EXISTS (SELECT 1 FROM firmware WHERE tenant_id=1 AND product_id=1 AND version='1.3.0');
INSERT INTO `firmware` (`tenant_id`,`product_id`,`version`,`file_url`,`file_size`,`checksum_md5`,`status`,`description`)
SELECT 1,2,'2.1.0','https://cdn.example.com/fw/switch_2.1.0.bin',524288,'cccccccccccccccccccccccccccccccc','PUBLISHED','智能开关功能增强'
WHERE NOT EXISTS (SELECT 1 FROM firmware WHERE tenant_id=1 AND product_id=2 AND version='2.1.0');
INSERT INTO `firmware` (`tenant_id`,`product_id`,`version`,`file_url`,`file_size`,`checksum_md5`,`status`,`description`)
SELECT 1,4,'3.2.0','https://cdn.example.com/fw/ac_3.2.0.bin',2097152,'dddddddddddddddddddddddddddddddd','DRAFT','空调固件草稿'
WHERE NOT EXISTS (SELECT 1 FROM firmware WHERE tenant_id=1 AND product_id=4 AND version='3.2.0');

INSERT INTO `ota_task` (`tenant_id`,`firmware_id`,`task_name`,`target_type`,`target_product_key`,`strategy`,`status`,`total_count`,`success_count`,`fail_count`)
SELECT 1, f.id, '【演示】温湿度传感器批量升级', 'PRODUCT', 'temp_hum_sensor', 'IMMEDIATE', 'RUNNING', 3, 2, 0
FROM firmware f WHERE f.tenant_id=1 AND f.product_id=1 AND f.version='1.3.0'
AND NOT EXISTS (SELECT 1 FROM ota_task WHERE task_name='【演示】温湿度传感器批量升级');

INSERT INTO `ota_task` (`tenant_id`,`firmware_id`,`task_name`,`target_type`,`strategy`,`status`,`total_count`,`success_count`,`fail_count`)
SELECT 1, f.id, '【演示】智能开关全量升级', 'ALL', 'IMMEDIATE', 'COMPLETED', 2, 2, 0
FROM firmware f WHERE f.tenant_id=1 AND f.product_id=2 AND f.version='2.1.0'
AND NOT EXISTS (SELECT 1 FROM ota_task WHERE task_name='【演示】智能开关全量升级');

INSERT INTO `ota_device_progress` (`task_id`,`device_id`,`status`,`current_version`,`target_version`,`upgrade_time`)
SELECT t.id, 'device_001', 'SUCCESS', '1.2.0', '1.3.0', NOW()
FROM ota_task t WHERE t.task_name='【演示】温湿度传感器批量升级'
AND NOT EXISTS (SELECT 1 FROM ota_device_progress p WHERE p.task_id=t.id AND p.device_id='device_001');
INSERT INTO `ota_device_progress` (`task_id`,`device_id`,`status`,`current_version`,`target_version`,`upgrade_time`)
SELECT t.id, 'device_009', 'SUCCESS', '1.2.0', '1.3.0', NOW()
FROM ota_task t WHERE t.task_name='【演示】温湿度传感器批量升级'
AND NOT EXISTS (SELECT 1 FROM ota_device_progress p WHERE p.task_id=t.id AND p.device_id='device_009');
INSERT INTO `ota_device_progress` (`task_id`,`device_id`,`status`,`current_version`,`target_version`)
SELECT t.id, 'device_010', 'DOWNLOADING', '1.1.8', '1.3.0'
FROM ota_task t WHERE t.task_name='【演示】温湿度传感器批量升级'
AND NOT EXISTS (SELECT 1 FROM ota_device_progress p WHERE p.task_id=t.id AND p.device_id='device_010');

-- ---------- 告警规则 ----------
DELETE FROM `alarm_rule` WHERE `tenant_id`=1 AND `rule_name` LIKE '【演示】%';
INSERT INTO `alarm_rule` (`tenant_id`,`rule_name`,`rule_type`,`drl_content`,`enabled`,`description`) VALUES
(1,'【演示】温度超限告警','THRESHOLD',
'package com.iot.platform.rules.dynamic;
import com.iot.platform.model.dto.DevicePropertyFact;
import com.iot.platform.model.dto.AlarmResult;
rule "DemoTempCritical"
when
  $f: DevicePropertyFact(properties["temperature"] != null, Double.parseDouble(properties["temperature"].toString()) > 40)
then
  $f.addResult(new AlarmResult("CRITICAL", "THRESHOLD", "温度超限", "温度超过40℃", "temperature", Double.parseDouble($f.getProperties().get("temperature").toString())));
end',
1,'演示：温度>40℃触发严重告警'),
(1,'【演示】低电量告警','THRESHOLD',
'package com.iot.platform.rules.dynamic;
import com.iot.platform.model.dto.DevicePropertyFact;
import com.iot.platform.model.dto.AlarmResult;
rule "DemoLowBattery"
when
  $f: DevicePropertyFact(properties["battery"] != null, Double.parseDouble(properties["battery"].toString()) < 20)
then
  $f.addResult(new AlarmResult("WARNING", "THRESHOLD", "电量不足", "电池电量低于20%", "battery", Double.parseDouble($f.getProperties().get("battery").toString())));
end',
1,'演示：电量<20%'),
(1,'【演示】复合高温高湿','COMPOSITE',
'package com.iot.platform.rules.dynamic;
import com.iot.platform.model.dto.DevicePropertyFact;
import com.iot.platform.model.dto.AlarmResult;
rule "DemoHotHumid"
when
  $f: DevicePropertyFact(properties["temperature"] != null, properties["humidity"] != null, Double.parseDouble(properties["temperature"].toString()) > 35, Double.parseDouble(properties["humidity"].toString()) > 80)
then
  $f.addResult(new AlarmResult("WARNING", "COMPOSITE", "高温高湿", "温湿度同时偏高", "temperature", Double.parseDouble($f.getProperties().get("temperature").toString())));
end',
1,'演示：温度>35 且湿度>80');

-- ---------- 能源表补充 ----------
INSERT INTO `energy_meter` (`tenant_id`,`meter_code`,`meter_name`,`meter_type`,`protocol`,`location`,`device_id`,`ct_ratio`,`pt_ratio`,`rated_power`,`status`,`remark`)
SELECT 1,'EM_003','充电桩专用电表','ELECTRICITY','MQTT','地下车库','device_007',50,1,200,1,'演示'
WHERE NOT EXISTS (SELECT 1 FROM energy_meter WHERE meter_code='EM_003' AND tenant_id=1);
INSERT INTO `energy_meter` (`tenant_id`,`meter_code`,`meter_name`,`meter_type`,`protocol`,`location`,`device_id`,`status`,`remark`)
SELECT 1,'GM_001','锅炉房燃气表','GAS','MODBUS','锅炉房',NULL,1,'演示'
WHERE NOT EXISTS (SELECT 1 FROM energy_meter WHERE meter_code='GM_001' AND tenant_id=1);

UPDATE energy_meter SET rated_power=800 WHERE meter_code='EM_001' AND tenant_id=1;
UPDATE energy_meter SET rated_power=300 WHERE meter_code='EM_002' AND tenant_id=1;

-- ---------- 负荷调度 ----------
DELETE FROM load_dispatch_strategy WHERE tenant_id=1 AND strategy_name LIKE '【演示】%';
INSERT INTO load_dispatch_strategy (`tenant_id`,`strategy_name`,`strategy_type`,`target_device_id`,`action_type`,`action_params`,`trigger_condition`,`enabled`,`priority`,`last_execute_time`) VALUES
(1,'【演示】午间空调削峰','PEAK_SHAVING','device_005','AC_TEMP_ADJUST','{"temp":26}','{"activePower":">500","peakFlag":"==1"}',1,10,DATE_SUB(NOW(), INTERVAL 1 DAY)),
(1,'【演示】充电桩限功率','DEMAND_RESPONSE','device_007','EV_CHARGE_LIMIT','{"powerLimit":30}','{"activePower":">700"}',1,20,DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(1,'【演示】夜间谷电蓄能提示','VALLEY_FILLING','device_003','DEVICE_SHUTDOWN','{"delayMinutes":0}','{"peakFlag":"==2"}',0,5,NULL);

-- ---------- 普通用户 ----------
INSERT INTO sys_user (`tenant_id`,`username`,`password`,`role`,`status`)
SELECT 1,'operator','$2b$10$htaDVnXCcpNWyl.wVjdkfusNoTZgEl4cyySQSJCMX9AMQjF3TynqG','USER',1
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username='operator' AND tenant_id=1);
INSERT INTO sys_user (`tenant_id`,`username`,`password`,`role`,`status`)
SELECT 1,'viewer','$2b$10$htaDVnXCcpNWyl.wVjdkfusNoTZgEl4cyySQSJCMX9AMQjF3TynqG','USER',1
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username='viewer' AND tenant_id=1);

-- ---------- 能耗读数（近7天逐小时） ----------
DELETE FROM energy_reading WHERE tenant_id=1 AND meter_code IN ('EM_001','EM_002','EM_003') AND reading_time >= UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 8 DAY))*1000;

INSERT INTO energy_reading (`tenant_id`,`meter_id`,`meter_code`,`meter_type`,`reading_time`,`voltage`,`current`,`active_power`,`reactive_power`,`power_factor`,`frequency`,`energy_consumption`,`cumulative_energy`,`peak_flag`)
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790035200000, 380.0, 56.1, 14.52, 4.356, 0.92, 50.0, 14.52, 125014.52, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790038800000, 380.0, 58.08, 14.52, 4.356, 0.92, 50.0, 14.52, 125029.04, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790042400000, 380.0, 60.06, 14.52, 4.356, 0.92, 50.0, 14.52, 125043.56, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790046000000, 380.0, 62.04, 14.52, 4.356, 0.92, 50.0, 14.52, 125058.08, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790049600000, 380.0, 64.02, 14.52, 4.356, 0.92, 50.0, 14.52, 125072.6, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790053200000, 380.0, 56.1, 14.52, 4.356, 0.92, 50.0, 14.52, 125087.12, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790056800000, 380.0, 58.08, 14.52, 4.356, 0.92, 50.0, 14.52, 125101.64, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790060400000, 380.0, 98.28, 23.76, 7.128, 0.92, 50.0, 23.76, 125125.4, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790064000000, 380.0, 152.28, 35.64, 10.692, 0.92, 50.0, 35.64, 125161.04, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790067600000, 380.0, 157.14, 35.64, 10.692, 0.92, 50.0, 35.64, 125196.68, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790071200000, 380.0, 137.7, 35.64, 10.692, 0.92, 50.0, 35.64, 125232.32, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790074800000, 380.0, 95.04, 23.76, 7.128, 0.92, 50.0, 23.76, 125256.08, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790078400000, 380.0, 98.28, 23.76, 7.128, 0.92, 50.0, 23.76, 125279.84, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790082000000, 380.0, 101.52, 23.76, 7.128, 0.92, 50.0, 23.76, 125303.6, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790085600000, 380.0, 104.76, 23.76, 7.128, 0.92, 50.0, 23.76, 125327.36, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790089200000, 380.0, 91.8, 23.76, 7.128, 0.92, 50.0, 23.76, 125351.12, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790092800000, 380.0, 95.04, 23.76, 7.128, 0.92, 50.0, 23.76, 125374.88, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790096400000, 380.0, 98.28, 23.76, 7.128, 0.92, 50.0, 23.76, 125398.64, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790100000000, 380.0, 152.28, 35.64, 10.692, 0.92, 50.0, 35.64, 125434.28, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790103600000, 380.0, 157.14, 35.64, 10.692, 0.92, 50.0, 35.64, 125469.92, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790107200000, 380.0, 137.7, 35.64, 10.692, 0.92, 50.0, 35.64, 125505.56, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790110800000, 380.0, 95.04, 23.76, 7.128, 0.92, 50.0, 23.76, 125529.32, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790114400000, 380.0, 98.28, 23.76, 7.128, 0.92, 50.0, 23.76, 125553.08, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790118000000, 380.0, 101.52, 23.76, 7.128, 0.92, 50.0, 23.76, 125576.84, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790121600000, 380.0, 56.1, 14.52, 4.356, 0.92, 50.0, 14.52, 125591.36, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790125200000, 380.0, 58.08, 14.52, 4.356, 0.92, 50.0, 14.52, 125605.88, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790128800000, 380.0, 60.06, 14.52, 4.356, 0.92, 50.0, 14.52, 125620.4, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790132400000, 380.0, 62.04, 14.52, 4.356, 0.92, 50.0, 14.52, 125634.92, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790136000000, 380.0, 64.02, 14.52, 4.356, 0.92, 50.0, 14.52, 125649.44, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790139600000, 380.0, 56.1, 14.52, 4.356, 0.92, 50.0, 14.52, 125663.96, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790143200000, 380.0, 58.08, 14.52, 4.356, 0.92, 50.0, 14.52, 125678.48, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790146800000, 380.0, 98.28, 23.76, 7.128, 0.92, 50.0, 23.76, 125702.24, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790150400000, 380.0, 152.28, 35.64, 10.692, 0.92, 50.0, 35.64, 125737.88, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790154000000, 380.0, 157.14, 35.64, 10.692, 0.92, 50.0, 35.64, 125773.52, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790157600000, 380.0, 137.7, 35.64, 10.692, 0.92, 50.0, 35.64, 125809.16, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790161200000, 380.0, 95.04, 23.76, 7.128, 0.92, 50.0, 23.76, 125832.92, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790164800000, 380.0, 98.28, 23.76, 7.128, 0.92, 50.0, 23.76, 125856.68, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790168400000, 380.0, 101.52, 23.76, 7.128, 0.92, 50.0, 23.76, 125880.44, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790172000000, 380.0, 104.76, 23.76, 7.128, 0.92, 50.0, 23.76, 125904.2, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790175600000, 380.0, 91.8, 23.76, 7.128, 0.92, 50.0, 23.76, 125927.96, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790179200000, 380.0, 95.04, 23.76, 7.128, 0.92, 50.0, 23.76, 125951.72, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790182800000, 380.0, 98.28, 23.76, 7.128, 0.92, 50.0, 23.76, 125975.48, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790186400000, 380.0, 152.28, 35.64, 10.692, 0.92, 50.0, 35.64, 126011.12, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790190000000, 380.0, 157.14, 35.64, 10.692, 0.92, 50.0, 35.64, 126046.76, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790193600000, 380.0, 137.7, 35.64, 10.692, 0.92, 50.0, 35.64, 126082.4, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790197200000, 380.0, 95.04, 23.76, 7.128, 0.92, 50.0, 23.76, 126106.16, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790200800000, 380.0, 98.28, 23.76, 7.128, 0.92, 50.0, 23.76, 126129.92, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790204400000, 380.0, 101.52, 23.76, 7.128, 0.92, 50.0, 23.76, 126153.68, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790208000000, 380.0, 56.1, 14.52, 4.356, 0.92, 50.0, 14.52, 126168.2, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790211600000, 380.0, 58.08, 14.52, 4.356, 0.92, 50.0, 14.52, 126182.72, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790215200000, 380.0, 60.06, 14.52, 4.356, 0.92, 50.0, 14.52, 126197.24, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790218800000, 380.0, 62.04, 14.52, 4.356, 0.92, 50.0, 14.52, 126211.76, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790222400000, 380.0, 64.02, 14.52, 4.356, 0.92, 50.0, 14.52, 126226.28, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790226000000, 380.0, 56.1, 14.52, 4.356, 0.92, 50.0, 14.52, 126240.8, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790229600000, 380.0, 58.08, 14.52, 4.356, 0.92, 50.0, 14.52, 126255.32, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790233200000, 380.0, 98.28, 23.76, 7.128, 0.92, 50.0, 23.76, 126279.08, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790236800000, 380.0, 152.28, 35.64, 10.692, 0.92, 50.0, 35.64, 126314.72, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790240400000, 380.0, 157.14, 35.64, 10.692, 0.92, 50.0, 35.64, 126350.36, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790244000000, 380.0, 137.7, 35.64, 10.692, 0.92, 50.0, 35.64, 126386.0, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790247600000, 380.0, 95.04, 23.76, 7.128, 0.92, 50.0, 23.76, 126409.76, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001';

INSERT INTO energy_reading (`tenant_id`,`meter_id`,`meter_code`,`meter_type`,`reading_time`,`voltage`,`current`,`active_power`,`reactive_power`,`power_factor`,`frequency`,`energy_consumption`,`cumulative_energy`,`peak_flag`)
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790251200000, 380.0, 98.28, 23.76, 7.128, 0.92, 50.0, 23.76, 126433.52, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790254800000, 380.0, 101.52, 23.76, 7.128, 0.92, 50.0, 23.76, 126457.28, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790258400000, 380.0, 104.76, 23.76, 7.128, 0.92, 50.0, 23.76, 126481.04, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790262000000, 380.0, 91.8, 23.76, 7.128, 0.92, 50.0, 23.76, 126504.8, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790265600000, 380.0, 95.04, 23.76, 7.128, 0.92, 50.0, 23.76, 126528.56, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790269200000, 380.0, 98.28, 23.76, 7.128, 0.92, 50.0, 23.76, 126552.32, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790272800000, 380.0, 152.28, 35.64, 10.692, 0.92, 50.0, 35.64, 126587.96, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790276400000, 380.0, 157.14, 35.64, 10.692, 0.92, 50.0, 35.64, 126623.6, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790280000000, 380.0, 137.7, 35.64, 10.692, 0.92, 50.0, 35.64, 126659.24, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790283600000, 380.0, 95.04, 23.76, 7.128, 0.92, 50.0, 23.76, 126683.0, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790287200000, 380.0, 98.28, 23.76, 7.128, 0.92, 50.0, 23.76, 126706.76, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790290800000, 380.0, 101.52, 23.76, 7.128, 0.92, 50.0, 23.76, 126730.52, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790294400000, 380.0, 56.1, 14.52, 4.356, 0.92, 50.0, 14.52, 126745.04, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790298000000, 380.0, 58.08, 14.52, 4.356, 0.92, 50.0, 14.52, 126759.56, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790301600000, 380.0, 60.06, 14.52, 4.356, 0.92, 50.0, 14.52, 126774.08, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790305200000, 380.0, 62.04, 14.52, 4.356, 0.92, 50.0, 14.52, 126788.6, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790308800000, 380.0, 64.02, 14.52, 4.356, 0.92, 50.0, 14.52, 126803.12, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790312400000, 380.0, 56.1, 14.52, 4.356, 0.92, 50.0, 14.52, 126817.64, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790316000000, 380.0, 58.08, 14.52, 4.356, 0.92, 50.0, 14.52, 126832.16, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790319600000, 380.0, 98.28, 23.76, 7.128, 0.92, 50.0, 23.76, 126855.92, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790323200000, 380.0, 152.28, 35.64, 10.692, 0.92, 50.0, 35.64, 126891.56, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790326800000, 380.0, 157.14, 35.64, 10.692, 0.92, 50.0, 35.64, 126927.2, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790330400000, 380.0, 137.7, 35.64, 10.692, 0.92, 50.0, 35.64, 126962.84, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790334000000, 380.0, 95.04, 23.76, 7.128, 0.92, 50.0, 23.76, 126986.6, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790337600000, 380.0, 98.28, 23.76, 7.128, 0.92, 50.0, 23.76, 127010.36, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790341200000, 380.0, 101.52, 23.76, 7.128, 0.92, 50.0, 23.76, 127034.12, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790344800000, 380.0, 104.76, 23.76, 7.128, 0.92, 50.0, 23.76, 127057.88, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790348400000, 380.0, 91.8, 23.76, 7.128, 0.92, 50.0, 23.76, 127081.64, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790352000000, 380.0, 95.04, 23.76, 7.128, 0.92, 50.0, 23.76, 127105.4, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790355600000, 380.0, 98.28, 23.76, 7.128, 0.92, 50.0, 23.76, 127129.16, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790359200000, 380.0, 152.28, 35.64, 10.692, 0.92, 50.0, 35.64, 127164.8, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790362800000, 380.0, 157.14, 35.64, 10.692, 0.92, 50.0, 35.64, 127200.44, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790366400000, 380.0, 137.7, 35.64, 10.692, 0.92, 50.0, 35.64, 127236.08, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790370000000, 380.0, 95.04, 23.76, 7.128, 0.92, 50.0, 23.76, 127259.84, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790373600000, 380.0, 98.28, 23.76, 7.128, 0.92, 50.0, 23.76, 127283.6, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790377200000, 380.0, 101.52, 23.76, 7.128, 0.92, 50.0, 23.76, 127307.36, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790380800000, 380.0, 56.1, 14.52, 4.356, 0.92, 50.0, 14.52, 127321.88, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790384400000, 380.0, 58.08, 14.52, 4.356, 0.92, 50.0, 14.52, 127336.4, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790388000000, 380.0, 60.06, 14.52, 4.356, 0.92, 50.0, 14.52, 127350.92, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790391600000, 380.0, 62.04, 14.52, 4.356, 0.92, 50.0, 14.52, 127365.44, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790395200000, 380.0, 64.02, 14.52, 4.356, 0.92, 50.0, 14.52, 127379.96, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790398800000, 380.0, 56.1, 14.52, 4.356, 0.92, 50.0, 14.52, 127394.48, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790402400000, 380.0, 58.08, 14.52, 4.356, 0.92, 50.0, 14.52, 127409.0, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790406000000, 380.0, 98.28, 23.76, 7.128, 0.92, 50.0, 23.76, 127432.76, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790409600000, 380.0, 152.28, 35.64, 10.692, 0.92, 50.0, 35.64, 127468.4, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790413200000, 380.0, 157.14, 35.64, 10.692, 0.92, 50.0, 35.64, 127504.04, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790416800000, 380.0, 137.7, 35.64, 10.692, 0.92, 50.0, 35.64, 127539.68, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790420400000, 380.0, 95.04, 23.76, 7.128, 0.92, 50.0, 23.76, 127563.44, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790424000000, 380.0, 98.28, 23.76, 7.128, 0.92, 50.0, 23.76, 127587.2, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790427600000, 380.0, 101.52, 23.76, 7.128, 0.92, 50.0, 23.76, 127610.96, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790431200000, 380.0, 104.76, 23.76, 7.128, 0.92, 50.0, 23.76, 127634.72, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790434800000, 380.0, 91.8, 23.76, 7.128, 0.92, 50.0, 23.76, 127658.48, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790438400000, 380.0, 95.04, 23.76, 7.128, 0.92, 50.0, 23.76, 127682.24, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790442000000, 380.0, 98.28, 23.76, 7.128, 0.92, 50.0, 23.76, 127706.0, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790445600000, 380.0, 152.28, 35.64, 10.692, 0.92, 50.0, 35.64, 127741.64, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790449200000, 380.0, 157.14, 35.64, 10.692, 0.92, 50.0, 35.64, 127777.28, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790452800000, 380.0, 137.7, 35.64, 10.692, 0.92, 50.0, 35.64, 127812.92, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790456400000, 380.0, 95.04, 23.76, 7.128, 0.92, 50.0, 23.76, 127836.68, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790460000000, 380.0, 98.28, 23.76, 7.128, 0.92, 50.0, 23.76, 127860.44, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790463600000, 380.0, 101.52, 23.76, 7.128, 0.92, 50.0, 23.76, 127884.2, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001';

INSERT INTO energy_reading (`tenant_id`,`meter_id`,`meter_code`,`meter_type`,`reading_time`,`voltage`,`current`,`active_power`,`reactive_power`,`power_factor`,`frequency`,`energy_consumption`,`cumulative_energy`,`peak_flag`)
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790467200000, 380.0, 56.1, 14.52, 4.356, 0.92, 50.0, 14.52, 127898.72, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790470800000, 380.0, 58.08, 14.52, 4.356, 0.92, 50.0, 14.52, 127913.24, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790474400000, 380.0, 60.06, 14.52, 4.356, 0.92, 50.0, 14.52, 127927.76, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790478000000, 380.0, 62.04, 14.52, 4.356, 0.92, 50.0, 14.52, 127942.28, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790481600000, 380.0, 64.02, 14.52, 4.356, 0.92, 50.0, 14.52, 127956.8, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790485200000, 380.0, 56.1, 14.52, 4.356, 0.92, 50.0, 14.52, 127971.32, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790488800000, 380.0, 58.08, 14.52, 4.356, 0.92, 50.0, 14.52, 127985.84, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790492400000, 380.0, 98.28, 23.76, 7.128, 0.92, 50.0, 23.76, 128009.6, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790496000000, 380.0, 152.28, 35.64, 10.692, 0.92, 50.0, 35.64, 128045.24, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790499600000, 380.0, 157.14, 35.64, 10.692, 0.92, 50.0, 35.64, 128080.88, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790503200000, 380.0, 137.7, 35.64, 10.692, 0.92, 50.0, 35.64, 128116.52, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790506800000, 380.0, 95.04, 23.76, 7.128, 0.92, 50.0, 23.76, 128140.28, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790510400000, 380.0, 98.28, 23.76, 7.128, 0.92, 50.0, 23.76, 128164.04, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790514000000, 380.0, 101.52, 23.76, 7.128, 0.92, 50.0, 23.76, 128187.8, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790517600000, 380.0, 104.76, 23.76, 7.128, 0.92, 50.0, 23.76, 128211.56, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790521200000, 380.0, 91.8, 23.76, 7.128, 0.92, 50.0, 23.76, 128235.32, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790524800000, 380.0, 95.04, 23.76, 7.128, 0.92, 50.0, 23.76, 128259.08, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790528400000, 380.0, 98.28, 23.76, 7.128, 0.92, 50.0, 23.76, 128282.84, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790532000000, 380.0, 152.28, 35.64, 10.692, 0.92, 50.0, 35.64, 128318.48, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790535600000, 380.0, 157.14, 35.64, 10.692, 0.92, 50.0, 35.64, 128354.12, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790539200000, 380.0, 137.7, 35.64, 10.692, 0.92, 50.0, 35.64, 128389.76, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790542800000, 380.0, 95.04, 23.76, 7.128, 0.92, 50.0, 23.76, 128413.52, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790546400000, 380.0, 98.28, 23.76, 7.128, 0.92, 50.0, 23.76, 128437.28, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790550000000, 380.0, 101.52, 23.76, 7.128, 0.92, 50.0, 23.76, 128461.04, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790553600000, 380.0, 56.1, 14.52, 4.356, 0.92, 50.0, 14.52, 128475.56, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790557200000, 380.0, 58.08, 14.52, 4.356, 0.92, 50.0, 14.52, 128490.08, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790560800000, 380.0, 60.06, 14.52, 4.356, 0.92, 50.0, 14.52, 128504.6, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790564400000, 380.0, 62.04, 14.52, 4.356, 0.92, 50.0, 14.52, 128519.12, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790568000000, 380.0, 64.02, 14.52, 4.356, 0.92, 50.0, 14.52, 128533.64, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790571600000, 380.0, 56.1, 14.52, 4.356, 0.92, 50.0, 14.52, 128548.16, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790575200000, 380.0, 58.08, 14.52, 4.356, 0.92, 50.0, 14.52, 128562.68, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790578800000, 380.0, 98.28, 23.76, 7.128, 0.92, 50.0, 23.76, 128586.44, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790582400000, 380.0, 152.28, 35.64, 10.692, 0.92, 50.0, 35.64, 128622.08, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790586000000, 380.0, 157.14, 35.64, 10.692, 0.92, 50.0, 35.64, 128657.72, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_001', 'ELECTRICITY', 1790589600000, 380.0, 137.7, 35.64, 10.692, 0.92, 50.0, 35.64, 128693.36, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001';

INSERT INTO energy_reading (`tenant_id`,`meter_id`,`meter_code`,`meter_type`,`reading_time`,`voltage`,`current`,`active_power`,`reactive_power`,`power_factor`,`frequency`,`energy_consumption`,`cumulative_energy`,`peak_flag`)
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790035200000, 220.0, 18.7, 4.84, 1.452, 0.88, 50.0, 4.84, 48004.84, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790038800000, 220.0, 19.36, 4.84, 1.452, 0.88, 50.0, 4.84, 48009.68, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790042400000, 220.0, 20.02, 4.84, 1.452, 0.88, 50.0, 4.84, 48014.52, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790046000000, 220.0, 20.68, 4.84, 1.452, 0.88, 50.0, 4.84, 48019.36, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790049600000, 220.0, 21.34, 4.84, 1.452, 0.88, 50.0, 4.84, 48024.2, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790053200000, 220.0, 18.7, 4.84, 1.452, 0.88, 50.0, 4.84, 48029.04, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790056800000, 220.0, 19.36, 4.84, 1.452, 0.88, 50.0, 4.84, 48033.88, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790060400000, 220.0, 32.76, 7.92, 2.376, 0.88, 50.0, 7.92, 48041.8, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790064000000, 220.0, 50.76, 11.88, 3.564, 0.88, 50.0, 11.88, 48053.68, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790067600000, 220.0, 52.38, 11.88, 3.564, 0.88, 50.0, 11.88, 48065.56, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790071200000, 220.0, 45.9, 11.88, 3.564, 0.88, 50.0, 11.88, 48077.44, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790074800000, 220.0, 31.68, 7.92, 2.376, 0.88, 50.0, 7.92, 48085.36, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790078400000, 220.0, 32.76, 7.92, 2.376, 0.88, 50.0, 7.92, 48093.28, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790082000000, 220.0, 33.84, 7.92, 2.376, 0.88, 50.0, 7.92, 48101.2, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790085600000, 220.0, 34.92, 7.92, 2.376, 0.88, 50.0, 7.92, 48109.12, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790089200000, 220.0, 30.6, 7.92, 2.376, 0.88, 50.0, 7.92, 48117.04, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790092800000, 220.0, 31.68, 7.92, 2.376, 0.88, 50.0, 7.92, 48124.96, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790096400000, 220.0, 32.76, 7.92, 2.376, 0.88, 50.0, 7.92, 48132.88, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790100000000, 220.0, 50.76, 11.88, 3.564, 0.88, 50.0, 11.88, 48144.76, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790103600000, 220.0, 52.38, 11.88, 3.564, 0.88, 50.0, 11.88, 48156.64, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790107200000, 220.0, 45.9, 11.88, 3.564, 0.88, 50.0, 11.88, 48168.52, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790110800000, 220.0, 31.68, 7.92, 2.376, 0.88, 50.0, 7.92, 48176.44, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790114400000, 220.0, 32.76, 7.92, 2.376, 0.88, 50.0, 7.92, 48184.36, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790118000000, 220.0, 33.84, 7.92, 2.376, 0.88, 50.0, 7.92, 48192.28, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790121600000, 220.0, 18.7, 4.84, 1.452, 0.88, 50.0, 4.84, 48197.12, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790125200000, 220.0, 19.36, 4.84, 1.452, 0.88, 50.0, 4.84, 48201.96, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790128800000, 220.0, 20.02, 4.84, 1.452, 0.88, 50.0, 4.84, 48206.8, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790132400000, 220.0, 20.68, 4.84, 1.452, 0.88, 50.0, 4.84, 48211.64, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790136000000, 220.0, 21.34, 4.84, 1.452, 0.88, 50.0, 4.84, 48216.48, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790139600000, 220.0, 18.7, 4.84, 1.452, 0.88, 50.0, 4.84, 48221.32, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790143200000, 220.0, 19.36, 4.84, 1.452, 0.88, 50.0, 4.84, 48226.16, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790146800000, 220.0, 32.76, 7.92, 2.376, 0.88, 50.0, 7.92, 48234.08, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790150400000, 220.0, 50.76, 11.88, 3.564, 0.88, 50.0, 11.88, 48245.96, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790154000000, 220.0, 52.38, 11.88, 3.564, 0.88, 50.0, 11.88, 48257.84, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790157600000, 220.0, 45.9, 11.88, 3.564, 0.88, 50.0, 11.88, 48269.72, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790161200000, 220.0, 31.68, 7.92, 2.376, 0.88, 50.0, 7.92, 48277.64, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790164800000, 220.0, 32.76, 7.92, 2.376, 0.88, 50.0, 7.92, 48285.56, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790168400000, 220.0, 33.84, 7.92, 2.376, 0.88, 50.0, 7.92, 48293.48, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790172000000, 220.0, 34.92, 7.92, 2.376, 0.88, 50.0, 7.92, 48301.4, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790175600000, 220.0, 30.6, 7.92, 2.376, 0.88, 50.0, 7.92, 48309.32, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790179200000, 220.0, 31.68, 7.92, 2.376, 0.88, 50.0, 7.92, 48317.24, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790182800000, 220.0, 32.76, 7.92, 2.376, 0.88, 50.0, 7.92, 48325.16, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790186400000, 220.0, 50.76, 11.88, 3.564, 0.88, 50.0, 11.88, 48337.04, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790190000000, 220.0, 52.38, 11.88, 3.564, 0.88, 50.0, 11.88, 48348.92, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790193600000, 220.0, 45.9, 11.88, 3.564, 0.88, 50.0, 11.88, 48360.8, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790197200000, 220.0, 31.68, 7.92, 2.376, 0.88, 50.0, 7.92, 48368.72, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790200800000, 220.0, 32.76, 7.92, 2.376, 0.88, 50.0, 7.92, 48376.64, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790204400000, 220.0, 33.84, 7.92, 2.376, 0.88, 50.0, 7.92, 48384.56, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790208000000, 220.0, 18.7, 4.84, 1.452, 0.88, 50.0, 4.84, 48389.4, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790211600000, 220.0, 19.36, 4.84, 1.452, 0.88, 50.0, 4.84, 48394.24, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790215200000, 220.0, 20.02, 4.84, 1.452, 0.88, 50.0, 4.84, 48399.08, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790218800000, 220.0, 20.68, 4.84, 1.452, 0.88, 50.0, 4.84, 48403.92, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790222400000, 220.0, 21.34, 4.84, 1.452, 0.88, 50.0, 4.84, 48408.76, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790226000000, 220.0, 18.7, 4.84, 1.452, 0.88, 50.0, 4.84, 48413.6, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790229600000, 220.0, 19.36, 4.84, 1.452, 0.88, 50.0, 4.84, 48418.44, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790233200000, 220.0, 32.76, 7.92, 2.376, 0.88, 50.0, 7.92, 48426.36, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790236800000, 220.0, 50.76, 11.88, 3.564, 0.88, 50.0, 11.88, 48438.24, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790240400000, 220.0, 52.38, 11.88, 3.564, 0.88, 50.0, 11.88, 48450.12, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790244000000, 220.0, 45.9, 11.88, 3.564, 0.88, 50.0, 11.88, 48462.0, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790247600000, 220.0, 31.68, 7.92, 2.376, 0.88, 50.0, 7.92, 48469.92, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002';

INSERT INTO energy_reading (`tenant_id`,`meter_id`,`meter_code`,`meter_type`,`reading_time`,`voltage`,`current`,`active_power`,`reactive_power`,`power_factor`,`frequency`,`energy_consumption`,`cumulative_energy`,`peak_flag`)
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790251200000, 220.0, 32.76, 7.92, 2.376, 0.88, 50.0, 7.92, 48477.84, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790254800000, 220.0, 33.84, 7.92, 2.376, 0.88, 50.0, 7.92, 48485.76, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790258400000, 220.0, 34.92, 7.92, 2.376, 0.88, 50.0, 7.92, 48493.68, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790262000000, 220.0, 30.6, 7.92, 2.376, 0.88, 50.0, 7.92, 48501.6, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790265600000, 220.0, 31.68, 7.92, 2.376, 0.88, 50.0, 7.92, 48509.52, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790269200000, 220.0, 32.76, 7.92, 2.376, 0.88, 50.0, 7.92, 48517.44, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790272800000, 220.0, 50.76, 11.88, 3.564, 0.88, 50.0, 11.88, 48529.32, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790276400000, 220.0, 52.38, 11.88, 3.564, 0.88, 50.0, 11.88, 48541.2, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790280000000, 220.0, 45.9, 11.88, 3.564, 0.88, 50.0, 11.88, 48553.08, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790283600000, 220.0, 31.68, 7.92, 2.376, 0.88, 50.0, 7.92, 48561.0, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790287200000, 220.0, 32.76, 7.92, 2.376, 0.88, 50.0, 7.92, 48568.92, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790290800000, 220.0, 33.84, 7.92, 2.376, 0.88, 50.0, 7.92, 48576.84, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790294400000, 220.0, 18.7, 4.84, 1.452, 0.88, 50.0, 4.84, 48581.68, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790298000000, 220.0, 19.36, 4.84, 1.452, 0.88, 50.0, 4.84, 48586.52, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790301600000, 220.0, 20.02, 4.84, 1.452, 0.88, 50.0, 4.84, 48591.36, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790305200000, 220.0, 20.68, 4.84, 1.452, 0.88, 50.0, 4.84, 48596.2, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790308800000, 220.0, 21.34, 4.84, 1.452, 0.88, 50.0, 4.84, 48601.04, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790312400000, 220.0, 18.7, 4.84, 1.452, 0.88, 50.0, 4.84, 48605.88, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790316000000, 220.0, 19.36, 4.84, 1.452, 0.88, 50.0, 4.84, 48610.72, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790319600000, 220.0, 32.76, 7.92, 2.376, 0.88, 50.0, 7.92, 48618.64, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790323200000, 220.0, 50.76, 11.88, 3.564, 0.88, 50.0, 11.88, 48630.52, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790326800000, 220.0, 52.38, 11.88, 3.564, 0.88, 50.0, 11.88, 48642.4, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790330400000, 220.0, 45.9, 11.88, 3.564, 0.88, 50.0, 11.88, 48654.28, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790334000000, 220.0, 31.68, 7.92, 2.376, 0.88, 50.0, 7.92, 48662.2, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790337600000, 220.0, 32.76, 7.92, 2.376, 0.88, 50.0, 7.92, 48670.12, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790341200000, 220.0, 33.84, 7.92, 2.376, 0.88, 50.0, 7.92, 48678.04, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790344800000, 220.0, 34.92, 7.92, 2.376, 0.88, 50.0, 7.92, 48685.96, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790348400000, 220.0, 30.6, 7.92, 2.376, 0.88, 50.0, 7.92, 48693.88, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790352000000, 220.0, 31.68, 7.92, 2.376, 0.88, 50.0, 7.92, 48701.8, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790355600000, 220.0, 32.76, 7.92, 2.376, 0.88, 50.0, 7.92, 48709.72, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790359200000, 220.0, 50.76, 11.88, 3.564, 0.88, 50.0, 11.88, 48721.6, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790362800000, 220.0, 52.38, 11.88, 3.564, 0.88, 50.0, 11.88, 48733.48, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790366400000, 220.0, 45.9, 11.88, 3.564, 0.88, 50.0, 11.88, 48745.36, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790370000000, 220.0, 31.68, 7.92, 2.376, 0.88, 50.0, 7.92, 48753.28, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790373600000, 220.0, 32.76, 7.92, 2.376, 0.88, 50.0, 7.92, 48761.2, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790377200000, 220.0, 33.84, 7.92, 2.376, 0.88, 50.0, 7.92, 48769.12, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790380800000, 220.0, 18.7, 4.84, 1.452, 0.88, 50.0, 4.84, 48773.96, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790384400000, 220.0, 19.36, 4.84, 1.452, 0.88, 50.0, 4.84, 48778.8, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790388000000, 220.0, 20.02, 4.84, 1.452, 0.88, 50.0, 4.84, 48783.64, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790391600000, 220.0, 20.68, 4.84, 1.452, 0.88, 50.0, 4.84, 48788.48, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790395200000, 220.0, 21.34, 4.84, 1.452, 0.88, 50.0, 4.84, 48793.32, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790398800000, 220.0, 18.7, 4.84, 1.452, 0.88, 50.0, 4.84, 48798.16, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790402400000, 220.0, 19.36, 4.84, 1.452, 0.88, 50.0, 4.84, 48803.0, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790406000000, 220.0, 32.76, 7.92, 2.376, 0.88, 50.0, 7.92, 48810.92, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790409600000, 220.0, 50.76, 11.88, 3.564, 0.88, 50.0, 11.88, 48822.8, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790413200000, 220.0, 52.38, 11.88, 3.564, 0.88, 50.0, 11.88, 48834.68, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790416800000, 220.0, 45.9, 11.88, 3.564, 0.88, 50.0, 11.88, 48846.56, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790420400000, 220.0, 31.68, 7.92, 2.376, 0.88, 50.0, 7.92, 48854.48, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790424000000, 220.0, 32.76, 7.92, 2.376, 0.88, 50.0, 7.92, 48862.4, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790427600000, 220.0, 33.84, 7.92, 2.376, 0.88, 50.0, 7.92, 48870.32, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790431200000, 220.0, 34.92, 7.92, 2.376, 0.88, 50.0, 7.92, 48878.24, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790434800000, 220.0, 30.6, 7.92, 2.376, 0.88, 50.0, 7.92, 48886.16, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790438400000, 220.0, 31.68, 7.92, 2.376, 0.88, 50.0, 7.92, 48894.08, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790442000000, 220.0, 32.76, 7.92, 2.376, 0.88, 50.0, 7.92, 48902.0, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790445600000, 220.0, 50.76, 11.88, 3.564, 0.88, 50.0, 11.88, 48913.88, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790449200000, 220.0, 52.38, 11.88, 3.564, 0.88, 50.0, 11.88, 48925.76, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790452800000, 220.0, 45.9, 11.88, 3.564, 0.88, 50.0, 11.88, 48937.64, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790456400000, 220.0, 31.68, 7.92, 2.376, 0.88, 50.0, 7.92, 48945.56, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790460000000, 220.0, 32.76, 7.92, 2.376, 0.88, 50.0, 7.92, 48953.48, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790463600000, 220.0, 33.84, 7.92, 2.376, 0.88, 50.0, 7.92, 48961.4, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002';

INSERT INTO energy_reading (`tenant_id`,`meter_id`,`meter_code`,`meter_type`,`reading_time`,`voltage`,`current`,`active_power`,`reactive_power`,`power_factor`,`frequency`,`energy_consumption`,`cumulative_energy`,`peak_flag`)
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790467200000, 220.0, 18.7, 4.84, 1.452, 0.88, 50.0, 4.84, 48966.24, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790470800000, 220.0, 19.36, 4.84, 1.452, 0.88, 50.0, 4.84, 48971.08, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790474400000, 220.0, 20.02, 4.84, 1.452, 0.88, 50.0, 4.84, 48975.92, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790478000000, 220.0, 20.68, 4.84, 1.452, 0.88, 50.0, 4.84, 48980.76, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790481600000, 220.0, 21.34, 4.84, 1.452, 0.88, 50.0, 4.84, 48985.6, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790485200000, 220.0, 18.7, 4.84, 1.452, 0.88, 50.0, 4.84, 48990.44, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790488800000, 220.0, 19.36, 4.84, 1.452, 0.88, 50.0, 4.84, 48995.28, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790492400000, 220.0, 32.76, 7.92, 2.376, 0.88, 50.0, 7.92, 49003.2, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790496000000, 220.0, 50.76, 11.88, 3.564, 0.88, 50.0, 11.88, 49015.08, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790499600000, 220.0, 52.38, 11.88, 3.564, 0.88, 50.0, 11.88, 49026.96, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790503200000, 220.0, 45.9, 11.88, 3.564, 0.88, 50.0, 11.88, 49038.84, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790506800000, 220.0, 31.68, 7.92, 2.376, 0.88, 50.0, 7.92, 49046.76, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790510400000, 220.0, 32.76, 7.92, 2.376, 0.88, 50.0, 7.92, 49054.68, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790514000000, 220.0, 33.84, 7.92, 2.376, 0.88, 50.0, 7.92, 49062.6, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790517600000, 220.0, 34.92, 7.92, 2.376, 0.88, 50.0, 7.92, 49070.52, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790521200000, 220.0, 30.6, 7.92, 2.376, 0.88, 50.0, 7.92, 49078.44, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790524800000, 220.0, 31.68, 7.92, 2.376, 0.88, 50.0, 7.92, 49086.36, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790528400000, 220.0, 32.76, 7.92, 2.376, 0.88, 50.0, 7.92, 49094.28, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790532000000, 220.0, 50.76, 11.88, 3.564, 0.88, 50.0, 11.88, 49106.16, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790535600000, 220.0, 52.38, 11.88, 3.564, 0.88, 50.0, 11.88, 49118.04, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790539200000, 220.0, 45.9, 11.88, 3.564, 0.88, 50.0, 11.88, 49129.92, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790542800000, 220.0, 31.68, 7.92, 2.376, 0.88, 50.0, 7.92, 49137.84, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790546400000, 220.0, 32.76, 7.92, 2.376, 0.88, 50.0, 7.92, 49145.76, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790550000000, 220.0, 33.84, 7.92, 2.376, 0.88, 50.0, 7.92, 49153.68, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790553600000, 220.0, 18.7, 4.84, 1.452, 0.88, 50.0, 4.84, 49158.52, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790557200000, 220.0, 19.36, 4.84, 1.452, 0.88, 50.0, 4.84, 49163.36, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790560800000, 220.0, 20.02, 4.84, 1.452, 0.88, 50.0, 4.84, 49168.2, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790564400000, 220.0, 20.68, 4.84, 1.452, 0.88, 50.0, 4.84, 49173.04, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790568000000, 220.0, 21.34, 4.84, 1.452, 0.88, 50.0, 4.84, 49177.88, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790571600000, 220.0, 18.7, 4.84, 1.452, 0.88, 50.0, 4.84, 49182.72, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790575200000, 220.0, 19.36, 4.84, 1.452, 0.88, 50.0, 4.84, 49187.56, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790578800000, 220.0, 32.76, 7.92, 2.376, 0.88, 50.0, 7.92, 49195.48, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790582400000, 220.0, 50.76, 11.88, 3.564, 0.88, 50.0, 11.88, 49207.36, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790586000000, 220.0, 52.38, 11.88, 3.564, 0.88, 50.0, 11.88, 49219.24, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_002', 'ELECTRICITY', 1790589600000, 220.0, 45.9, 11.88, 3.564, 0.88, 50.0, 11.88, 49231.12, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002';

INSERT INTO energy_reading (`tenant_id`,`meter_id`,`meter_code`,`meter_type`,`reading_time`,`voltage`,`current`,`active_power`,`reactive_power`,`power_factor`,`frequency`,`energy_consumption`,`cumulative_energy`,`peak_flag`)
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790035200000, 380.0, 14.03, 3.63, 1.089, 0.95, 50.0, 3.63, 9203.63, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790038800000, 380.0, 14.52, 3.63, 1.089, 0.95, 50.0, 3.63, 9207.26, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790042400000, 380.0, 15.01, 3.63, 1.089, 0.95, 50.0, 3.63, 9210.89, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790046000000, 380.0, 15.51, 3.63, 1.089, 0.95, 50.0, 3.63, 9214.52, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790049600000, 380.0, 16.0, 3.63, 1.089, 0.95, 50.0, 3.63, 9218.15, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790053200000, 380.0, 14.03, 3.63, 1.089, 0.95, 50.0, 3.63, 9221.78, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790056800000, 380.0, 14.52, 3.63, 1.089, 0.95, 50.0, 3.63, 9225.41, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790060400000, 380.0, 24.57, 5.94, 1.782, 0.95, 50.0, 5.94, 9231.35, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790064000000, 380.0, 38.07, 8.91, 2.673, 0.95, 50.0, 8.91, 9240.26, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790067600000, 380.0, 39.28, 8.91, 2.673, 0.95, 50.0, 8.91, 9249.17, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790071200000, 380.0, 34.42, 8.91, 2.673, 0.95, 50.0, 8.91, 9258.08, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790074800000, 380.0, 23.76, 5.94, 1.782, 0.95, 50.0, 5.94, 9264.02, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790078400000, 380.0, 24.57, 5.94, 1.782, 0.95, 50.0, 5.94, 9269.96, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790082000000, 380.0, 25.38, 5.94, 1.782, 0.95, 50.0, 5.94, 9275.9, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790085600000, 380.0, 26.19, 5.94, 1.782, 0.95, 50.0, 5.94, 9281.84, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790089200000, 380.0, 22.95, 5.94, 1.782, 0.95, 50.0, 5.94, 9287.78, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790092800000, 380.0, 23.76, 5.94, 1.782, 0.95, 50.0, 5.94, 9293.72, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790096400000, 380.0, 24.57, 5.94, 1.782, 0.95, 50.0, 5.94, 9299.66, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790100000000, 380.0, 38.07, 8.91, 2.673, 0.95, 50.0, 8.91, 9308.57, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790103600000, 380.0, 39.28, 8.91, 2.673, 0.95, 50.0, 8.91, 9317.48, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790107200000, 380.0, 34.42, 8.91, 2.673, 0.95, 50.0, 8.91, 9326.39, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790110800000, 380.0, 23.76, 5.94, 1.782, 0.95, 50.0, 5.94, 9332.33, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790114400000, 380.0, 24.57, 5.94, 1.782, 0.95, 50.0, 5.94, 9338.27, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790118000000, 380.0, 25.38, 5.94, 1.782, 0.95, 50.0, 5.94, 9344.21, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790121600000, 380.0, 14.03, 3.63, 1.089, 0.95, 50.0, 3.63, 9347.84, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790125200000, 380.0, 14.52, 3.63, 1.089, 0.95, 50.0, 3.63, 9351.47, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790128800000, 380.0, 15.01, 3.63, 1.089, 0.95, 50.0, 3.63, 9355.1, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790132400000, 380.0, 15.51, 3.63, 1.089, 0.95, 50.0, 3.63, 9358.73, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790136000000, 380.0, 16.0, 3.63, 1.089, 0.95, 50.0, 3.63, 9362.36, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790139600000, 380.0, 14.03, 3.63, 1.089, 0.95, 50.0, 3.63, 9365.99, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790143200000, 380.0, 14.52, 3.63, 1.089, 0.95, 50.0, 3.63, 9369.62, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790146800000, 380.0, 24.57, 5.94, 1.782, 0.95, 50.0, 5.94, 9375.56, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790150400000, 380.0, 38.07, 8.91, 2.673, 0.95, 50.0, 8.91, 9384.47, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790154000000, 380.0, 39.28, 8.91, 2.673, 0.95, 50.0, 8.91, 9393.38, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790157600000, 380.0, 34.42, 8.91, 2.673, 0.95, 50.0, 8.91, 9402.29, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790161200000, 380.0, 23.76, 5.94, 1.782, 0.95, 50.0, 5.94, 9408.23, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790164800000, 380.0, 24.57, 5.94, 1.782, 0.95, 50.0, 5.94, 9414.17, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790168400000, 380.0, 25.38, 5.94, 1.782, 0.95, 50.0, 5.94, 9420.11, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790172000000, 380.0, 26.19, 5.94, 1.782, 0.95, 50.0, 5.94, 9426.05, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790175600000, 380.0, 22.95, 5.94, 1.782, 0.95, 50.0, 5.94, 9431.99, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790179200000, 380.0, 23.76, 5.94, 1.782, 0.95, 50.0, 5.94, 9437.93, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790182800000, 380.0, 24.57, 5.94, 1.782, 0.95, 50.0, 5.94, 9443.87, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790186400000, 380.0, 38.07, 8.91, 2.673, 0.95, 50.0, 8.91, 9452.78, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790190000000, 380.0, 39.28, 8.91, 2.673, 0.95, 50.0, 8.91, 9461.69, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790193600000, 380.0, 34.42, 8.91, 2.673, 0.95, 50.0, 8.91, 9470.6, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790197200000, 380.0, 23.76, 5.94, 1.782, 0.95, 50.0, 5.94, 9476.54, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790200800000, 380.0, 24.57, 5.94, 1.782, 0.95, 50.0, 5.94, 9482.48, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790204400000, 380.0, 25.38, 5.94, 1.782, 0.95, 50.0, 5.94, 9488.42, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790208000000, 380.0, 14.03, 3.63, 1.089, 0.95, 50.0, 3.63, 9492.05, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790211600000, 380.0, 14.52, 3.63, 1.089, 0.95, 50.0, 3.63, 9495.68, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790215200000, 380.0, 15.01, 3.63, 1.089, 0.95, 50.0, 3.63, 9499.31, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790218800000, 380.0, 15.51, 3.63, 1.089, 0.95, 50.0, 3.63, 9502.94, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790222400000, 380.0, 16.0, 3.63, 1.089, 0.95, 50.0, 3.63, 9506.57, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790226000000, 380.0, 14.03, 3.63, 1.089, 0.95, 50.0, 3.63, 9510.2, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790229600000, 380.0, 14.52, 3.63, 1.089, 0.95, 50.0, 3.63, 9513.83, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790233200000, 380.0, 24.57, 5.94, 1.782, 0.95, 50.0, 5.94, 9519.77, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790236800000, 380.0, 38.07, 8.91, 2.673, 0.95, 50.0, 8.91, 9528.68, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790240400000, 380.0, 39.28, 8.91, 2.673, 0.95, 50.0, 8.91, 9537.59, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790244000000, 380.0, 34.42, 8.91, 2.673, 0.95, 50.0, 8.91, 9546.5, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790247600000, 380.0, 23.76, 5.94, 1.782, 0.95, 50.0, 5.94, 9552.44, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003';

INSERT INTO energy_reading (`tenant_id`,`meter_id`,`meter_code`,`meter_type`,`reading_time`,`voltage`,`current`,`active_power`,`reactive_power`,`power_factor`,`frequency`,`energy_consumption`,`cumulative_energy`,`peak_flag`)
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790251200000, 380.0, 24.57, 5.94, 1.782, 0.95, 50.0, 5.94, 9558.38, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790254800000, 380.0, 25.38, 5.94, 1.782, 0.95, 50.0, 5.94, 9564.32, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790258400000, 380.0, 26.19, 5.94, 1.782, 0.95, 50.0, 5.94, 9570.26, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790262000000, 380.0, 22.95, 5.94, 1.782, 0.95, 50.0, 5.94, 9576.2, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790265600000, 380.0, 23.76, 5.94, 1.782, 0.95, 50.0, 5.94, 9582.14, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790269200000, 380.0, 24.57, 5.94, 1.782, 0.95, 50.0, 5.94, 9588.08, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790272800000, 380.0, 38.07, 8.91, 2.673, 0.95, 50.0, 8.91, 9596.99, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790276400000, 380.0, 39.28, 8.91, 2.673, 0.95, 50.0, 8.91, 9605.9, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790280000000, 380.0, 34.42, 8.91, 2.673, 0.95, 50.0, 8.91, 9614.81, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790283600000, 380.0, 23.76, 5.94, 1.782, 0.95, 50.0, 5.94, 9620.75, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790287200000, 380.0, 24.57, 5.94, 1.782, 0.95, 50.0, 5.94, 9626.69, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790290800000, 380.0, 25.38, 5.94, 1.782, 0.95, 50.0, 5.94, 9632.63, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790294400000, 380.0, 14.03, 3.63, 1.089, 0.95, 50.0, 3.63, 9636.26, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790298000000, 380.0, 14.52, 3.63, 1.089, 0.95, 50.0, 3.63, 9639.89, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790301600000, 380.0, 15.01, 3.63, 1.089, 0.95, 50.0, 3.63, 9643.52, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790305200000, 380.0, 15.51, 3.63, 1.089, 0.95, 50.0, 3.63, 9647.15, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790308800000, 380.0, 16.0, 3.63, 1.089, 0.95, 50.0, 3.63, 9650.78, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790312400000, 380.0, 14.03, 3.63, 1.089, 0.95, 50.0, 3.63, 9654.41, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790316000000, 380.0, 14.52, 3.63, 1.089, 0.95, 50.0, 3.63, 9658.04, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790319600000, 380.0, 24.57, 5.94, 1.782, 0.95, 50.0, 5.94, 9663.98, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790323200000, 380.0, 38.07, 8.91, 2.673, 0.95, 50.0, 8.91, 9672.89, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790326800000, 380.0, 39.28, 8.91, 2.673, 0.95, 50.0, 8.91, 9681.8, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790330400000, 380.0, 34.42, 8.91, 2.673, 0.95, 50.0, 8.91, 9690.71, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790334000000, 380.0, 23.76, 5.94, 1.782, 0.95, 50.0, 5.94, 9696.65, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790337600000, 380.0, 24.57, 5.94, 1.782, 0.95, 50.0, 5.94, 9702.59, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790341200000, 380.0, 25.38, 5.94, 1.782, 0.95, 50.0, 5.94, 9708.53, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790344800000, 380.0, 26.19, 5.94, 1.782, 0.95, 50.0, 5.94, 9714.47, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790348400000, 380.0, 22.95, 5.94, 1.782, 0.95, 50.0, 5.94, 9720.41, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790352000000, 380.0, 23.76, 5.94, 1.782, 0.95, 50.0, 5.94, 9726.35, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790355600000, 380.0, 24.57, 5.94, 1.782, 0.95, 50.0, 5.94, 9732.29, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790359200000, 380.0, 38.07, 8.91, 2.673, 0.95, 50.0, 8.91, 9741.2, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790362800000, 380.0, 39.28, 8.91, 2.673, 0.95, 50.0, 8.91, 9750.11, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790366400000, 380.0, 34.42, 8.91, 2.673, 0.95, 50.0, 8.91, 9759.02, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790370000000, 380.0, 23.76, 5.94, 1.782, 0.95, 50.0, 5.94, 9764.96, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790373600000, 380.0, 24.57, 5.94, 1.782, 0.95, 50.0, 5.94, 9770.9, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790377200000, 380.0, 25.38, 5.94, 1.782, 0.95, 50.0, 5.94, 9776.84, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790380800000, 380.0, 14.03, 3.63, 1.089, 0.95, 50.0, 3.63, 9780.47, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790384400000, 380.0, 14.52, 3.63, 1.089, 0.95, 50.0, 3.63, 9784.1, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790388000000, 380.0, 15.01, 3.63, 1.089, 0.95, 50.0, 3.63, 9787.73, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790391600000, 380.0, 15.51, 3.63, 1.089, 0.95, 50.0, 3.63, 9791.36, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790395200000, 380.0, 16.0, 3.63, 1.089, 0.95, 50.0, 3.63, 9794.99, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790398800000, 380.0, 14.03, 3.63, 1.089, 0.95, 50.0, 3.63, 9798.62, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790402400000, 380.0, 14.52, 3.63, 1.089, 0.95, 50.0, 3.63, 9802.25, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790406000000, 380.0, 24.57, 5.94, 1.782, 0.95, 50.0, 5.94, 9808.19, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790409600000, 380.0, 38.07, 8.91, 2.673, 0.95, 50.0, 8.91, 9817.1, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790413200000, 380.0, 39.28, 8.91, 2.673, 0.95, 50.0, 8.91, 9826.01, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790416800000, 380.0, 34.42, 8.91, 2.673, 0.95, 50.0, 8.91, 9834.92, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790420400000, 380.0, 23.76, 5.94, 1.782, 0.95, 50.0, 5.94, 9840.86, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790424000000, 380.0, 24.57, 5.94, 1.782, 0.95, 50.0, 5.94, 9846.8, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790427600000, 380.0, 25.38, 5.94, 1.782, 0.95, 50.0, 5.94, 9852.74, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790431200000, 380.0, 26.19, 5.94, 1.782, 0.95, 50.0, 5.94, 9858.68, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790434800000, 380.0, 22.95, 5.94, 1.782, 0.95, 50.0, 5.94, 9864.62, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790438400000, 380.0, 23.76, 5.94, 1.782, 0.95, 50.0, 5.94, 9870.56, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790442000000, 380.0, 24.57, 5.94, 1.782, 0.95, 50.0, 5.94, 9876.5, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790445600000, 380.0, 38.07, 8.91, 2.673, 0.95, 50.0, 8.91, 9885.41, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790449200000, 380.0, 39.28, 8.91, 2.673, 0.95, 50.0, 8.91, 9894.32, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790452800000, 380.0, 34.42, 8.91, 2.673, 0.95, 50.0, 8.91, 9903.23, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790456400000, 380.0, 23.76, 5.94, 1.782, 0.95, 50.0, 5.94, 9909.17, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790460000000, 380.0, 24.57, 5.94, 1.782, 0.95, 50.0, 5.94, 9915.11, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790463600000, 380.0, 25.38, 5.94, 1.782, 0.95, 50.0, 5.94, 9921.05, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003';

INSERT INTO energy_reading (`tenant_id`,`meter_id`,`meter_code`,`meter_type`,`reading_time`,`voltage`,`current`,`active_power`,`reactive_power`,`power_factor`,`frequency`,`energy_consumption`,`cumulative_energy`,`peak_flag`)
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790467200000, 380.0, 14.03, 3.63, 1.089, 0.95, 50.0, 3.63, 9924.68, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790470800000, 380.0, 14.52, 3.63, 1.089, 0.95, 50.0, 3.63, 9928.31, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790474400000, 380.0, 15.01, 3.63, 1.089, 0.95, 50.0, 3.63, 9931.94, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790478000000, 380.0, 15.51, 3.63, 1.089, 0.95, 50.0, 3.63, 9935.57, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790481600000, 380.0, 16.0, 3.63, 1.089, 0.95, 50.0, 3.63, 9939.2, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790485200000, 380.0, 14.03, 3.63, 1.089, 0.95, 50.0, 3.63, 9942.83, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790488800000, 380.0, 14.52, 3.63, 1.089, 0.95, 50.0, 3.63, 9946.46, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790492400000, 380.0, 24.57, 5.94, 1.782, 0.95, 50.0, 5.94, 9952.4, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790496000000, 380.0, 38.07, 8.91, 2.673, 0.95, 50.0, 8.91, 9961.31, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790499600000, 380.0, 39.28, 8.91, 2.673, 0.95, 50.0, 8.91, 9970.22, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790503200000, 380.0, 34.42, 8.91, 2.673, 0.95, 50.0, 8.91, 9979.13, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790506800000, 380.0, 23.76, 5.94, 1.782, 0.95, 50.0, 5.94, 9985.07, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790510400000, 380.0, 24.57, 5.94, 1.782, 0.95, 50.0, 5.94, 9991.01, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790514000000, 380.0, 25.38, 5.94, 1.782, 0.95, 50.0, 5.94, 9996.95, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790517600000, 380.0, 26.19, 5.94, 1.782, 0.95, 50.0, 5.94, 10002.89, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790521200000, 380.0, 22.95, 5.94, 1.782, 0.95, 50.0, 5.94, 10008.83, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790524800000, 380.0, 23.76, 5.94, 1.782, 0.95, 50.0, 5.94, 10014.77, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790528400000, 380.0, 24.57, 5.94, 1.782, 0.95, 50.0, 5.94, 10020.71, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790532000000, 380.0, 38.07, 8.91, 2.673, 0.95, 50.0, 8.91, 10029.62, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790535600000, 380.0, 39.28, 8.91, 2.673, 0.95, 50.0, 8.91, 10038.53, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790539200000, 380.0, 34.42, 8.91, 2.673, 0.95, 50.0, 8.91, 10047.44, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790542800000, 380.0, 23.76, 5.94, 1.782, 0.95, 50.0, 5.94, 10053.38, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790546400000, 380.0, 24.57, 5.94, 1.782, 0.95, 50.0, 5.94, 10059.32, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790550000000, 380.0, 25.38, 5.94, 1.782, 0.95, 50.0, 5.94, 10065.26, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790553600000, 380.0, 14.03, 3.63, 1.089, 0.95, 50.0, 3.63, 10068.89, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790557200000, 380.0, 14.52, 3.63, 1.089, 0.95, 50.0, 3.63, 10072.52, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790560800000, 380.0, 15.01, 3.63, 1.089, 0.95, 50.0, 3.63, 10076.15, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790564400000, 380.0, 15.51, 3.63, 1.089, 0.95, 50.0, 3.63, 10079.78, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790568000000, 380.0, 16.0, 3.63, 1.089, 0.95, 50.0, 3.63, 10083.41, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790571600000, 380.0, 14.03, 3.63, 1.089, 0.95, 50.0, 3.63, 10087.04, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790575200000, 380.0, 14.52, 3.63, 1.089, 0.95, 50.0, 3.63, 10090.67, 2 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790578800000, 380.0, 24.57, 5.94, 1.782, 0.95, 50.0, 5.94, 10096.61, 0 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790582400000, 380.0, 38.07, 8.91, 2.673, 0.95, 50.0, 8.91, 10105.52, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790586000000, 380.0, 39.28, 8.91, 2.673, 0.95, 50.0, 8.91, 10114.43, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_003', 'ELECTRICITY', 1790589600000, 380.0, 34.42, 8.91, 2.673, 0.95, 50.0, 8.91, 10123.34, 1 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003';

-- ---------- 能耗日报 ----------
DELETE FROM energy_report WHERE tenant_id=1 AND report_type='DAILY' AND meter_code IN ('EM_001','EM_002','EM_003');
INSERT INTO energy_report (`tenant_id`,`meter_id`,`meter_code`,`report_type`,`report_date`,`total_energy`,`peak_energy`,`valley_energy`,`flat_energy`,`max_demand`,`avg_power_factor`,`yoy_ratio`,`mom_ratio`,`cost`)
SELECT 1, m.id, 'EM_001', 'DAILY', '2026-09-22', 920, 349.6, 202.4, 368.0, 76.7, 0.91, NULL, NULL, 631.12 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_002', 'DAILY', '2026-09-22', 310, 117.8, 68.2, 124.0, 25.8, 0.91, NULL, NULL, 212.66 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_003', 'DAILY', '2026-09-22', 145, 55.1, 31.9, 58.0, 12.1, 0.91, NULL, NULL, 99.47 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_001', 'DAILY', '2026-09-23', 920, 349.6, 202.4, 368.0, 76.7, 0.91, NULL, NULL, 631.12 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_002', 'DAILY', '2026-09-23', 310, 117.8, 68.2, 124.0, 25.8, 0.91, NULL, NULL, 212.66 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_003', 'DAILY', '2026-09-23', 145, 55.1, 31.9, 58.0, 12.1, 0.91, NULL, NULL, 99.47 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_001', 'DAILY', '2026-09-24', 920, 349.6, 202.4, 368.0, 76.7, 0.91, NULL, NULL, 631.12 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_002', 'DAILY', '2026-09-24', 310, 117.8, 68.2, 124.0, 25.8, 0.91, NULL, NULL, 212.66 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_003', 'DAILY', '2026-09-24', 145, 55.1, 31.9, 58.0, 12.1, 0.91, NULL, NULL, 99.47 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_001', 'DAILY', '2026-09-25', 920, 349.6, 202.4, 368.0, 76.7, 0.91, NULL, NULL, 631.12 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_002', 'DAILY', '2026-09-25', 310, 117.8, 68.2, 124.0, 25.8, 0.91, NULL, NULL, 212.66 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_003', 'DAILY', '2026-09-25', 145, 55.1, 31.9, 58.0, 12.1, 0.91, NULL, NULL, 99.47 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_001', 'DAILY', '2026-09-26', 920, 349.6, 202.4, 368.0, 76.7, 0.91, NULL, NULL, 631.12 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_002', 'DAILY', '2026-09-26', 310, 117.8, 68.2, 124.0, 25.8, 0.91, NULL, NULL, 212.66 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_003', 'DAILY', '2026-09-26', 145, 55.1, 31.9, 58.0, 12.1, 0.91, NULL, NULL, 99.47 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_001', 'DAILY', '2026-09-27', 920, 349.6, 202.4, 368.0, 76.7, 0.91, NULL, NULL, 631.12 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_002', 'DAILY', '2026-09-27', 310, 117.8, 68.2, 124.0, 25.8, 0.91, NULL, NULL, 212.66 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_003', 'DAILY', '2026-09-27', 145, 55.1, 31.9, 58.0, 12.1, 0.91, NULL, NULL, 99.47 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003'
UNION ALL
SELECT 1, m.id, 'EM_001', 'DAILY', '2026-09-28', 920, 349.6, 202.4, 368.0, 76.7, 0.91, NULL, NULL, 631.12 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_001'
UNION ALL
SELECT 1, m.id, 'EM_002', 'DAILY', '2026-09-28', 310, 117.8, 68.2, 124.0, 25.8, 0.91, NULL, NULL, 212.66 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_002'
UNION ALL
SELECT 1, m.id, 'EM_003', 'DAILY', '2026-09-28', 145, 55.1, 31.9, 58.0, 12.1, 0.91, NULL, NULL, 99.47 FROM energy_meter m WHERE m.tenant_id=1 AND m.meter_code='EM_003';


-- ---------- 碳排放 ----------
DELETE FROM carbon_emission WHERE tenant_id=1 AND source_name LIKE '【演示】%';
INSERT INTO carbon_emission (`tenant_id`,`scope`,`source_type`,`source_name`,`consumption`,`consumption_unit`,`emission_factor`,`co2_emission`,`calculation_date`) VALUES
(1,'SCOPE_2','ELECTRICITY','【演示】外购电力',1320,'kWh',0.5810,766.92,'2026-09-15'),
(1,'SCOPE_2','ELECTRICITY','【演示】外购电力',1280,'kWh',0.5810,743.68,'2026-09-16'),
(1,'SCOPE_1','NATURAL_GAS','【演示】锅炉天然气',92,'m³',2.1622,198.92,'2026-09-16'),
(1,'SCOPE_2','ELECTRICITY','【演示】外购电力',1240,'kWh',0.5810,720.44,'2026-09-17'),
(1,'SCOPE_2','ELECTRICITY','【演示】外购电力',1200,'kWh',0.5810,697.2,'2026-09-18'),
(1,'SCOPE_2','ELECTRICITY','【演示】外购电力',1360,'kWh',0.5810,790.16,'2026-09-19'),
(1,'SCOPE_1','NATURAL_GAS','【演示】锅炉天然气',89,'m³',2.1622,192.44,'2026-09-19'),
(1,'SCOPE_2','ELECTRICITY','【演示】外购电力',1320,'kWh',0.5810,766.92,'2026-09-20'),
(1,'SCOPE_2','ELECTRICITY','【演示】外购电力',1280,'kWh',0.5810,743.68,'2026-09-21'),
(1,'SCOPE_2','ELECTRICITY','【演示】外购电力',1240,'kWh',0.5810,720.44,'2026-09-22'),
(1,'SCOPE_1','NATURAL_GAS','【演示】锅炉天然气',86,'m³',2.1622,185.95,'2026-09-22'),
(1,'SCOPE_2','ELECTRICITY','【演示】外购电力',1200,'kWh',0.5810,697.2,'2026-09-23'),
(1,'SCOPE_2','ELECTRICITY','【演示】外购电力',1360,'kWh',0.5810,790.16,'2026-09-24'),
(1,'SCOPE_2','ELECTRICITY','【演示】外购电力',1320,'kWh',0.5810,766.92,'2026-09-25'),
(1,'SCOPE_1','NATURAL_GAS','【演示】锅炉天然气',83,'m³',2.1622,179.46,'2026-09-25'),
(1,'SCOPE_2','ELECTRICITY','【演示】外购电力',1280,'kWh',0.5810,743.68,'2026-09-26'),
(1,'SCOPE_2','ELECTRICITY','【演示】外购电力',1240,'kWh',0.5810,720.44,'2026-09-27'),
(1,'SCOPE_2','ELECTRICITY','【演示】外购电力',1200,'kWh',0.5810,697.2,'2026-09-28'),
(1,'SCOPE_1','NATURAL_GAS','【演示】锅炉天然气',80,'m³',2.1622,172.98,'2026-09-28');

-- 完成
SELECT 'V2 demo seed applied' AS message;
