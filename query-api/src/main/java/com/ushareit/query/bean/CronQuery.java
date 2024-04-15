package com.ushareit.query.bean;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Data
@Entity
@Builder
@Table(name = "cron_query")
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@ApiModel("定时查询")
public class CronQuery extends BaseEntity {
    @ApiModelProperty("任务编号")
    @Column(name = "task_id")
    private Integer taskId;

    @ApiModelProperty("任务名称")
    @Column(name = "task_name")
    private String taskName;

    @ApiModelProperty("调度频率")
    @Column(name = "schedule")
    private String schedule;

    @ApiModelProperty("邮箱")
    @Column(name = "email")
    private String email;

    @ApiModelProperty("开始时间")
    @Column(name = "start_time")
    private String startTime;

    @ApiModelProperty("截止时间")
    @Column(name = "end_time")
    private String endTime;

    @ApiModelProperty("定时执行的SQL语句")
    @Column(name = "origin_sql")
    private String originSql;

    @ApiModelProperty("用户")
    @Column(name = "user_name")
    private String userName;

    @ApiModelProperty("用户组")
    @Column(name = "user_group")
    private String userGroup;

    @ApiModelProperty("执行引擎")
    @Column(name = "engine")
    private String engine;

    @ApiModelProperty("数据源区域")
    @Column(name = "region")
    private String region;

    @ApiModelProperty("数据源")
    @Column(name = "catalog")
    private String catalog;

    @ApiModelProperty("数据库")
    @Column(name = "db")
    private String db;

    @ApiModelProperty("任务状态")
    @Column(name = "status")
    private Integer status;

    @ApiModelProperty("任务创建时间")
    @Column(name = "create_time")
    private Timestamp createTime;

    public void copy(CronQuery cq) {
    }
}
