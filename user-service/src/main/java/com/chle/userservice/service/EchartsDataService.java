package com.chle.userservice.service;

import com.chle.userservice.domain.EchartsData;
import com.baomidou.mybatisplus.extension.service.IService;
import com.chle.userservice.domain.vo.EchartsDataVO;

import java.util.List;

/**
* @author CHEN
* @description 针对表【echarts_data】的数据库操作Service
* @createDate 2026-08-12 14:58:58
*/
public interface EchartsDataService extends IService<EchartsData> {

    List<EchartsDataVO> getTotalData(Long userId);
    void saveData(double completeRate, Long userId);
}
