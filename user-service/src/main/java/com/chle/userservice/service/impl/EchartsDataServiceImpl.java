package com.chle.userservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chle.userservice.domain.EchartsData;
import com.chle.userservice.domain.PlanList;
import com.chle.userservice.domain.vo.EchartsDataVO;
import com.chle.userservice.mapper.EchartsDataMapper;
import com.chle.userservice.mapper.PlanListMapper;
import com.chle.userservice.service.EchartsDataService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;
import java.util.List;


/**
 * @author CHEN
 * @description 针对表【echarts_data】的数据库操作Service实现
 * @createDate 2026-08-11 17:24:18
 */
@Service
public class EchartsDataServiceImpl extends ServiceImpl<EchartsDataMapper, EchartsData>
        implements EchartsDataService {

    private final PlanListMapper planListMapper;

    public EchartsDataServiceImpl(PlanListMapper planListMapper) {
        this.planListMapper = planListMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<EchartsDataVO> getTotalData(Long userId) {
        // 1. 时间范围：今日 00:00:00 ~ 23:59:59
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1).minusNanos(1);

        // 2. 统计今日计划完成情况（一次查询获取总数和完成数）
        TodayPlanStatistics statistics = calculateTodayStatistics(userId, todayStart, todayEnd);

        // 3. 计算并保存今日完成率（避免除零）
        double completionRate = statistics.total == 0 ? 0.0
                : (double) statistics.completed * 100 / statistics.total;
        saveTodayCompletionRate(userId, completionRate, todayStart);

        // 4. 查询本周完整数据（包含今日新保存的记录）
        LocalDateTime weekStart = LocalDateTime.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .toLocalDate().atStartOfDay();
        LocalDateTime weekEnd = LocalDateTime.now()
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                .toLocalDate().atTime(23, 59, 59);

        List<EchartsData> weekData = baseMapper.selectList(
                new LambdaQueryWrapper<EchartsData>()
                        .eq(EchartsData::getUserId, userId)   // 修正为 getUserId
                        .between(EchartsData::getDataTime, weekStart, weekEnd)
        );

        // 5. 转换为 VO（空集合安全处理）
        return weekData.stream()
                .map(this::convertToVO)
                .toList();
    }

    /**
     * 统计今日计划总数和已完成数（一次性查询）
     */
    private TodayPlanStatistics calculateTodayStatistics(Long userId, LocalDateTime start, LocalDateTime end) {
        // 使用 MyBatis-Plus 条件构造器，同时查询总记录和已完成记录
        // 注意：这里用两个查询，但实际可优化为一条 SQL（使用 SELECT COUNT 和条件聚合），但 MP 不支持直接聚合，
        // 为了简洁且性能尚可（两次查询），也可以接受。或者使用自定义 SQL，但此处保持清晰。
        long total = planListMapper.selectCount(
                new LambdaQueryWrapper<PlanList>()
                        .eq(PlanList::getUserId, userId)
                        .between(PlanList::getDeadLine, start, end)
                // 注意：如果业务要求必须同时满足开始时间也在今日，请保留；否则只按截止时间统计更合理
                // 这里假设以截止时间为准，去掉 startTime 条件（视业务而定，可注释或保留）
                // 如果必须同时满足，则保留下面这行
                // .between(PlanList::getStartTime, start, end)
        );

        long completed = planListMapper.selectCount(
                new LambdaQueryWrapper<PlanList>()
                        .eq(PlanList::getUserId, userId)
                        .eq(PlanList::getStatus, 1)
                        .between(PlanList::getDeadLine, start, end)
        );

        return new TodayPlanStatistics(total, completed);
    }

    /**
     * 保存今日完成率（若已存在则更新，否则插入）
     */
    private void saveTodayCompletionRate(Long userId, double rate, LocalDateTime todayStart) {
        // 查询今日是否存在记录（使用 between 更精确）
        EchartsData existing = baseMapper.selectOne(
                new LambdaQueryWrapper<EchartsData>()
                        .eq(EchartsData::getUserId, userId)
                        .between(EchartsData::getDataTime, todayStart, todayStart.plusDays(1).minusNanos(1))
        );

        EchartsData data = (existing != null) ? existing : new EchartsData();
        data.setUserId(userId);
        data.setCompletionRate((int) Math.round(rate)); // 保留整数百分比
        data.setDataTime(new Date());

        // 若实体已存在则更新，否则插入
        saveOrUpdate(data);
    }

    /**
     * 实体转 VO
     */
    private EchartsDataVO convertToVO(EchartsData data) {
        EchartsDataVO vo = new EchartsDataVO();
        vo.setDataTime(data.getDataTime());
        vo.setCompletionRate(String.valueOf(data.getCompletionRate()));
        return vo;
    }

    /**
     * 内部类：今日统计结果
     */
    private record TodayPlanStatistics(long total, long completed) {
    }

    // ========== 保留原有 saveData 方法（但可弃用，或调整为调用私有方法） ==========
    @Override
    @Deprecated
    public void saveData(double completeRate, Long userId) {
        // 直接调用新方法，避免重复逻辑
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        saveTodayCompletionRate(userId, completeRate, todayStart);
    }
}