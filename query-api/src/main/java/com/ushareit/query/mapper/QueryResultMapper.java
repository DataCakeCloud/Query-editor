package com.ushareit.query.mapper;

import com.ushareit.query.bean.Meta;
import com.ushareit.query.bean.QueryResult;
import com.ushareit.query.repositry.mapper.CrudMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @author: huyx
 * @create: 2022-01-19 15:24
 */
@Mapper
public interface QueryResultMapper extends CrudMapper<QueryResult> {
    @Select({"SELECT * FROM query_result WHERE query_inc_id = #{query_inc_id}"})
    QueryResult listByQueryID(@Param("query_inc_id") Integer query_inc_id);
}