package com.ushareit.query.service;

import com.github.pagehelper.PageInfo;
import com.ushareit.query.bean.BaseEntity;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * Base Service
 *
 * @author Much
 * @date 2018/10/26
 */
public interface CrudService<T extends BaseEntity> {

    /**
     * 根据非空参数查询
     * 查询前20000条记录
     *
     * @param t 查询对象
     * @return List
     */
    List<T> listByExample(T t);

    /**
     * 分页查询
     *
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @param t        查询參數
     * @return PageInfo
     */
    PageInfo<T> listByPage(int pageNum, int pageSize, T t);

    /**
     * 分页查询
     * 需要实现 Mapper Select SQL
     *
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @param paramMap 查询參數
     * @return PageInfo
     */
    PageInfo<T> listByPage(int pageNum, int pageSize, Map<String, String> paramMap);

    /**
     * 根据主键查询
     *
     * @param id 主键
     * @return T
     */
    T getById(@NotNull Object id);

    /**
     * 根据名称查询
     *
     * @param name 名称
     * @return T
     */
    T getByName(@NotNull String name);

    /**
     * 根据名称查询
     *
     * @param t 查询參數
     * @return T
     */
    T selectOne(@NotNull T t);

    /**
     * 保存
     *
     * @param t 保存对象
     * @return 保存对象的Id
     */
    Object save(@Valid @NotNull T t);

    /**
     * 保存List
     *
     * @param t 保存对象List
     */
    void save(@Valid @NotEmpty List<T> t);

    /**
     * 根据主键更新非空属性
     *
     * @param t 更新对象
     */
    void update(@Valid @NotNull T t);

    /**
     * 根据主键更新非空属性
     * 实现方式:逐条执行，并没有真正的批量执行
     *
     * @param tList 更新集合
     */
    void update(@Valid @NotEmpty List<T> tList);

    /**
     * 根据主键删除
     *
     * @param id 主键
     */
    void delete(@NotNull Object id);
}
