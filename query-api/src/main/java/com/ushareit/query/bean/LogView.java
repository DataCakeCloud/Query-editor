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
import java.time.LocalDateTime;

/**
 * @author: wangsy1
 * @create: 2022-12-07 15:22
 */
@Data
@Entity
@Builder
@Table(name = "logview")
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@ApiModel("记录看板图标使用情况表")
public class LogView extends DataEntity {

    @ApiModelProperty("类型")
    @Column(name = "type")
    private String type;

    @ApiModelProperty("打开的视图id")
    @Column(name = "view_id")
    private Integer viewId;

    @ApiModelProperty("创建人")
    @Column(name = "create_by")
    private String createBy;

    public void copy(LogView logView) {
        this.type = logView.getType();
        this.viewId = logView.getViewId();
        this.createBy = logView.getCreateBy();
        this.setCreateTime(logView.getCreateTime());
    }
}
