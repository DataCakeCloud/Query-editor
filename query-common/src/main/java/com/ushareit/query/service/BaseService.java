package com.ushareit.query.service;

import com.ushareit.query.bean.BaseEntity;

/**
 * Base Service
 *
 * @author Much
 * @date 2018/10/26
 */
public interface BaseService <T extends BaseEntity> extends CrudService<T>{
}
