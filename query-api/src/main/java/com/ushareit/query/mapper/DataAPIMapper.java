package com.ushareit.query.mapper;

import com.github.pagehelper.Page;
import com.ushareit.query.bean.DataAPI;
import com.ushareit.query.repositry.mapper.CrudMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: huyx
 * @create: 2022-01-19 15:24
 */
@Mapper
public interface DataAPIMapper extends CrudMapper<DataAPI> {
    /**
     * 根据MAP参数查询
     *
     * @param title title
     * @param path path
     * @param querySql querySql
     * @param engineZh engineZh
     * @param param param
     * @param createBy createBy
     * @param status status
     * @param user user
     * @return
     */
    @Select({"<script>" +
            "SELECT * FROM data_api " +
            "WHERE 1=1 " +
            "AND create_by = #{user}" +
            "<if test='title!=null and \"\" neq title'> AND (LOWER(title) LIKE LOWER(CONCAT('%',#{title},'%'))) </if> " +
            "<if test='path!=null and \"\" neq path'> AND (path LIKE BINARY CONCAT('%',#{path},'%')) </if> " +
            "<if test='querySql!=null and \"\" neq querySql'> AND (LOWER(query_sql) LIKE BINARY LOWER(CONCAT('%',#{querySql},'%'))) </if> " +
            "<if test='engineZh!=null and \"\" neq engineZh'> AND (engine LIKE BINARY CONCAT('%',#{engineZh},'%')) </if> " +
            "<if test='param!=null and \"\" neq param'> AND (param LIKE BINARY CONCAT('%',#{param},'%')) </if> " +
            "<if test='createBy!=null and \"\" neq createBy'> AND (create_by LIKE BINARY CONCAT('%',#{createBy},'%')) </if> " +
            "<if test='status!=null'> AND (status = #{status}) </if> " +
            "<if test='region!=null and \"\" neq region'> AND (LOWER(region) LIKE BINARY LOWER(CONCAT('%',#{region},'%'))) </if> " +
            "<if test='user!=null and \"\" neq user'> AND (create_by LIKE BINARY CONCAT('%',#{user},'%')) </if> " +
            " ORDER BY update_time DESC" +
            "</script>"})
    Page<DataAPI> listByInfo(@Param("title") String title,
                             @Param("path") String path,
                             @Param("querySql") String querySql,
                             @Param("engineZh") String engineZh,
                             @Param("param") String param,
                             @Param("createBy") String createBy,
                             @Param("status") Integer status,
                             @Param("user") String user,
                             @Param("region") String region);

    /**
     * 根据MAP参数查询
     *
     * @param title title
     * @param path path
     * @param querySql querySql
     * @param engineZh engineZh
     * @param param param
     * @param createBy createBy
     * @param status status
     * @return
     */
    @Select({"<script>" +
            "SELECT * FROM data_api " +
            "WHERE 1=1 " +
            "<if test='title!=null and \"\" neq title'> AND (LOWER(title) LIKE LOWER(CONCAT('%',#{title},'%'))) </if> " +
            "<if test='path!=null and \"\" neq path'> AND (LOWER(path) LIKE LOWER(CONCAT('%',#{path},'%'))) </if> " +
            "<if test='querySql!=null and \"\" neq querySql'> AND (LOWER(query_sql) LIKE BINARY LOWER(CONCAT('%',#{querySql},'%'))) </if> " +
            "<if test='engineZh!=null and \"\" neq engineZh'> AND (LOWER(engine) LIKE LOWER(CONCAT('%',#{engineZh},'%'))) </if> " +
            "<if test='param!=null and \"\" neq param'> AND (LOWER(param) LIKE LOWER(CONCAT('%',#{param},'%'))) </if> " +
            "<if test='createBy!=null and \"\" neq createBy'> AND (LOWER(create_by) LIKE LOWER(CONCAT('%',#{createBy},'%'))) </if> " +
            "<if test='region!=null and \"\" neq region'> AND (LOWER(region) LIKE BINARY LOWER(CONCAT('%',#{region},'%'))) </if> " +
            "<if test='status!=null'> AND (status = #{status}) </if> " +
            " ORDER BY update_time DESC" +
            "</script>"})
    Page<DataAPI> listForAdmin(@Param("title") String title,
                               @Param("path") String path,
                               @Param("querySql") String querySql,
                               @Param("engineZh") String engineZh,
                               @Param("param") String param,
                               @Param("createBy") String createBy,
                               @Param("status") Integer status,
                               @Param("region") String region);

    /**
     * 根据ID查询
     *
     * @param id id
     * @return
     */
    @Select({"SELECT * FROM data_api WHERE id=#{id}"})
    DataAPI selectByID(@Param("id") Integer id);

    /**
     * 根据创建人查询
     *
     * @param user user
     * @return
     */
    @Select({"SELECT title FROM data_api WHERE create_by=#{user}"})
    ArrayList<String> selectTitleByUser(@Param("user") String user);

    /**
     * 根据创建人查询
     *
     * @param user user
     * @param id id
     * @return
     */
    @Select({"SELECT title FROM data_api WHERE create_by=#{user} AND id!=#{id}"})
    ArrayList<String> selectTitleByUserWithoutId(@Param("user") String user, @Param("id") Integer id);
}
