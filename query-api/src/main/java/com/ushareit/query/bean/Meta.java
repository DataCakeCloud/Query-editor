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
@Table(name = "engine")
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@ApiModel("引擎表")
public class Meta extends DataEntity {
    @ApiModelProperty("引擎类型")
    @Column(name = "engine_type")
    private String engineType;

    @ApiModelProperty("引擎key")
    @Column(name = "engine_key")
    private String engineKey;

    @ApiModelProperty("引擎name")
    @Column(name = "engine_name")
    private String engineName;

    @ApiModelProperty("引擎url")
    @Column(name = "engine_url")
    private String engineUrl;

    @ApiModelProperty("数据库")
    @Column(name = "engine_database")
    private String engineDatabase;

    @ApiModelProperty("用户名")
    @Column(name = "username")
    private String username;

    @ApiModelProperty("密码")
    @Column(name = "password")
    private String password;

    @ApiModelProperty("mysql数据库是否异步执行任务")
    @Column(name = "is_async")
    private Integer isAsync;

    @ApiModelProperty("区域")
    @Column(name = "region")
    private String region;

    public void copy(Meta meta) {
        this.engineType = meta.getEngineType();
        this.engineKey = meta.getEngineKey();
        this.engineName = meta.getEngineName();
        this.engineUrl = meta.getEngineUrl();
        this.engineDatabase = meta.getEngineDatabase();
        this.username = meta.getUsername();
        this.password = meta.getPassword();
        this.isAsync = meta.getIsAsync();
        this.region = meta.getRegion();
        this.setCreateBy(meta.getCreateBy());
        this.setUpdateBy(meta.getUpdateBy());
    }
}
