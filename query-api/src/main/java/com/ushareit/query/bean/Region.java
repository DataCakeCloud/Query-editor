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
 * @author: tangjk
 * @create: 2022-01-19 15:24
 */
@Data
@Entity
@Builder
@Table(name = "region")
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@ApiModel("可用区域表")
public class Region extends DataEntity {
    @ApiModelProperty("region名")
    @Column(name = "name")
    private String name;

    @ApiModelProperty("region中文名")
    @Column(name = "name_zh")
    private String nameZh;

    public void copy(Region region) {
        this.name = region.getName();
        this.nameZh = region.getNameZh();
    }
}

