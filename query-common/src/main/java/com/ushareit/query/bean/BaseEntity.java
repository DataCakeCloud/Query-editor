package com.ushareit.query.bean;

import lombok.Data;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.io.Serializable;

/**
 * @author wuyan
 * @date 2018/11/5
 **/
@Data
public class BaseEntity implements Serializable {

    /**
     * id是主键，自动生成
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

}
