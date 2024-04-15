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
@Table(name = "user")
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@ApiModel("用户表")
public class User extends DataEntity {
    @ApiModelProperty("用户名")
    @Column(name = "name")
    private String name;

    @ApiModelProperty("是否管理员")
    @Column(name = "is_admin")
    private Integer isAdmin;

    public void copy(User user) {
        this.name = user.getName();
        this.isAdmin = user.getIsAdmin();
        this.setCreateBy(user.getCreateBy());
        this.setUpdateBy(user.getUpdateBy());
    }
}
