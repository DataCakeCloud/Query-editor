package com.ushareit.query.mapper;

import com.github.pagehelper.Page;
import com.ushareit.query.bean.QueryHistory;
import com.ushareit.query.repositry.mapper.CrudMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: huyx
 * @create: 2022-01-19 15:24
 */
@Mapper
public interface QueryHistoryMapper extends CrudMapper<QueryHistory> {
    /**
     * 根据name查询
     *
     * @param queryId queryId
     * @return
     */
    @Override
    @Select({"SELECT * FROM query_history WHERE query_id=#{queryId}"})
    QueryHistory selectByName(@Param("queryId") String queryId);

    /**
     * 根据uuid查询
     *
     * @param uuid uuid
     * @return
     */
    @Select({"SELECT * FROM query_history WHERE uuid=#{uuid}"})
    QueryHistory selectByUuid(@Param("uuid") String uuid);

    /**
     * 根据MAP参数查询
     *
     * @param info info
     * @param name name
     * @return
     */
    @Select({"<script>" +
            "SELECT * FROM query_history " +
            "WHERE 1=1 AND status!=4 " +
            "AND api_id IS NULL " +
            "AND create_by = #{name} " +
            "AND user_group = #{userGroup} " +
            "<if test='info!=null and \"\" neq info'> AND (id LIKE BINARY CONCAT('%',#{info},'%') OR LOWER(query_sql) LIKE BINARY LOWER(CONCAT('%',#{info},'%')) OR start_time LIKE BINARY CONCAT('%',#{info},'%') OR LOWER(engine_label) LIKE BINARY LOWER(CONCAT('%',#{info},'%')) OR statusZh LIKE CONCAT('%',#{info},'%') OR create_by LIKE CONCAT('%',#{info},'%') ) </if> " +
            " ORDER BY id DESC" +
            "</script>"})
    Page<QueryHistory> listByInfo(@Param("info") String info, @Param("name") String name, @Param("userGroup") String userGroup);

    /**
     * 根据MAP参数查询
     *
     * @param info info
     * @return
     */
    @Select({"<script>" +
            "SELECT * FROM query_history " +
            "WHERE 1=1 AND status!=4 " +
            "AND api_id IS NULL " +
            "AND user_group = #{userGroup} " +
            "<if test='info!=null and \"\" neq info'> AND (id LIKE BINARY CONCAT('%',#{info},'%') OR LOWER(query_sql) LIKE BINARY LOWER(CONCAT('%',#{info},'%')) OR start_time LIKE BINARY CONCAT('%',#{info},'%') OR LOWER(engine_label) LIKE BINARY LOWER(CONCAT('%',#{info},'%')) OR statusZh LIKE CONCAT('%',#{info},'%') OR create_by LIKE CONCAT('%',#{info},'%') ) </if> " +
            " ORDER BY id DESC" +
            "</script>"})
    Page<QueryHistory> listForAdmin(@Param("info") String info, @Param("userGroup") String userGroup);

    /**
     * 根据MAP参数查询
     *
     * @param info info
     * @param name name
     * @return
     */
    @Select({"<script>" +
            "SELECT * FROM query_history " +
            "WHERE 1=1 AND status!=4 " +
            "AND api_id IS NULL " +
            "AND create_by = #{name} " +
            "AND user_group = #{userGroup} " +
            "<if test='query_id!=null and \"\" neq query_id'> AND id LIKE BINARY CONCAT('%',#{query_id},'%') </if> " +
            "<if test='query_sql!=null and \"\" neq query_sql'> AND LOWER(query_sql) LIKE BINARY LOWER(CONCAT('%',#{query_sql},'%')) </if> " +
            "<if test='engine!=null and \"\" neq engine'> AND LOWER(engine) LIKE BINARY LOWER(CONCAT('%',#{engine},'%')) </if> " +
            "<if test='status!=null and status!=-1'> AND status=#{status} </if> " +
            "<if test='task_id!=null'> AND task_id=#{task_id} </if> " +
            " ORDER BY id DESC" +
            "</script>"})
    Page<QueryHistory> listByDetails(@Param("info") String info,
            @RequestParam("query_id") String query_id,
            @RequestParam("query_sql") String query_sql,
            @RequestParam("engine") String engine,
            @RequestParam("status") Integer status,
            @RequestParam("task_id") Integer task_id,
    		@Param("userGroup") String userGroup,
    		@Param("name") String name);

    /**
     * 根据MAP参数查询
     *
     * @param info info
     * @param name name
     * @return
     */
    @Select({"<script>" +
            "SELECT * FROM query_history " +
            "WHERE 1=1 AND status!=4 " +
            "AND api_id IS NULL " +
            "AND user_group = #{userGroup} " +
            "<if test='query_id!=null and \"\" neq query_id'> AND id LIKE BINARY CONCAT('%',#{query_id},'%') </if> " +
            "<if test='query_sql!=null and \"\" neq query_sql'> AND LOWER(query_sql) LIKE BINARY LOWER(CONCAT('%',#{query_sql},'%')) </if> " +
            "<if test='createBy!=null and \"\" neq createBy'> AND LOWER(create_by) LIKE LOWER(CONCAT('%',#{createBy},'%')) </if> " +
            "<if test='engine!=null and \"\" neq engine'> AND LOWER(engine) LIKE BINARY LOWER(CONCAT('%',#{engine},'%')) </if> " +
            "<if test='status!=null and status!=-1'> AND status=#{status} </if> " +
            "<if test='task_id!=null'> AND task_id=#{task_id} </if> " +
            " ORDER BY id DESC" +
            "</script>"})
    Page<QueryHistory> listByAdminDetails(@Param("info") String info,
            @RequestParam("query_id") String query_id,
            @RequestParam("query_sql") String query_sql,
            @RequestParam("createBy") String createBy,
            @RequestParam("engine") String engine,
            @RequestParam("status") Integer status,
            @Param("userGroup") String userGroup,
            @RequestParam("task_id") Integer task_id);
    
    /**
     * 根据uuid更新api id
     *
     * @param uuid uuid
     * @param id id
     * @param user user
     * @return
     */
    @Update({"UPDATE query_history set api_id=#{id}, update_by=#{user} WHERE uuid=#{uuid}"})
    void updateApiId(@Param("uuid") String uuid, @Param("id") Integer id, @Param("user") String user);

    /**
     * 根据api id获取历史记录
     *
     * @param apiId apiId
     * @return
     */
    @Select({"SELECT * FROM query_history WHERE api_id=#{apiId}"})
    ArrayList<QueryHistory> selectByApiId(@Param("apiId") Integer apiId);
    
    /**
     * 根据api id获取历史记录
     *
     * @param apiId apiId
     * @return
     */
    @Select({"<script>SELECT id, create_by, create_time, api_id FROM query_history WHERE api_id in " +
             "<foreach collection=\"apiIds\" index = \"index\" item = \"id\" open= \"(\" separator=\",\" close=\")\">" +
             "#{id}</foreach></script>"})
    ArrayList<QueryHistory> selectByApiIds(@Param("apiIds") List<Integer> apiIds);
}
