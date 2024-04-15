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
 * @author: wangsy1
 * @create: 2022-11-24 15:22
 */
@Data
@Entity
@Builder
@Table(name = "sharebi")
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@ApiModel("分享表")
public class Sharebi extends DataEntity {

    @ApiModelProperty("分享人")
    @Column(name = "sharer")
    private String sharer;

    @ApiModelProperty("被分享人")
    @Column(name = "sharee")
    private String sharee;

    @ApiModelProperty("分享类型")
    @Column(name = "type")
    private String type;

    @ApiModelProperty("被分享chart_id或dashboard_id")
    @Column(name = "share_id")
    private Integer shareId;

    @ApiModelProperty("权限级别，1为查看")
    @Column(name = "grade")
    private Integer grade;

    @ApiModelProperty("shareUrl")
    @Column(name = "share_url")
    private String shareUrl;

    public void copy(Sharebi share){
        this.sharer = share.getSharer();
        this.sharee = share.getSharee();
        this.type = share.getType();
        this.shareId = share.getShareId();
        this.grade = share.getGrade();
        this.shareUrl = share.getShareUrl();
        this.setCreateTime(share.getCreateTime());
    }
}
