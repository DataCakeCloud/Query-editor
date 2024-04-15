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
 * @create: 2022-11-07 15:22
 */
@Data
@Entity
@Builder
@Table(name = "favortable")
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@ApiModel("收藏数据表")
public class FavorTable extends DataEntity {

    @ApiModelProperty("名称")
    @Column(name = "name")
    private String name;

    @ApiModelProperty("活跃判断")
    @Column(name = "is_active")
    private Integer isActive;

    @ApiModelProperty("数据区域")
    @Column(name = "region")
    private String region;

    @ApiModelProperty("数据源")
    @Column(name = "catalog")
    private String catalog;

    @ApiModelProperty("数据表")
    @Column(name = "db")
    private String db;

    public void copy(FavorTable favorTable) {
        this.name = favorTable.getName();
        this.isActive = favorTable.getIsActive();
        this.region = favorTable.getRegion();
        this.catalog = favorTable.getCatalog();
        this.db = favorTable.getDb();
        this.setCreateBy(favorTable.getCreateBy());
        this.setUpdateBy(favorTable.getUpdateBy());
        this.setCreateTime(favorTable.getCreateTime());
        this.setUpdateTime(favorTable.getUpdateTime());
    }
}
