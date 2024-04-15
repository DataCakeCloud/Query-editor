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
@Table(name = "dash_chart")
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@ApiModel("看板图标对照表")
public class DashChart extends DataEntity {

    @ApiModelProperty("dashboard的id")
    @Column(name = "dashboard_id")
    private Integer dashboardId;

    @ApiModelProperty("chart的id")
    @Column(name = "chart_id")
    private Integer chartId;

    @ApiModelProperty("活跃判断")
    @Column(name = "is_active")
    private Integer isActive;

    public void copy(DashChart dashChart){
        this.dashboardId = dashChart.getDashboardId();
        this.chartId = dashChart.getChartId();
        this.isActive = dashChart.getIsActive();
        this.setCreateBy(dashChart.getCreateBy());
        this.setUpdateBy(dashChart.getUpdateBy());
        this.setCreateTime(dashChart.getCreateTime());
        this.setUpdateTime(dashChart.getUpdateTime());
    }
}
