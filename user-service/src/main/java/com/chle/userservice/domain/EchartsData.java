package com.chle.userservice.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 
 * @TableName echarts_data
 */
@TableName(value ="echarts_data")
@Data
public class EchartsData {
    /**
     * 
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 
     */
    private Date dataTime;

    /**
     * 
     */
    private Integer completionRate;

    /**
     * 
     */
    private Long userId;

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        EchartsData other = (EchartsData) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getDataTime() == null ? other.getDataTime() == null : this.getDataTime().equals(other.getDataTime()))
            && (this.getCompletionRate() == null ? other.getCompletionRate() == null : this.getCompletionRate().equals(other.getCompletionRate()))
            && (this.getUserId() == null ? other.getUserId() == null : this.getUserId().equals(other.getUserId()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getDataTime() == null) ? 0 : getDataTime().hashCode());
        result = prime * result + ((getCompletionRate() == null) ? 0 : getCompletionRate().hashCode());
        result = prime * result + ((getUserId() == null) ? 0 : getUserId().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", dataTime=").append(dataTime);
        sb.append(", completionRate=").append(completionRate);
        sb.append(", userId=").append(userId);
        sb.append("]");
        return sb.toString();
    }
}