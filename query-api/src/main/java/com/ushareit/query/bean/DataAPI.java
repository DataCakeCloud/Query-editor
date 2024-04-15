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
 * @author: huyx
 * @create: 2022-01-19 15:24
 */
@Data
@Entity
@Builder
@Table(name = "data_api")
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@ApiModel("数据API表")
public class DataAPI extends DataEntity {
    @ApiModelProperty("API名称")
    @Column(name = "title")
    private String title;

    @ApiModelProperty("API路径")
    @Column(name = "path")
    private String path;

    @ApiModelProperty("API对应的sql")
    @Column(name = "query_sql")
    private String querySql;

    @ApiModelProperty("engine的key")
    @Column(name = "engine")
    private String engine;

    @ApiModelProperty("engine的label")
    @Column(name = "engineZh")
    private String engineZh;

    @ApiModelProperty("API的参数")
    @Column(name = "param")
    private String param;

    @ApiModelProperty("API的状态")
    @Column(name = "status")
    private Integer status;

    @ApiModelProperty("uuid")
    @Column(name = "uuid")
    private String uuid;

    @ApiModelProperty("region")
    @Column(name = "region")
    private String region;

    @ApiModelProperty("catalog")
    @Column(name = "catalog")
    private String catalog;

    public void copy(DataAPI dataAPI) {
        this.title = dataAPI.getTitle();
        this.path = dataAPI.getPath();
        this.querySql = dataAPI.getQuerySql();
        this.engine = dataAPI.getEngine();
        this.engineZh = dataAPI.getEngineZh();
        this.param = dataAPI.getParam();
        this.status = dataAPI.getStatus();
        this.uuid = dataAPI.getUuid();
        this.region = dataAPI.getRegion();
        this.catalog = dataAPI.getCatalog();
        this.setCreateBy(dataAPI.getCreateBy());
        this.setUpdateBy(dataAPI.getUpdateBy());
    }
}
