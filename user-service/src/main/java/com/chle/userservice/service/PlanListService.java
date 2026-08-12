package com.chle.userservice.service;

import com.chle.userservice.domain.PlanList;
import com.baomidou.mybatisplus.extension.service.IService;
import com.chle.userservice.domain.dto.PlanListDTO;
import com.chle.userservice.domain.vo.PlanListDataVO;

import java.util.List;

/**
* @author CHEN
* @description 针对表【planlist】的数据库操作Service
* @createDate 2026-08-11 14:51:47
*/
public interface PlanListService extends IService<PlanList> {

    List<PlanList> getDailyPlan(Long userId, String date);

    Object deletePlanById(Long planId, Long userId);

    Object completePlan(Long planId, Long userId);

    PlanListDataVO getPlanData(Long userId, int pageNum, int pageSize);

    Object addPlan(PlanListDTO planListDTO);
}
