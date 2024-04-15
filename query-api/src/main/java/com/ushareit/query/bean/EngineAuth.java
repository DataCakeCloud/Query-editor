package com.ushareit.query.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
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
@Table(name = "engine_auth")
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@ApiModel("用户引擎权限表")
public class EngineAuth extends DataEntity {
    @ApiModelProperty("用户名")
    @Column(name = "name")
    private String name;

    @ApiModelProperty("用户有权限的引擎")
    @Column(name = "engine")
    private String engine;

    public void copy(EngineAuth engineAuth) {
        this.name = engineAuth.getName();
        this.engine = engineAuth.getEngine();
        this.setCreateBy(engineAuth.getCreateBy());
        this.setUpdateBy(engineAuth.getUpdateBy());
    }
}
