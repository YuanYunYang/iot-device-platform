package com.iot.platform.repository;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iot.platform.model.entity.OtaTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * OTA 升级任务 Mapper 接口
 * <p>
 * 继承 MyBatis-Plus BaseMapper，提供任务标准 CRUD（受多租户过滤保护）；
 * 另提供设备进度上报链路使用的跨租户查询/统计方法（标注 {@code @InterceptorIgnore}），
 * 因进度上报发生在 MQTT 异步回调线程，无租户上下文。
 *
 * @author iot-platform
 */
@Mapper
public interface OtaTaskMapper extends BaseMapper<OtaTask> {

    /**
     * 跨租户按主键查询任务（供异步进度处理使用）
     *
     * @param id 任务ID
     * @return 任务实体
     */
    @InterceptorIgnore(tenantLine = "1")
    @Select("SELECT * FROM ota_task WHERE id = #{id} AND deleted = 0 LIMIT 1")
    OtaTask selectByIdIgnoreTenant(@Param("id") Long id);

    /**
     * 成功数 +1
     *
     * @param taskId 任务ID
     * @return 受影响行数
     */
    @InterceptorIgnore(tenantLine = "1")
    @Update("UPDATE ota_task SET success_count = success_count + 1, update_time = NOW() " +
            "WHERE id = #{taskId} AND deleted = 0")
    int incrementSuccessCount(@Param("taskId") Long taskId);

    /**
     * 失败数 +1
     *
     * @param taskId 任务ID
     * @return 受影响行数
     */
    @InterceptorIgnore(tenantLine = "1")
    @Update("UPDATE ota_task SET fail_count = fail_count + 1, update_time = NOW() " +
            "WHERE id = #{taskId} AND deleted = 0")
    int incrementFailCount(@Param("taskId") Long taskId);

    /**
     * 跨租户更新任务状态（进度处理链路使用）
     *
     * @param taskId 任务ID
     * @param status 目标状态
     * @return 受影响行数
     */
    @InterceptorIgnore(tenantLine = "1")
    @Update("UPDATE ota_task SET status = #{status}, update_time = NOW() " +
            "WHERE id = #{taskId} AND deleted = 0")
    int updateStatusIgnoreTenant(@Param("taskId") Long taskId, @Param("status") String status);
}
