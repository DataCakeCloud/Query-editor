package com.ushareit.query.bean;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.sql.Timestamp;

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
@Table(name = "share_grade")
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@ApiModel("分享权限表")
public class ShareGrade extends BaseEntity {
    @ApiModelProperty("分享人")
    @Column(name = "sharer")
    private String sharer;

    @ApiModelProperty("被分享人")
    @Column(name = "sharee")
    private String sharee;

    @ApiModelProperty("权限级别，1为编辑，2为运行，3为查看")
    @Column(name = "grade")
    private Integer grade;    

    @ApiModelProperty("shareUrl")
    @Column(name = "shareUrl")
    private String shareUrl;

    @ApiModelProperty("sql_name")
    @Column(name = "sql_name")
    private String sqlName;

    @Column(name = "create_time")
    private Timestamp createTime;

    public void copy(ShareGrade sg) {
        this.sharer = sg.getSharer();
        this.sharee = sg.getSharee();
        this.grade = sg.getGrade();
        this.shareUrl = sg.getShareUrl();
        this.sqlName = sg.getSqlName();
        this.createTime = sg.getCreateTime();
    }
}

