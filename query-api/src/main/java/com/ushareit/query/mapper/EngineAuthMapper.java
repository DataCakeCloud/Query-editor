package com.ushareit.query.mapper;

import com.github.pagehelper.Page;
import com.ushareit.query.bean.EngineAuth;
import com.ushareit.query.repositry.mapper.CrudMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * @author: huyx
 * @create: 2022-01-19 15:24
 */
@Mapper
public interface EngineAuthMapper extends CrudMapper<EngineAuth> {
    /**
     * 根据name查询
     *
     * @param name name
     * @return
     */
    @Override
    @Select({"SELECT id, name, json_extract(engine,'$') engine, create_by, create_time, update_by, update_time FROM engine_auth WHERE name=#{name}"})
    EngineAuth selectByName(@Param("name") String name);

    /**
     * 根据MAP参数查询
     *
     * @param info info
     * @return
     */
    @Select({"<script>" +
            "SELECT id, name, json_extract(engine,'$') engine, create_by, create_time, update_by, update_time FROM engine_auth " +
            "WHERE 1=1 " +
            "<if test='info!=null and \"\" neq info'> AND (name LIKE CONCAT('%',#{info},'%') OR LOWER(JSON_EXTRACT(engine, '$[*]')) LIKE LOWER(CONCAT('%',#{info},'%'))) </if> " +
            " ORDER BY update_time DESC" +
            "</script>"})
    Page<EngineAuth> listByInfo(@Param("info") String info);
}
