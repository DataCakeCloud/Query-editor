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
@Table(name = "ai_chat")
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@ApiModel("分享权限表")
public class AIChat extends BaseEntity {
    @ApiModelProperty("uuid")
    @Column(name = "uuid")
    private String uuid;

    @ApiModelProperty("聊天人")
    @Column(name = "user_name")
    private String userName;

    @ApiModelProperty("内容")
    @Column(name = "content")
    private String content;

    @ApiModelProperty("回复")
    @Column(name = "reply")
    private String reply;

    @Column(name = "create_time")
    private Timestamp createTime;

    public void copy(AIChat ac) {
        this.uuid = ac.getUuid();
        this.userName = ac.getUserName();
        this.content = ac.getContent();
        this.reply = ac.getReply();
        this.createTime = ac.getCreateTime();
    }
}

