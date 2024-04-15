package com.ushareit.query.mapper;

import com.ushareit.query.bean.TransSql;
import com.ushareit.query.repositry.mapper.CrudMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * @author: tangjk
 * @create: 2022-01-19 15:24
 */
@Mapper
public interface TransSqlMapper extends CrudMapper<TransSql> {
    @Insert("insert into trans_sql" +
            "(user_name, " +
            "origin_sql, " +
            "target_sql) " +
            "values(" +
            "#{user_name}, " +
            "#{origin_sql}, " +
            "#{target_sql})")
    void addTransSql(@Param("user_name")String user_name,
                     @Param("origin_sql")String origin_sql,
                     @Param("target_sql")String target_sql);
}
