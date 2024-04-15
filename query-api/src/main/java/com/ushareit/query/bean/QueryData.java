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
@Table(name = "query_data")
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@ApiModel("query与mysql数据表对照表")
public class QueryData extends DataEntity {

    @ApiModelProperty("任务标识")
    @Column(name = "uuid")
    private String uuid;

    @ApiModelProperty("数据区域")
    @Column(name = "region")
    private String region;

    @ApiModelProperty("数据表名")
    @Column(name = "db")
    private String db;

    @ApiModelProperty("de任务id")
    @Column(name = "detaskid")
    private Integer detaskid;

    @ApiModelProperty("活跃判断")
    @Column(name = "is_active")
    private Integer isActive;

    public void copy(QueryData queryData){
        this.uuid = queryData.getUuid();
        this.region = queryData.getRegion();
        this.db = queryData.getDb();
        this.detaskid = queryData.getDetaskid();
        this.isActive = queryData.getIsActive();
        this.setCreateBy(queryData.getCreateBy());
        this.setUpdateBy(queryData.getUpdateBy());
        this.setCreateTime(queryData.getCreateTime());
        this.setUpdateTime(queryData.getUpdateTime());
    }
}
