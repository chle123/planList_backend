package com.chle.userservice.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chle.common.result.Result;
import com.chle.userservice.domain.PlanList;
import com.chle.userservice.domain.dto.PlanListDTO;
import com.chle.userservice.domain.vo.PlanListDataVO;
import com.chle.userservice.enums.PlanOption;
import com.chle.userservice.enums.PlanStatus;
import com.chle.userservice.service.EchartsDataService;
import com.chle.userservice.service.PlanListService;
import com.chle.userservice.mapper.PlanListMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


/**
 * @author CHEN
 * @description 针对表【planlist】的数据库操作Service实现
 * @createDate 2026-08-10 15:44:02
 */
@Service
public class PlanListServiceImpl extends ServiceImpl<PlanListMapper, PlanList>
        implements PlanListService {

    private final EchartsDataService echartsDataService;

    public PlanListServiceImpl(EchartsDataService echartsDataService) {
        this.echartsDataService = echartsDataService;
    }

    @Override
    public PlanListDataVO getPlanData(Long userId,int pageNum,int pageSize) {
        //查询stats
        //1.查询今日计划
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayEnd = LocalDate.now().atTime(23, 59, 59);
        //更新数据状态为超时
        List<PlanList> plans1 = baseMapper.selectList(new LambdaQueryWrapper<PlanList>()
                .eq(PlanList::getUserId, userId)
                .lt(PlanList::getDeadLine, now)
        );
        List<Integer> ids1 = plans1.stream().map(PlanList::getId).toList();
        if (!ids1.isEmpty()) {
            LambdaUpdateWrapper<PlanList> updateWrapper1 = new LambdaUpdateWrapper<>();
            updateWrapper1.in(PlanList::getId, ids1).set(PlanList::getStatus, 2);
            baseMapper.update(null, updateWrapper1);  // 或者 update(实体, updateWrapper)
        }
        //更新数据状态为进行中
        List<PlanList> plans2 = baseMapper.selectList(new LambdaQueryWrapper<PlanList>()
                .eq(PlanList::getUserId, userId)
                .gt(PlanList::getDeadLine, now)
                .lt(PlanList::getStartTime, now)
                .ne(PlanList::getStatus, 1)
        );
        List<Integer> ids2 = plans2.stream().map(PlanList::getId).toList();
        if (!ids2.isEmpty()) {
            LambdaUpdateWrapper<PlanList> updateWrapper2 = new LambdaUpdateWrapper<>();
            updateWrapper2.in(PlanList::getId, ids2).set(PlanList::getStatus, 3);
            baseMapper.update(null, updateWrapper2);  // 或者 update(实体, updateWrapper)
        }
        Long value = baseMapper.selectCount(new LambdaQueryWrapper<PlanList>().eq(PlanList::getUserId, userId).between(PlanList::getCreateTime, todayStart, todayEnd));
        //2.查询已完成
        Long value2 = baseMapper.selectCount(new LambdaQueryWrapper<PlanList>().eq(PlanList::getUserId, userId).eq(PlanList::getStatus, 1));
        //查询正在进行中
        Long value3 = baseMapper.selectCount(new LambdaQueryWrapper<PlanList>().eq(PlanList::getUserId, userId).eq(PlanList::getStatus, 3));
        //今日即将过期
        Long value4 = baseMapper.selectCount(new LambdaQueryWrapper<PlanList>()
                .eq(PlanList::getUserId, userId)
                .eq(PlanList::getStatus, 2) // 且 <= 当前时间+10分钟
        );
        //查询所有计划
        Long value5 = baseMapper.selectCount(new LambdaQueryWrapper<PlanList>().eq(PlanList::getUserId, userId));
        List<Integer> stats = List.of(value.intValue(),value2.intValue(),value3.intValue(),value4.intValue(),value5.intValue());

        //查询todoStats
        LocalDateTime tenMinutesLater = now.plusMinutes(10);
        //今日新增
        Long newCount = baseMapper.selectCount(new LambdaQueryWrapper<PlanList>().eq(PlanList::getUserId, userId).between(PlanList::getCreateTime, todayStart, todayEnd));

        //今日完成
        Long completed = baseMapper.selectCount(new LambdaQueryWrapper<PlanList>().eq(PlanList::getUserId, userId).eq(PlanList::getStatus, 1).between(PlanList::getCreateTime, todayStart, todayEnd));
        PlanListDataVO.TodoStats todoStats = new PlanListDataVO.TodoStats();
        todoStats.setNewCount(newCount.intValue());
        todoStats.setCompleted(completed.intValue());
        //查询todos
        Page<PlanList> page = new Page<>(pageNum, pageSize);
        IPage<PlanList> todos =  baseMapper.selectPage(page, new LambdaQueryWrapper<PlanList>()
                .eq(PlanList::getUserId, userId)
                .orderByAsc(PlanList::getCreateTime)
        );
        return new PlanListDataVO(stats, todoStats,todos);
    }

    @Override
    public Object addPlan(PlanListDTO planListDTO) {
        Long isExists = baseMapper.selectCount(new LambdaQueryWrapper<PlanList>().eq(PlanList::getUserId, planListDTO.getUserId()).eq(PlanList::getContent, planListDTO.getContent()).between(PlanList::getCreateTime, planListDTO.getStartTime(), planListDTO.getDeadLine()));
        if (isExists > 0) {
            return Result.fail("Plan already exists");
        }
        PlanList planList = new PlanList();
        BeanUtils.copyProperties(planListDTO, planList);
        int isAdded = baseMapper.insert(planList);
        //更新数据状态为进行中
        List<PlanList> plans = baseMapper.selectList(new LambdaQueryWrapper<PlanList>()
                .eq(PlanList::getUserId, planListDTO.getUserId())
                .gt(PlanList::getDeadLine, planListDTO.getDeadLine())
                .lt(PlanList::getStartTime, planListDTO.getStartTime())
                .ne(PlanList::getStatus, 1)
        );
        List<Integer> ids = plans.stream().map(PlanList::getId).toList();
        LambdaUpdateWrapper<PlanList> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(PlanList::getId, ids).set(PlanList::getStatus, 3);

        if (isAdded > 0) {
            PlanOption option = PlanOption.ADD;
            return Result.ok(option);
        }
        return Result.fail("Failed to add plan");
    }

    @Override
    public List<PlanList> getDailyPlan(Long userId, String date) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//        LocalDateTime dateTime = LocalDateTime.parse(date, formatter);
//        Date dateObj = Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
        LocalDateTime todayEnd = LocalDate.now().atTime(23, 59, 59);
        //更新数据状态为进行中
        List<PlanList> plans = baseMapper.selectList(new LambdaQueryWrapper<PlanList>()
                .eq(PlanList::getUserId, userId)
                .gt(PlanList::getDeadLine, now)
                .lt(PlanList::getStartTime, now)
                .ne(PlanList::getStatus, 1)
        );
        List<Integer> ids = plans.stream().map(PlanList::getId).toList();
        if (!ids.isEmpty()) {
            LambdaUpdateWrapper<PlanList> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.in(PlanList::getId, ids).set(PlanList::getStatus, 3);
            baseMapper.update(null, updateWrapper);  // 或者 update(实体, updateWrapper)
        }
        //查询今日计划
        List<PlanList> planList = baseMapper.selectList(new LambdaQueryWrapper<PlanList>()
                .eq(PlanList::getUserId, userId)
                .ge(PlanList::getStartTime, todayStart)
                .le(PlanList::getDeadLine, todayEnd)
        );
        //统计今日计划完成情况并存入数据库
        double completedRate = (double) ((baseMapper.selectCount(new LambdaQueryWrapper<PlanList>()
                .eq(PlanList::getUserId, userId)
                .eq(PlanList::getStatus, 1)
                .between(PlanList::getDeadLine, todayStart, todayEnd))*100)/ planList.size());
        System.out.println("今日计划完成情况: " + completedRate);
        echartsDataService.saveData(completedRate, userId);
        return planList;

    }

    @Override
    public Object deletePlanById(Long planId, Long userId) {
        int isDeleted = baseMapper.delete(new LambdaQueryWrapper<PlanList>().eq(PlanList::getId, planId).eq(PlanList::getUserId, userId));
        if (isDeleted > 0) {
            PlanOption option = PlanOption.DELETE;
            return Result.ok(option);
        }
        return Result.fail("Failed to delete plan");
    }

    @Override
    public Object completePlan(Long planId, Long userId) {

        PlanList planList = baseMapper.selectOne(new LambdaQueryWrapper<PlanList>().eq(PlanList::getId, planId).eq(PlanList::getUserId, userId));
        planList.setStatus(1);
        int result = baseMapper.update(planList
                , new LambdaQueryWrapper<PlanList>().eq(PlanList::getId, planId).eq(PlanList::getUserId, userId));
        if (result > 0) {
            return Result.ok(PlanStatus.COMPLETE);
        }
        return Result.fail("Failed to complete plan");
    }

}




