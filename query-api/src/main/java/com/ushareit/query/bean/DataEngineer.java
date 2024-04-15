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
@Table(name = "data_engineer")
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@ApiModel("调度信息表")
public class DataEngineer extends DataEntity {
    @ApiModelProperty("uuid")
    @Column(name = "uuid")
    private String uuid;

    @ApiModelProperty("engine的key")
    @Column(name = "engine")
    private String engine;

    @ApiModelProperty("sql")
    @Column(name = "query_sql")
    private String querySql;

    public void copy(DataEngineer dataEngineer) {
        this.uuid = dataEngineer.getUuid();
        this.engine = dataEngineer.getEngine();
        this.querySql = dataEngineer.getQuerySql();
        this.setCreateBy(dataEngineer.getCreateBy());
        this.setUpdateBy(dataEngineer.getUpdateBy());
    }
}
