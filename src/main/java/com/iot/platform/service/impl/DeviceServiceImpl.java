package com.iot.platform.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.iot.platform.common.GlobalExceptionHandler;
import com.iot.platform.common.ResultCode;
import com.iot.platform.config.RedisConfig;
import com.iot.platform.model.dto.DeviceRegisterDTO;
import com.iot.platform.model.entity.Device;
import com.iot.platform.model.entity.Product;
import com.iot.platform.model.enums.DeviceStatusEnum;
import com.iot.platform.model.vo.DeviceVO;
import com.iot.platform.repository.DeviceMapper;
import com.iot.platform.repository.ProductMapper;
import com.iot.platform.service.DeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 设备服务实现
 * <p>
 * 实现设备注册、查询、状态更新、心跳维护等核心逻辑。
 * 设备在线状态同时写入 MySQL 与 Redis 缓存，热数据优先读取 Redis。
 *
 * @author iot-platform
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceServiceImpl extends ServiceImpl<DeviceMapper, Device> implements DeviceService {

    private final DeviceMapper deviceMapper;
    private final ProductMapper productMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public DeviceVO register(DeviceRegisterDTO dto) {
        // 校验设备是否已存在
        Device exist = deviceMapper.selectByDeviceId(dto.getDeviceId());
        if (exist != null) {
            throw new GlobalExceptionHandler.BusinessException(
                    ResultCode.DEVICE_ALREADY_EXISTS, "设备已存在: " + dto.getDeviceId());
        }

        // 校验产品是否存在
        Product product = productMapper.selectByProductKey(dto.getProductKey());
        if (product == null) {
            throw new GlobalExceptionHandler.BusinessException(
                    ResultCode.PRODUCT_NOT_FOUND, "产品不存在: " + dto.getProductKey());
        }

        // 构建设备实体
        Device device = new Device();
        device.setDeviceId(dto.getDeviceId());
        device.setDeviceName(dto.getDeviceName());
        device.setProductId(product.getId());
        device.setProductKey(product.getProductKey());
        // 生成设备密钥（MD5 摘要）
        device.setDeviceSecret(SecureUtil.md5(dto.getDeviceId() + System.currentTimeMillis()));
        device.setStatus(DeviceStatusEnum.UNKNOWN.getCode());
        device.setLocation(dto.getLocation());
        device.setRemark(dto.getRemark());

        deviceMapper.insert(device);
        log.info("设备注册成功: deviceId={}, productKey={}", dto.getDeviceId(), dto.getProductKey());

        return convertToVO(device, product.getProductName());
    }

    @Override
    public DeviceVO getDeviceByDeviceId(String deviceId) {
        Device device = deviceMapper.selectByDeviceId(deviceId);
        if (device == null) {
            throw new GlobalExceptionHandler.BusinessException(
                    ResultCode.DEVICE_NOT_FOUND, "设备不存在: " + deviceId);
        }
        // 优先从 Redis 读取实时状态
        String status = getCachedStatus(deviceId);
        if (status != null) {
            device.setStatus(status);
        }
        return convertToVO(device, getProductName(device.getProductId()));
    }

    @Override
    public List<DeviceVO> listDevices(String status, int page, int size) {
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(status)) {
            wrapper.eq(Device::getStatus, status);
        }
        wrapper.orderByDesc(Device::getCreateTime);

        Page<Device> pageResult = deviceMapper.selectPage(new Page<>(page, size), wrapper);
        return pageResult.getRecords().stream()
                .map(device -> convertToVO(device, getProductName(device.getProductId())))
                .collect(Collectors.toList());
    }

    @Override
    public void updateDeviceStatus(String deviceId, String status, String clientIp) {
        Device device = deviceMapper.selectByDeviceId(deviceId);
        if (device == null) {
            log.warn("更新设备状态失败，设备不存在: {}", deviceId);
            throw new GlobalExceptionHandler.BusinessException(
                    ResultCode.DEVICE_NOT_FOUND, "设备不存在: " + deviceId);
        }

        Device update = new Device();
        update.setId(device.getId());
        update.setStatus(status);
        update.setIpAddress(clientIp);
        update.setLastHeartbeatTime(LocalDateTime.now());

        if (DeviceStatusEnum.ONLINE.getCode().equals(status)) {
            update.setLastOnlineTime(LocalDateTime.now());
        } else if (DeviceStatusEnum.OFFLINE.getCode().equals(status)) {
            update.setLastOfflineTime(LocalDateTime.now());
        }

        deviceMapper.updateById(update);

        // 同步状态到 Redis 缓存
        cacheDeviceStatus(deviceId, status);
        log.info("设备状态已更新: deviceId={}, status={}", deviceId, status);
    }

    @Override
    public void updateHeartbeat(String deviceId) {
        Device device = deviceMapper.selectByDeviceId(deviceId);
        if (device == null) {
            log.debug("心跳更新跳过，设备未注册: {}", deviceId);
            return;
        }

        // 更新数据库心跳时间
        Device update = new Device();
        update.setId(device.getId());
        update.setLastHeartbeatTime(LocalDateTime.now());
        // 心跳到达视为在线
        update.setStatus(DeviceStatusEnum.ONLINE.getCode());
        deviceMapper.updateById(update);

        // 更新 Redis 心跳时间戳与在线状态
        redisTemplate.opsForValue().set(RedisConfig.DEVICE_HEARTBEAT_KEY + deviceId,
                LocalDateTime.now().toEpochSecond(ZoneOffset.of("+8")), 300, TimeUnit.SECONDS);
        cacheDeviceStatus(deviceId, DeviceStatusEnum.ONLINE.getCode());
    }

    @Override
    public void deleteDevice(String deviceId) {
        Device device = deviceMapper.selectByDeviceId(deviceId);
        if (device == null) {
            throw new GlobalExceptionHandler.BusinessException(
                    ResultCode.DEVICE_NOT_FOUND, "设备不存在: " + deviceId);
        }
        deviceMapper.deleteById(device.getId());
        // 清理 Redis 缓存
        redisTemplate.delete(RedisConfig.DEVICE_STATUS_KEY + deviceId);
        redisTemplate.delete(RedisConfig.DEVICE_HEARTBEAT_KEY + deviceId);
        log.info("设备已删除: deviceId={}", deviceId);
    }

    @Override
    public int markTimeoutDevicesOffline(int timeoutSeconds) {
        int count = deviceMapper.updateOfflineByTimeout(timeoutSeconds);
        if (count > 0) {
            log.info("检测到 {} 台设备心跳超时，已标记为离线", count);
        }
        return count;
    }

    @Override
    public LocalDateTime getLastHeartbeat(String deviceId) {
        Object ts = redisTemplate.opsForValue().get(RedisConfig.DEVICE_HEARTBEAT_KEY + deviceId);
        if (ts != null) {
            try {
                long epoch = Long.parseLong(ts.toString());
                return LocalDateTime.ofEpochSecond(epoch, 0, ZoneOffset.of("+8"));
            } catch (NumberFormatException e) {
                // 回退到数据库查询
            }
        }
        Device device = deviceMapper.selectByDeviceId(deviceId);
        return device == null ? null : device.getLastHeartbeatTime();
    }

    /**
     * 缓存设备状态到 Redis
     */
    private void cacheDeviceStatus(String deviceId, String status) {
        redisTemplate.opsForValue().set(RedisConfig.DEVICE_STATUS_KEY + deviceId, status, 300, TimeUnit.SECONDS);
    }

    /**
     * 获取缓存的设备状态
     */
    private String getCachedStatus(String deviceId) {
        Object status = redisTemplate.opsForValue().get(RedisConfig.DEVICE_STATUS_KEY + deviceId);
        return status == null ? null : status.toString();
    }

    /**
     * 获取产品名称
     */
    private String getProductName(Long productId) {
        if (productId == null) {
            return null;
        }
        Product product = productMapper.selectById(productId);
        return product == null ? null : product.getProductName();
    }

    /**
     * 实体转 VO
     */
    private DeviceVO convertToVO(Device device, String productName) {
        DeviceVO vo = new DeviceVO();
        vo.setId(device.getId());
        vo.setDeviceId(device.getDeviceId());
        vo.setDeviceName(device.getDeviceName());
        vo.setProductId(device.getProductId());
        vo.setProductKey(device.getProductKey());
        vo.setProductName(productName);
        vo.setStatus(device.getStatus());
        vo.setStatusDesc(DeviceStatusEnum.of(device.getStatus()).getDesc());
        vo.setFirmwareVersion(device.getFirmwareVersion());
        vo.setIpAddress(device.getIpAddress());
        vo.setLastOnlineTime(device.getLastOnlineTime());
        vo.setLastHeartbeatTime(device.getLastHeartbeatTime());
        vo.setLocation(device.getLocation());
        vo.setRemark(device.getRemark());
        vo.setCreateTime(device.getCreateTime());
        return vo;
    }
}
