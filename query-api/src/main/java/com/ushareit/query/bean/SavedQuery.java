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
@Table(name = "saved_query")
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@ApiModel("保存查询表")
public class SavedQuery extends DataEntity {
    @ApiModelProperty("名称")
    @Column(name = "title")
    private String title;

    @ApiModelProperty("查询语句")
    @Column(name = "query_sql")
    private String querySql;

    @ApiModelProperty("查询描述")
    @Column(name = "description")
    private String description;

    @ApiModelProperty("查询引擎")
    @Column(name = "engine")
    private String engine;

    @ApiModelProperty("查询引擎")
    @Column(name = "engine_zh")
    private String engineZh;

    @ApiModelProperty("用户组")
    @Column(name = "user_group")
    private String userGroup;

    @ApiModelProperty("sql的参数")
    @Column(name = "param")
    private String param;

    @ApiModelProperty("文件夹ID")
    @Column(name = "folderID")
    private Integer folderID;

    @ApiModelProperty("region")
    @Column(name = "region")
    private String region;

    @ApiModelProperty("catalog")
    @Column(name = "catalog")
    private String catalog;

    public void copy(SavedQuery savedQuery) {
        this.title = savedQuery.getTitle();
        this.querySql = savedQuery.getQuerySql();
        this.description = savedQuery.getDescription();
        this.engine = savedQuery.getEngine();
        this.engineZh = savedQuery.getEngineZh();
        this.userGroup = savedQuery.getUserGroup();
        this.param = savedQuery.getParam();
        this.folderID = savedQuery.getFolderID();
        this.region = savedQuery.getRegion();
        this.catalog = savedQuery.getCatalog();
        this.setCreateBy(savedQuery.getCreateBy());
        this.setUpdateBy(savedQuery.getUpdateBy());
    }
}
