package com.chle.userservice.controller;

import com.chle.common.result.Result;
import com.chle.userservice.domain.PlanList;
import com.chle.userservice.domain.dto.PlanListDTO;
import com.chle.userservice.domain.vo.PlanListDataVO;
import com.chle.userservice.service.PlanListService;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Controller
@RequestMapping("/planList")
@RestController
@RequiredArgsConstructor
public class PlanListController {
    private final PlanListService planlistService;

    @PostMapping("/data")
    public Result<PlanListDataVO> getData(@Param("userId") Long userId, @Param("pageNum") int pageNum, @Param("pageSize") int pageSize) {
        PlanListDataVO planListDataVO = planlistService.getPlanData(userId, pageNum, pageSize);
        if (planListDataVO == null) {
            return Result.fail("Plan list not found for user: " + userId);
        }
        return Result.ok(planListDataVO);
    }

    @PostMapping("/dailyPlan")
    public Result<List<PlanList>> getDailyPlan(@Param("userId") Long userId, @Param("date") String date ) {
        return Result.ok(planlistService.getDailyPlan(userId, date));
    }

    @DeleteMapping("/deletePlan")
    public Result<?> deletePlan(@Param("planId") Long planId, @Param("userId") Long userId) {
        return Result.ok(planlistService.deletePlanById(planId, userId));
    }

    @PostMapping("/completePlan")
    public Result<?> completePlan(@RequestParam("planId") Long planId, @RequestParam("userId") Long userId) {
        return Result.ok(planlistService.completePlan(planId, userId));
    }

    @PostMapping("/addPlan")
    public Result<?> addPlan(@RequestBody PlanListDTO planListDTO) {
        return Result.ok(planlistService.addPlan(planListDTO));
    }

}
