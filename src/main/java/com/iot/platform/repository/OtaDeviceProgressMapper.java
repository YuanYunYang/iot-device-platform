package com.iot.platform.repository;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iot.platform.model.entity.OtaDeviceProgress;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * OTA 设备升级进度 Mapper 接口
 * <p>
 * ota_device_progress 表为任务执行明细，不含 tenant_id 字段，
 * 故在接口级别通过 {@code @InterceptorIgnore} 关闭多租户过滤。
 *
 * @author iot-platform
 */
@Mapper
@InterceptorIgnore(tenantLine = "1")
public interface OtaDeviceProgressMapper extends BaseMapper<OtaDeviceProgress> {

    /**
     * 按任务ID查询全部设备进度
     *
     * @param taskId 任务ID
     * @return 进度列表
     */
    @Select("SELECT * FROM ota_device_progress WHERE task_id = #{taskId} ORDER BY id ASC")
    List<OtaDeviceProgress> selectByTaskId(@Param("taskId") Long taskId);

    /**
     * 按任务ID与设备ID查询单条进度
     *
     * @param taskId   任务ID
     * @param deviceId 设备ID
     * @return 进度实体
     */
    @Select("SELECT * FROM ota_device_progress WHERE task_id = #{taskId} AND device_id = #{deviceId} LIMIT 1")
    OtaDeviceProgress selectByTaskAndDevice(@Param("taskId") Long taskId, @Param("deviceId") String deviceId);

    /**
     * 更新设备升级进度状态
     *
     * @param id           进度主键
     * @param status       升级状态
     * @param errorMsg     失败原因（可为空）
     * @param upgradeTime  升级完成时间（可为空）
     * @return 受影响行数
     */
    @Update("UPDATE ota_device_progress SET status = #{status}, error_msg = #{errorMsg}, " +
            "upgrade_time = #{upgradeTime}, update_time = NOW() WHERE id = #{id}")
    int updateProgress(@Param("id") Long id, @Param("status") String status,
                      @Param("errorMsg") String errorMsg, @Param("upgradeTime") String upgradeTime);
}
