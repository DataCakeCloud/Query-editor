package com.ushareit.query.bean;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author: wangsy1
 * @create: 2022-11-24 15:22
 */
@Data
@Entity
@Builder
@Table(name = "favordashchart")
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@ApiModel("收藏图标看板表")
public class FavorDashChart extends DataEntity {

    @ApiModelProperty("收藏id")
    @Column(name = "favor_id")
    private Integer favorId;

    @ApiModelProperty("收藏类型")
    @Column(name = "type")
    private String type;

    @ApiModelProperty("活跃判断")
    @Column(name = "is_active")
    private Integer isActive;

    public void copy(FavorDashChart favorDashChart) {
        this.favorId = favorDashChart.getFavorId();
        this.type = favorDashChart.getType();
        this.isActive = favorDashChart.getIsActive();
        this.setCreateBy(favorDashChart.getCreateBy());
        this.setUpdateBy(favorDashChart.getUpdateBy());
        this.setCreateTime(favorDashChart.getCreateTime());
        this.setUpdateTime(favorDashChart.getUpdateTime());
    }
}
