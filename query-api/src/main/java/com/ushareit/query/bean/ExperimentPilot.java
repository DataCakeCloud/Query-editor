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
@Table(name = "experiment_pilot")
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@ApiModel("实验控制名单表")
public class ExperimentPilot extends BaseEntity {
    @ApiModelProperty("exp_id")
    @Column(name = "exp_id")
    private Integer exp_id;

    @ApiModelProperty("user_name")
    @Column(name = "user_name")
    private String user_name;
    
    @ApiModelProperty("action,0:blacklist, 1:whitelist")
    @Column(name = "action")
    private Integer action;

    public void copy(ExperimentPilot pilot) {
        this.exp_id = pilot.getExp_id();
        this.user_name = pilot.getUser_name();
        this.action = pilot.getAction();
    }
}


