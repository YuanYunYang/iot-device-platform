package com.iot.platform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.iot.platform.model.dto.LoadDispatchStrategyCreateDTO;
import com.iot.platform.model.entity.LoadDispatchStrategy;
import com.iot.platform.model.vo.LoadDispatchVO;

import java.util.List;

/**
 * 负荷调度服务接口
 * <p>
 * 提供负荷调度策略的创建、自动调度、手动执行与查询能力。
 * 策略执行时通过 MQTT 向目标设备下发控制指令，并将执行记录写入 Redis。
 *
 * @author iot-platform
 */
public interface LoadDispatchService extends IService<LoadDispatchStrategy> {

    /**
     * 创建负荷调度策略
     *
     * @param dto 策略创建参数
     * @return 已创建的策略实体
     */
    LoadDispatchStrategy createStrategy(LoadDispatchStrategyCreateDTO dto);

    /**
     * 手动执行调度策略，通过 MQTT 下发控制指令
     *
     * @param strategyId 策略ID
     */
    void executeStrategy(Long strategyId);

    /**
     * 自动调度检查：遍历已启用策略，校验触发条件并执行满足条件的策略（由定时调度调用）
     */
    void executeAutoDispatch();

    /**
     * 分页查询调度策略
     *
     * @param strategyType 策略类型过滤（可空）
     * @param page         页码（从1开始）
     * @param size         每页条数
     * @return 策略视图列表
     */
    List<LoadDispatchVO> listStrategies(String strategyType, int page, int size);

    /**
     * 启用或禁用策略
     *
     * @param id      策略ID
     * @param enabled 是否启用
     */
    void enableStrategy(Long id, boolean enabled);

    /**
     * 删除策略
     *
     * @param id 策略ID
     */
    void deleteStrategy(Long id);

    /**
     * 分页查询执行历史
     *
     * @param page 页码（从1开始）
     * @param size 每页条数
     * @return 策略视图列表（含执行次数与最后执行时间）
     */
    List<LoadDispatchVO> getExecutionHistory(int page, int size);
}
