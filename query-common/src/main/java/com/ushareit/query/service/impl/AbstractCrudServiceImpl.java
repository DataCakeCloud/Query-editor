package com.ushareit.query.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ushareit.query.bean.BaseEntity;
import com.ushareit.query.service.CrudService;
import com.ushareit.query.repositry.mapper.CrudMapper;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Abstract base service
 *
 * @author Much
 * @date 2018/10/26
 */
public abstract class AbstractCrudServiceImpl<T extends BaseEntity> implements CrudService<T> {

    /**
     * get base mapper
     *
     * @return base mapper
     */
    public abstract CrudMapper<T> getBaseMapper();

    @Override
    public List<T> listByExample(T t) {
        return getBaseMapper().select(t);
    }

    public List<T> listByMap(Map<String, String> paramMap) {
        return getBaseMapper().listByMap(paramMap);
    }

    @Override
    public PageInfo<T> listByPage(int pageNum, int pageSize, T t) {
        PageHelper.startPage(pageNum, pageSize);
        List<T> pageRecord = getBaseMapper().select(t);
        return new PageInfo<>(pageRecord);
    }

    @Override
    public PageInfo<T> listByPage(int pageNum, int pageSize, Map<String, String> paramMap) {
        PageHelper.startPage(pageNum, pageSize);
        List<T> pageRecord = getBaseMapper().listByMap(paramMap);
        return new PageInfo<>(pageRecord);
    }

    @Override
    public T getById(Object id) {
        return getBaseMapper().selectByPrimaryKey(id);
    }

    @Override
    public T getByName(String name) { return getBaseMapper().selectByName(name);}

    public T getByUuid(String uuid) {return getBaseMapper().selectByUuid(uuid);}

    @Override
    public T selectOne(T t) { return getBaseMapper().selectOne(t);}

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Object save(T t) {
        return getBaseMapper().insertSelective(t);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(List<T> t) {
        getBaseMapper().insertList(t);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(T t) {
        getBaseMapper().updateByPrimaryKeySelective(t);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(List<T> tList) {
        batchUpdate(tList);
    }

    private void batchUpdate(List<T> tList) {
        for (T t : tList) {
            getBaseMapper().updateByPrimaryKeySelective(t);
        }
    }

    @Override
    public void delete(Object id) {
        getBaseMapper().deleteByPrimaryKey(id);
    }


}
