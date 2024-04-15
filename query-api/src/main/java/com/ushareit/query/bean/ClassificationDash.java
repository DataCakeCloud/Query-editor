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
@Table(name = "classificationdash")
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@ApiModel("文件夹看板对照表")
public class ClassificationDash extends DataEntity {

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

    public void copy(ClassificationDash classificationDash) {
        this.name = classificationDash.getName();
        this.level = classificationDash.getLevel();
        this.parentId = classificationDash.getParentId();
        this.isActive = classificationDash.getIsActive();
        this.isQuery = classificationDash.getIsQuery();
        this.setCreateBy(classificationDash.getCreateBy());
        this.setUpdateBy(classificationDash.getUpdateBy());
        this.setCreateTime(classificationDash.getCreateTime());
        this.setUpdateTime(classificationDash.getUpdateTime());
    }
}
