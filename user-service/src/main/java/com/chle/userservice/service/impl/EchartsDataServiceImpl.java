package com.chle.userservice.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.chle.userservice.domain.EchartsData;
import com.chle.userservice.domain.PlanList;
import com.chle.userservice.domain.vo.EchartsDataVO;
import com.chle.userservice.mapper.PlanListMapper;
import com.chle.userservice.service.EchartsDataService;
import com.chle.userservice.mapper.EchartsDataMapper;
import org.springframework.stereotype.Service;

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
        implements EchartsDataService{

    private final PlanListMapper planListMapper;

    // 通过构造器注入（推荐）
    public EchartsDataServiceImpl(PlanListMapper planListMapper) {
        this.planListMapper = planListMapper;
    }

    @Override
    public List<EchartsDataVO> getTotalData(Long userId) {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1).minusNanos(1);
        // 获取【本周周一 00:00:00】
        LocalDateTime weekStart = LocalDateTime.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .toLocalDate().atStartOfDay();

// 获取【本周周日 23:59:59】
        LocalDateTime weekEnd = LocalDateTime.now()
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                .toLocalDate().atTime(23,59,59);
        List<EchartsData> totalData = baseMapper.selectList(new LambdaQueryWrapper<EchartsData>().eq(EchartsData::getId , userId).between(EchartsData::getDataTime, weekStart, weekEnd));

        //统计今日计划完成情况并存入数据库
        // 使用 Db.count 静态方法查询符合条件的记录数
        // 使用 planListMapper 查询今日所有计划
        List<PlanList> planList = planListMapper.selectList(
                new LambdaQueryWrapper<PlanList>()
                        .eq(PlanList::getUserId, userId)
                        .between(PlanList::getDeadLine, todayStart, todayEnd)
                        .between(PlanList::getStartTime, todayStart, todayEnd)
        );
        long completedCount = planListMapper.selectCount(new LambdaQueryWrapper<PlanList>()
                .eq(PlanList::getUserId, userId)
                .eq(PlanList::getStatus, 1)
                .between(PlanList::getDeadLine, todayStart, todayEnd));

// 计算完成率，注意避免整数除法丢失精度
        double completedRate = (double) (completedCount * 100) / planList.size();
        this.saveData(completedRate, userId);
        return totalData.stream().map(echartsData -> {
            EchartsDataVO echartsDataVO = new EchartsDataVO();
            echartsDataVO.setDataTime(echartsData.getDataTime());
            echartsDataVO.setCompletionRate(String.valueOf(echartsData.getCompletionRate()));
            return echartsDataVO;
        }).toList();
    }

    @Override
    public void saveData(double completeRate, Long userId) {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        EchartsData echartsData = baseMapper.selectOne(new LambdaQueryWrapper<EchartsData>().eq(EchartsData::getUserId, userId).between(EchartsData::getDataTime, todayStart, todayStart.plusDays(1)));
        if (echartsData != null) {
            echartsData.setCompletionRate((int) completeRate);
            echartsData.setDataTime(new Date());
        } else {
            echartsData = new EchartsData();
            echartsData.setCompletionRate((int) completeRate);
            echartsData.setUserId(userId);
            echartsData.setDataTime(new Date());
        }
        saveOrUpdate(echartsData);
    }
}




