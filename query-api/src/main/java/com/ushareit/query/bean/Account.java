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
@Table(name = "account")
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@ApiModel("公共账号表")
public class Account extends DataEntity {
    @ApiModelProperty("用户组")
    @Column(name = "user_group")
    private String userGroup;

    @ApiModelProperty("用户名")
    @Column(name = "username")
    private String username;

    @ApiModelProperty("密码")
    @Column(name = "password")
    private String password;

    public void copy(Account account) {
        this.userGroup = account.getUserGroup();
        this.username = account.getUsername();
        this.password = account.getPassword();
        this.setCreateBy(account.getCreateBy());
        this.setUpdateBy(account.getUpdateBy());
    }
}
