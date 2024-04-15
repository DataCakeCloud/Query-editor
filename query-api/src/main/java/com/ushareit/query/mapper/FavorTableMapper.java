package com.ushareit.query.mapper;

import com.ushareit.query.bean.FavorTable;
import com.ushareit.query.repositry.mapper.CrudMapper;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author: wangsy1
 * @create: 2022-11-02 15:22
 */
@Mapper
public interface FavorTableMapper extends CrudMapper<FavorTable> {

    /**
     * 根据username查询
     *
     * @param title title
     * @param username username
     * @return
     */
    @Select({"<script>" +
            "SELECT name FROM favortable " +
            "WHERE name=#{title} " +
            "AND create_by=#{username} " +
            "AND is_active=#{active} " +
            "AND region=#{favor.region} " +
            "AND catalog=#{favor.catalog} " +
            "AND db=#{favor.db}" +
            "<if test='id!=null and \"\" neq id'> AND id != #{id} </if> " +
            "</script>"})
    List<String> selectByUsername(@Param("title") String title, @Param("username") String username, @Param("id") Integer id, @Param("active") Integer active,@Param("favor")FavorTable favorTable);

    @Update("update favortable set is_active = #{active}, update_time = #{time} where id = #{id} and create_by = #{name}")
    boolean del(@Param("id") Integer id, @Param("time") LocalDateTime time,@Param("active") Integer active,@Param("name") String name);

    @Select("SELECT * FROM favortable WHERE create_by=#{name} and is_active=#{active} and region=#{region} and catalog=#{catalog}")
    List<FavorTable> tableList(@Param("name")String name,@Param("active") Integer active,@Param("region")String region,
                               @Param("catalog")String catalog);
}
