package com.ushareit.query.bean;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import javax.persistence.Column;
import java.sql.Timestamp;

/**
 * 在数据库中拥有创建人和更新人的实体
 *
 * @author wuyan
 * @date 2018/11/5
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class DataEntity extends BaseEntity {

    @Column(name = "create_by")
    private String createBy;
    @Column(name = "update_by")
    private String updateBy;
    @Column(name = "create_time")
    private Timestamp createTime;
    @Column(name = "update_time")
    private Timestamp updateTime;

}
