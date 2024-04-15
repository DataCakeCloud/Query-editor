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
@Table(name = "error_info")
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@ApiModel("错误信息表")
public class ErrorInfo extends DataEntity {
    @ApiModelProperty("正则表达式")
    @Column(name = "eror_regex")
    private String erorRegex;

    @ApiModelProperty("错误类别")
    @Column(name = "error_type")
    private String errorType;

    @ApiModelProperty("错误中文描述")
    @Column(name = "error_zh")
    private String errorZh;

    public void copy(ErrorInfo er) {
        this.erorRegex = er.getErorRegex();
        this.errorType = er.getErrorType();
        this.errorZh = er.getErrorZh();
    }
}

