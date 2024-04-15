package com.ushareit.query.mapper;

import com.github.pagehelper.Page;
import com.ushareit.query.bean.SavedQuery;
import com.ushareit.query.repositry.mapper.CrudMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author: huyx
 * @create: 2022-01-19 15:24
 */
@Mapper
public interface SavedQueryMapper extends CrudMapper<SavedQuery> {
    /**
     * 根据name查询
     *
     * @param name name
     * @return
     */
    @Override
    @Select({"SELECT * FROM saved_query WHERE title=#{name}"})
    SavedQuery selectByName(@Param("name") String name);

    /**
     * 根据id查询
     *
     * @param id id
     * @return
     */
    @Select({"SELECT * FROM saved_query WHERE id=#{id}"})
    SavedQuery selectById(@Param("id") Integer id);

    /**
     * 根据MAP参数查询
     *
     * @param info info
     * @return
     */
    @Select({"<script>" +
            "SELECT * FROM saved_query " +
            "WHERE 1=1 " +
            "AND create_by = #{name}" +
            "<if test='info!=null and \"\" neq info'> AND (title LIKE CONCAT('%',#{info},'%') OR query_sql LIKE CONCAT('%',#{info},'%') OR description LIKE CONCAT('%',#{info},'%') OR engine_zh LIKE CONCAT('%',#{info},'%')) </if> " +
            " ORDER BY update_time DESC" +
            "</script>"})
    Page<SavedQuery> listByInfo(@Param("info") String info, @Param("name") String name);

    /**
     * 根据MAP参数查询
     *
     * @param info info
     * @return
     */
    @Select({"<script>" +
            "SELECT * FROM saved_query " +
            "WHERE 1=1 " +
            "AND create_by = #{name}" +
            "AND user_group = #{userGroup} " +
            "<if test='title!=null and \"\" neq title'> AND title LIKE CONCAT('%',#{title},'%') </if> " +
            "<if test='query_sql!=null and \"\" neq query_sql'> AND query_sql LIKE CONCAT('%',#{query_sql},'%') </if> " +
            "<if test='engine!=null and \"\" neq engine'> AND engine LIKE CONCAT('%',#{engine},'%') </if> " +
            "<if test='folderID!=null and folderID>0'> AND folderID=#{folderID} </if> " +
            "<if test='region!=null and \"\" neq region'> AND (LOWER(region) LIKE BINARY LOWER(CONCAT('%',#{region},'%'))) </if> " +
            " ORDER BY update_time DESC" +
            "</script>"})
    Page<SavedQuery> listByTitleSqlEngineFolder(@Param("title") String title,
    		@Param("query_sql") String query_sql,
    		@Param("engine") String engine,
    		@Param("folderID") Integer folderID,
    		@Param("region") String region,
                @Param("userGroup") String userGroup,
    		@Param("name") String name
    		);
    
    /**
     * 根据username查询
     *
     * @param title title
     * @param username username
     * @return
     */
    @Select({"<script>" +
            "SELECT title FROM saved_query " +
            "WHERE title=#{title} " +
            "AND create_by=#{username}" +
            "<if test='id!=null and \"\" neq id'> AND id != #{id} </if> " +
            "</script>"})
    List<String> selectByUsername(@Param("title") String title, @Param("username") String username, @Param("id") Integer id);

    @Update("update saved_query set folderID = #{parentid}, update_time = #{time} where id = #{id} and create_by = #{name}")
    boolean move(@Param("id") Integer id, @Param("time") LocalDateTime time, @Param("parentid") Integer parentId, @Param("name") String name);
}
