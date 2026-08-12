package com.chle.userservice.controller;

import com.chle.common.result.Result;

import com.chle.userservice.domain.vo.EchartsDataVO;
import com.chle.userservice.service.EchartsDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Controller
@RestController
@RequestMapping("/total")
@RequiredArgsConstructor
public class EchartsController {
    private final EchartsDataService echartsDataService;

    @GetMapping("/data/{userId}")
    public Result<List<EchartsDataVO>> getTotalData(@PathVariable("userId") Long userId) {
        List<EchartsDataVO> data = echartsDataService.getTotalData(userId);
        return Result.ok(data);
    }

}
