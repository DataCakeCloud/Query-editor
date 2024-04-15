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
@Table(name = "temp_table")
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@ApiModel("临时表")
public class CreateTempTable extends DataEntity {
    @ApiModelProperty("用户名")
    @Column(name = "name")
    private String name;
}
