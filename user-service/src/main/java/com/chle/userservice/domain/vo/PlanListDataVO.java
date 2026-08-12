package com.chle.userservice.domain.vo;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.chle.userservice.domain.PlanList;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class PlanListDataVO {
    private List<Integer> stats;
    private TodoStats todoStats;
    private IPage<PlanList> todos;

    @Data
    public static class TodoStats {
        private Integer newCount;
        private Integer completed;
    }

}
