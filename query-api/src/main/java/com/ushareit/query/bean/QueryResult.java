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
 * @author: tangjk
 * @create: 2022-01-19 15:24
 */
@Data
@Entity
@Builder
@Table(name = "query_result")
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@ApiModel("query result table")
public class QueryResult extends BaseEntity {
    @ApiModelProperty("increasing query id")
    @Column(name = "query_inc_id")
    private Integer queryIncId;

    @ApiModelProperty("the result of query")
    @Column(name = "result_str")
    private String resultStr;

    @ApiModelProperty("columns info of result")
    @Column(name = "columns_str")
    private String columnsStr;

    @ApiModelProperty("create time")
    @Column(name = "create_time")
    private Timestamp createTime;

    public void copy(QueryResult qr) {
        this.queryIncId = qr.getQueryIncId();
        this.resultStr = qr.getResultStr();
        this.columnsStr = qr.getColumnsStr();
        this.createTime = qr.getCreateTime();
    }
}

