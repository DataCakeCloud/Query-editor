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
import java.sql.Timestamp;

/**
 * @author: wangsy1
 * @create: 2022-11-02 15:22
 */
@Data
@Entity
@Builder
@Table(name = "classification")
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@ApiModel("文件夹表")
public class Classification extends DataEntity {

    @ApiModelProperty("名称")
    @Column(name = "name")
    private String name;

    @ApiModelProperty("文件夹等级")
    @Column(name = "level")
    private Integer level;

    @ApiModelProperty("父级id")
    @Column(name = "parent_id")
    private Integer parentId;

    @ApiModelProperty("活跃判断")
    @Column(name = "is_active")
    private Integer isActive;

    @ApiModelProperty("查询判断")
    @Column(name = "is_query")
    private Integer isQuery;

    public void copy(Classification classification) {
        this.name = classification.getName();
        this.level = classification.getLevel();
        this.parentId = classification.parentId;
        this.isActive = classification.getIsActive();
        this.isQuery = classification.getIsQuery();
        this.setCreateBy(classification.getCreateBy());
        this.setUpdateBy(classification.getUpdateBy());
        this.setCreateTime(classification.getCreateTime());
        this.setUpdateTime(classification.getUpdateTime());
    }
}
