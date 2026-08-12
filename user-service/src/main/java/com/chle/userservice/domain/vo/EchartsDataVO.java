package com.chle.userservice.domain.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.util.Date;
@Data
public class EchartsDataVO {

    @TableField("completion_rate")
    private String completionRate;
    @TableField("data_time")
    private Date dataTime;

}
