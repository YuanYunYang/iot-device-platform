package com.iot.platform.controller;

import com.iot.platform.service.MqttAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * EMQX HTTP 认证回调 Controller
 * <p>
 * 对接 EMQX 的 HTTP Auth 与 ACL 插件，提供 per-device 身份认证与权限校验。
 * 返回体统一为 {@code {"result": "allow"}} 或 {@code {"result": "deny"}}，
 * 不使用平台统一的 Result 包装，以符合 EMQX 插件协议约定。
 *
 * @author iot-platform
 */
@Slf4j
@RestController
@RequestMapping("/mqtt")
@RequiredArgsConstructor
@Tag(name = "EMQX认证", description = "EMQX HTTP Auth/ACL 回调接口")
public class MqttAuthController {

    private final MqttAuthService mqttAuthService;

    /**
     * EMQX 认证回调
     * <p>
     * EMQX 在设备建立连接前以 POST 方式回调，请求体为
     * {@code {"clientid":"...","username":"deviceId","password":"deviceSecret"}}。
     *
     * @param params EMQX 透传的认证参数
     * @return {@code {"result":"allow"}} 或 {@code {"result":"deny"}}
     */
    @Operation(summary = "EMQX认证回调", description = "校验设备 deviceId + deviceSecret")
    @PostMapping("/auth")
    public Map<String, Object> auth(@RequestBody Map<String, Object> params) {
        boolean allow = mqttAuthService.authenticate(params);
        return Map.<String, Object>of("result", allow ? "allow" : "deny");
    }

    /**
     * EMQX ACL 鉴权回调
     * <p>
     * EMQX 在设备发布/订阅前以 POST 方式回调，请求体为
     * {@code {"clientid":"...","username":"deviceId","topic":"...","access":"1|2"}}。
     *
     * @param params EMQX 透传的鉴权参数
     * @return {@code {"result":"allow"}} 或 {@code {"result":"deny"}}
     */
    @Operation(summary = "EMQX ACL回调", description = "校验设备是否有权发布/订阅指定 Topic")
    @PostMapping("/acl")
    public Map<String, Object> acl(@RequestBody Map<String, Object> params) {
        boolean allow = mqttAuthService.checkAcl(params);
        return Map.<String, Object>of("result", allow ? "allow" : "deny");
    }
}
