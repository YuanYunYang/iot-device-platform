package com.iot.platform.repository;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iot.platform.model.entity.AlarmRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 告警规则 Mapper 接口
 * <p>
 * 继承 MyBatis-Plus BaseMapper，提供规则标准 CRUD（受多租户过滤保护）；
 * 另提供跨租户加载全部启用规则的方法（供规则引擎重新加载时使用，
 * 因规则引擎在 MQTT 异步回调线程执行，无租户上下文）。
 *
 * @author iot-platform
 */
@Mapper
public interface AlarmRuleMapper extends BaseMapper<AlarmRule> {

    /**
     * 跨租户查询全部启用的规则（规则引擎重载时使用）
     *
     * @return 启用规则列表
     */
    @InterceptorIgnore(tenantLine = "1")
    @Select("SELECT * FROM alarm_rule WHERE enabled = 1 AND deleted = 0")
    List<AlarmRule> selectAllEnabled();
}
