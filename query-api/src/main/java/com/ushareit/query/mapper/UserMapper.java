package com.ushareit.query.mapper;

import com.ushareit.query.bean.User;
import com.ushareit.query.repositry.mapper.CrudMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * @author: huyx
 * @create: 2022-01-19 15:24
 */
@Mapper
public interface UserMapper extends CrudMapper<User> {
    /**
     * 根据name查询
     *
     * @param name name
     * @return
     */
    @Override
    @Select({"SELECT * FROM user WHERE name=#{name}"})
    User selectByName(@Param("name") String name);

    /**
     * 根据name查询引擎
     *
     * @param name name
     * @return
     */
    @Select({"SELECT json_extract(engine,'$') engine FROM engine_auth WHERE name=#{name}"})
    String selectEngineByName(@Param("name") String name);
}
