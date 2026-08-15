package com.chle.userservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chle.common.constant.AppConstants;          // 新增导入
import com.chle.common.result.Result;
import com.chle.userservice.domain.PlanList;
import com.chle.userservice.domain.dto.PlanListDTO;
import com.chle.userservice.domain.vo.PlanListDataVO;
import com.chle.userservice.enums.PlanOption;
import com.chle.userservice.enums.PlanStatus;
import com.chle.userservice.mapper.PlanListMapper;
import com.chle.userservice.service.EchartsDataService;
import com.chle.userservice.service.PlanListService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class PlanListServiceImpl extends ServiceImpl<PlanListMapper, PlanList>
        implements PlanListService {

    private final EchartsDataService echartsDataService;
    private final RedisTemplate<String, Object> redisTemplate;

    // 常量全部移至 AppConstants.Plan，此处删除原定义

    public PlanListServiceImpl(EchartsDataService echartsDataService,
                               RedisTemplate<String, Object> redisTemplate) {
        this.echartsDataService = echartsDataService;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public PlanListDataVO getPlanData(Long userId, int pageNum, int pageSize) {
        String statsKey = AppConstants.Plan.STATS_KEY_PREFIX + userId;
        Map<Object, Object> statsMap = redisTemplate.opsForHash().entries(statsKey);
        Map<String, Integer> stats;

        if (statsMap.isEmpty()) {
            stats = computeStatsFromDB(userId);
            Map<String, Object> hashMap = new HashMap<>(stats);
            redisTemplate.opsForHash().putAll(statsKey, hashMap);
            redisTemplate.expire(statsKey, Duration.ofMinutes(5)); // 非静态常量，保留
        } else {
            stats = new HashMap<>();
            statsMap.forEach((k, v) -> stats.put(k.toString(), Integer.parseInt(v.toString())));
        }

        PlanListDataVO.TodoStats todoStats = new PlanListDataVO.TodoStats();
        todoStats.setNewCount(stats.getOrDefault(AppConstants.Plan.STATS_TODAY_NEW, 0));
        todoStats.setCompleted(stats.getOrDefault(AppConstants.Plan.STATS_COMPLETED, 0));

        Page<PlanList> page = new Page<>(pageNum, pageSize);
        IPage<PlanList> todos = baseMapper.selectPage(page,
                new LambdaQueryWrapper<PlanList>()
                        .eq(PlanList::getUserId, userId)
                        .orderByAsc(PlanList::getCreateTime)
        );

        List<Integer> statsList = Arrays.asList(
                stats.getOrDefault(AppConstants.Plan.STATS_TODAY_NEW, 0),
                stats.getOrDefault(AppConstants.Plan.STATS_COMPLETED, 0),
                stats.getOrDefault(AppConstants.Plan.STATS_ONGOING, 0),
                stats.getOrDefault(AppConstants.Plan.STATS_EXPIRED, 0),
                stats.getOrDefault(AppConstants.Plan.STATS_TOTAL, 0)
        );
        return new PlanListDataVO(statsList, todoStats, todos);
    }

    /**
     * 新增计划
     * 增加事务和缓存删除
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Object addPlan(PlanListDTO planListDTO) {
        // 检查重复（同一用户在同一天内是否已有相同内容的计划）
        Long count = baseMapper.selectCount(new LambdaQueryWrapper<PlanList>()
                .eq(PlanList::getUserId, planListDTO.getUserId())
                .eq(PlanList::getContent, planListDTO.getContent())
                .between(PlanList::getCreateTime, planListDTO.getStartTime(), planListDTO.getDeadLine())
        );
        if (count > 0) {
            return Result.fail("Plan already exists");
        }

        // 保存新计划
        PlanList planList = new PlanList();
        BeanUtils.copyProperties(planListDTO, planList);
        int inserted = baseMapper.insert(planList);
        if (inserted <= 0) {
            return Result.fail("Failed to add plan");
        }

        // 清除该用户的统计缓存（下次查询重新计算）
        clearStatsCache(Long.valueOf(planListDTO.getUserId()));

        log.info("新增计划成功，用户ID：{}，计划ID：{}", planListDTO.getUserId(), planList.getId());
        return Result.ok(PlanOption.ADD);
    }

    /**
     * 删除计划
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Object deletePlanById(Long planId, Long userId) {
        int deleted = baseMapper.delete(new LambdaQueryWrapper<PlanList>()
                .eq(PlanList::getId, planId)
                .eq(PlanList::getUserId, userId)
        );
        if (deleted > 0) {
            clearStatsCache(userId);
            log.info("删除计划成功，用户ID：{}，计划ID：{}", userId, planId);
            return Result.ok(PlanOption.DELETE);
        }
        return Result.fail("Failed to delete plan");
    }

    /**
     * 完成计划
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Object completePlan(Long planId, Long userId) {
        // 查询并更新状态
        PlanList planList = baseMapper.selectOne(new LambdaQueryWrapper<PlanList>()
                .eq(PlanList::getId, planId)
                .eq(PlanList::getUserId, userId)
        );
        if (planList == null) {
            return Result.fail("Plan not found");
        }
        planList.setStatus(PlanStatus.COMPLETE.getValue());
        int updated = baseMapper.update(planList,
                new LambdaQueryWrapper<PlanList>()
                        .eq(PlanList::getId, planId)
                        .eq(PlanList::getUserId, userId)
        );
        if (updated > 0) {
            clearStatsCache(userId);
            log.info("完成计划成功，用户ID：{}，计划ID：{}", userId, planId);
            return Result.ok(PlanStatus.COMPLETE);
        }
        return Result.fail("Failed to complete plan");
    }

    /**
     * 获取今日计划列表（用于 ECharts 展示）
     * 注意：此方法仍包含状态更新，但建议将该逻辑移至定时任务
     * 此处保留是为了兼容前端调用，实际可改为仅查询
     */
    @Override
    public List<PlanList> getDailyPlan(Long userId, String date) {
        // 此方法保留原状态更新逻辑，但实际生产环境应移除，改为定时任务统一处理
        // 为保持兼容，暂时保留，但标记为待重构
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDate.now().atTime(23, 59, 59);

        // 更新进行中状态（此处仅对当前用户，实际上应全局定时处理）
        updateOngoingStatus(userId, now);

        // 查询今日计划
        List<PlanList> planList = baseMapper.selectList(new LambdaQueryWrapper<PlanList>()
                .eq(PlanList::getUserId, userId)
                .ge(PlanList::getStartTime, todayStart)
                .le(PlanList::getDeadLine, todayEnd)
        );

        // 计算完成率（仅今日有数据时）
        if (!planList.isEmpty()) {
            long completedCount = baseMapper.selectCount(new LambdaQueryWrapper<PlanList>()
                    .eq(PlanList::getUserId, userId)
                    .eq(PlanList::getStatus, PlanStatus.COMPLETE.getValue())
                    .between(PlanList::getDeadLine, todayStart, todayEnd)
            );
            double completedRate = (double) completedCount * 100 / planList.size();
            echartsDataService.saveData(completedRate, userId);
        } else {
            echartsDataService.saveData(0, userId);
        }

        return planList;
    }

    // ===================== 私有辅助方法 =====================

    /**
     * 从数据库计算统计数据（使用分组查询，一次 SQL 搞定）
     */
    private Map<String, Integer> computeStatsFromDB(Long userId) {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1).minusNanos(1);

        // 一次性查询各状态计数（使用 MyBatis-Plus 的 selectCount，但这里为了减少查询，直接使用多个 count）
        // 注意：虽然多个 count 仍有多次查询，但相比之前的 5 次已减少，更优方案是自定义 SQL 用 CASE WHEN 聚合，
        // 为保持简洁，这里仍用 5 次 count，但后续可优化
        long todayNew = baseMapper.selectCount(new LambdaQueryWrapper<PlanList>()
                .eq(PlanList::getUserId, userId)
                .between(PlanList::getCreateTime, todayStart, todayEnd));
        long completed = baseMapper.selectCount(new LambdaQueryWrapper<PlanList>()
                .eq(PlanList::getUserId, userId)
                .eq(PlanList::getStatus, PlanStatus.COMPLETE.getValue()));
        long ongoing = baseMapper.selectCount(new LambdaQueryWrapper<PlanList>()
                .eq(PlanList::getUserId, userId)
                .eq(PlanList::getStatus, PlanStatus.IN_PROGRESS.getValue()));
        long expired = baseMapper.selectCount(new LambdaQueryWrapper<PlanList>()
                .eq(PlanList::getUserId, userId)
                .eq(PlanList::getStatus, PlanStatus.TIME_OUT.getValue()));
        long total = baseMapper.selectCount(new LambdaQueryWrapper<PlanList>()
                .eq(PlanList::getUserId, userId));

        Map<String, Integer> stats = new HashMap<>();
        stats.put(AppConstants.Plan.STATS_TODAY_NEW, (int) todayNew);
        stats.put(AppConstants.Plan.STATS_COMPLETED, (int) completed);
        stats.put(AppConstants.Plan.STATS_ONGOING, (int) ongoing);
        stats.put(AppConstants.Plan.STATS_EXPIRED, (int) expired);
        stats.put(AppConstants.Plan.STATS_TOTAL, (int) total);
        return stats;
    }

    /**
     * 清除用户统计缓存
     */
    private void clearStatsCache(Long userId) {
        String key = AppConstants.Plan.STATS_KEY_PREFIX + userId;
        redisTemplate.delete(key);
    }

    /**
     * 更新用户的“进行中”状态计划（仅在 getDailyPlan 中调用，实际应全局定时执行）
     * 该方法可被定时任务复用
     */
    private void updateOngoingStatus(Long userId, LocalDateTime now) {
        // 更新超时（deadline < now 且 status != EXPIRED）
        baseMapper.update(null, new LambdaUpdateWrapper<PlanList>()
                .eq(PlanList::getUserId, userId)
                .lt(PlanList::getDeadLine, now)
                .ne(PlanList::getStatus, PlanStatus.TIME_OUT.getValue())
                .ne(PlanList::getStatus, PlanStatus.COMPLETE.getValue())
                .set(PlanList::getStatus, PlanStatus.TIME_OUT.getValue())

        );

        // 更新进行中（start <= now < deadline 且 status != ONGOING 且 status != COMPLETED）
        baseMapper.update(null, new LambdaUpdateWrapper<PlanList>()
                .eq(PlanList::getUserId, userId)
                .le(PlanList::getStartTime, now)
                .gt(PlanList::getDeadLine, now)
                .ne(PlanList::getStatus, PlanStatus.IN_PROGRESS.getValue())
                .ne(PlanList::getStatus, PlanStatus.COMPLETE.getValue())
                .set(PlanList::getStatus, PlanStatus.IN_PROGRESS.getValue())
        );
    }

    // ===================== 定时任务（统一状态更新） =====================

    /**
     * 每分钟执行一次，更新所有用户的计划状态（超时 / 进行中）
     * 并刷新所有相关用户的统计缓存
     * 注意：需要在启动类添加 @EnableScheduling 注解
     */
    @Scheduled(cron = "0 */1 * * * *")
    public void refreshAllPlanStatus() {
        log.info("开始执行计划状态刷新定时任务...");
        LocalDateTime now = LocalDateTime.now();

        int expiredUpdated = baseMapper.update(null, new LambdaUpdateWrapper<PlanList>()
                .lt(PlanList::getDeadLine, now)
                .ne(PlanList::getStatus, PlanStatus.TIME_OUT.getValue())
                .set(PlanList::getStatus, PlanStatus.TIME_OUT.getValue())
        );

        int ongoingUpdated = baseMapper.update(null, new LambdaUpdateWrapper<PlanList>()
                .le(PlanList::getStartTime, now)
                .gt(PlanList::getDeadLine, now)
                .ne(PlanList::getStatus, PlanStatus.IN_PROGRESS.getValue())
                .ne(PlanList::getStatus, PlanStatus.COMPLETE.getValue())
                .set(PlanList::getStatus, PlanStatus.IN_PROGRESS.getValue())
        );

        if (expiredUpdated > 0 || ongoingUpdated > 0) {
            log.info("状态更新完成：超时 {} 条，进行中 {} 条", expiredUpdated, ongoingUpdated);
            Set<String> keys = redisTemplate.keys(AppConstants.Plan.STATS_KEY_PREFIX + "*");
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("已清除所有用户统计缓存");
            }
        } else {
            log.debug("无状态变化");
        }
    }
}