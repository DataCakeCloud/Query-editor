package com.ushareit.query.repositry.mapper;

import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.BaseMapper;
import tk.mybatis.mapper.common.IdsMapper;
import tk.mybatis.mapper.common.MySqlMapper;

import java.util.Map;

/**
 * @author wuyan
 * @date 2020/5/10
 */
public interface CrudMapper<T> extends BaseMapper<T>, MySqlMapper<T>, IdsMapper<T> {

    /**
     * 根据Map查询数据库
     *
     * @param paramMap paramMap
     * @return List
     */
    Page<T> listByMap(@Param("paramMap") Map<String, String> paramMap);
    /**
     * 根据Map查询数据库
     *
     * @param name name
     * @return T
     */
    T selectByName(@Param("name") String name);

    /**
     * 根据Map查询数据库
     *
     * @param uuid uuid
     * @return T
     */
    T selectByUuid(@Param("uuid") String uuid);
}

