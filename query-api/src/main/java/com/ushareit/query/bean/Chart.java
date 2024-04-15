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
import java.time.LocalDateTime;

/**
 * @author: wangsy1
 * @create: 2022-11-24 15:22
 */
@Data
@Entity
@Builder
@Table(name = "chart")
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@ApiModel("图表表")
public class Chart extends DataEntity {

    @ApiModelProperty("名称")
    @Column(name = "name")
    private String name;

    @ApiModelProperty("类型")
    @Column(name = "type")
    private String type;

    @ApiModelProperty("描述")
    @Column(name = "describe_chart")
    private String describeChart;

    @ApiModelProperty("活跃判断")
    @Column(name = "is_active")
    private Integer isActive;

    @ApiModelProperty("分享判断")
    @Column(name = "is_share")
    private Integer isShare;

    @ApiModelProperty("查询语句")
    @Column(name = "query_sql")
    private String querySql;

    @ApiModelProperty("chart的参数")
    @Column(name = "param")
    private String param;

    @ApiModelProperty("任务标识")
    @Column(name = "uuid")
    private String uuid;

    @ApiModelProperty("收藏标识")
    @Column(name = "is_favorate")
    private Integer isFavorate;

    @ApiModelProperty("sql_content")
    @Column(name = "content")
    private String content;

    @ApiModelProperty("调度状态")
    @Column(name = "status")
    private String status;

    @ApiModelProperty("engine")
    @Column(name = "engine")
    private String engine;

    public void copy(Chart chart){
        this.name = chart.getName();
        this.type = chart.getType();
        this.describeChart = chart.getDescribeChart();
        this.isActive = chart.getIsActive();
        this.isShare = chart.getIsShare();
        this.querySql = chart.getQuerySql();
        this.param = chart.getParam();
        this.uuid = chart.getUuid();
        this.isFavorate = chart.getIsFavorate();
        this.content = chart.getContent();
        this.engine = chart.getEngine();
        this.status = chart.getStatus();
        this.setCreateBy(chart.getCreateBy());
        this.setUpdateBy(chart.getUpdateBy());
        this.setCreateTime(chart.getCreateTime());
        this.setUpdateTime(chart.getUpdateTime());
    }
}
