package com.iot.platform.config;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * InfluxDB 2.0 配置
 * <p>
 * 创建 InfluxDB 客户端 Bean，用于写入与查询设备时序数据。
 * 设备上报的属性数据按 measurement=device_data 写入指定 bucket。
 * <p>
 * 采用构造函数 @Value 注入，连接参数作为 final 字段暴露给业务层使用。
 *
 * @author iot-platform
 */
@Slf4j
@Getter
@Configuration
public class InfluxDBConfig {

    /** InfluxDB 服务地址 */
    private final String url;

    /** 访问令牌 */
    private final String token;

    /** 组织（避免命名为 org，否则会遮蔽 org.slf4j 包，导致 @Slf4j 静态日志生成冲突） */
    private final String orgName;

    /** Bucket（存储桶） */
    private final String bucket;

    public InfluxDBConfig(@Value("${influxdb.url}") String url,
                          @Value("${influxdb.token}") String token,
                          @Value("${influxdb.org}") String orgName,
                          @Value("${influxdb.bucket}") String bucket) {
        this.url = url;
        this.token = token;
        this.orgName = orgName;
        this.bucket = bucket;
        log.info("InfluxDB 配置加载完成: url={}, org={}, bucket={}", url, orgName, bucket);
    }

    /**
     * 创建 InfluxDB 客户端 Bean
     */
    @Bean
    public InfluxDBClient influxDBClient() {
        log.info("创建 InfluxDB 客户端: org={}, bucket={}", orgName, bucket);
        return InfluxDBClientFactory.create(url, token.toCharArray(), orgName, bucket);
    }
}
