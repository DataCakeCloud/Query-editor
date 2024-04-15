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
@Table(name = "trans_sql")
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@ApiModel("sql转换表")
public class TransSql extends DataEntity {
    @ApiModelProperty("用户名")
    @Column(name = "user_name")
    private String userName;

    @ApiModelProperty("原sql")
    @Column(name = "origin_sql")
    private String originSql;

    @ApiModelProperty("原sql类型")
    @Column(name = "origin_type")
    private String originType;

    @ApiModelProperty("target sql")
    @Column(name = "target_sql")
    private String targetSql;

    @ApiModelProperty("结果sql类型")
    @Column(name = "target_type")
    private String targetType;

    public void copy(TransSql ts) {
        this.userName = ts.getUserName();
        this.originSql = ts.getOriginSql();
        this.originType = ts.getOriginType();
        this.targetSql = ts.getTargetSql();
        this.targetType = ts.getTargetType();
    }
}

