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
@Table(name = "dashboard")
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@ApiModel("看板表")
public class Dashboard extends DataEntity {

    @ApiModelProperty("名称")
    @Column(name = "name")
    private String name;

    @ApiModelProperty("描述")
    @Column(name = "describe_dash")
    private String describeDash;

    @ApiModelProperty("活跃判断")
    @Column(name = "is_active")
    private Integer isActive;

    @ApiModelProperty("分享判断")
    @Column(name = "is_share")
    private Integer isShare;

    @ApiModelProperty("调度判断")
    @Column(name = "is_schedule")
    private Integer isSchedule;

    @ApiModelProperty("看板的参数")
    @Column(name = "param")
    private String param;

    @ApiModelProperty("看板所属文件夹")
    @Column(name = "classid")
    private Integer classId;

    @ApiModelProperty("crontab表达式")
    @Column(name = "crontab")
    private String crontab;

    @ApiModelProperty("收藏标识")
    @Column(name = "is_favorate")
    private Integer isFavorate;

    public void copy(Dashboard dashboard){
        this.name = dashboard.getName();
        this.describeDash = dashboard.getDescribeDash();
        this.isActive = dashboard.getIsActive();
        this.isShare = dashboard.getIsShare();
        this.isSchedule = dashboard.getIsSchedule();
        this.param = dashboard.getParam();
        this.classId = dashboard.getClassId();
        this.crontab = dashboard.getCrontab();
        this.isFavorate = dashboard.getIsFavorate();
        this.setCreateBy(dashboard.getCreateBy());
        this.setUpdateBy(dashboard.getUpdateBy());
        this.setCreateTime(dashboard.getCreateTime());
        this.setUpdateTime(dashboard.getUpdateTime());
    }
}
