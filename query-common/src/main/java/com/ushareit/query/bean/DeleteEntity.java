package com.ushareit.query.bean;

import javax.persistence.Column;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 逻辑删除的实体
 * @author Much
 * @date 2018/11/6
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class DeleteEntity extends DataEntity implements Cloneable{

    public static final Integer NOT_DELETE = 0;
    public static final Integer DELETE = 1;

    @Column(name = "delete_status")
    private Integer deleteStatus;

}
