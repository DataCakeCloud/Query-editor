package com.ushareit.query.mapper;

import com.github.pagehelper.Page;
import com.ushareit.query.bean.Classification;
import com.ushareit.query.bean.User;
import com.ushareit.query.repositry.mapper.CrudMapper;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author: wangsy1
 * @create: 2022-11-02 15:22
 */
@Mapper
public interface ClassificationMapper extends CrudMapper<Classification> {

    /**
     * 根据username查询
     *
     * @param title title
     * @param username username
     * @return
     */
    @Select({"<script>" +
            "SELECT name FROM classification " +
            "WHERE name=#{title} " +
            "AND create_by=#{username}" +
            "AND is_active=#{active}" +
            "<if test='id!=null and \"\" neq id'> AND id != #{id} </if> " +
            "</script>"})
    List<String> selectByUsername(@Param("title") String title, @Param("username") String username, @Param("id") Integer id, @Param("active") Integer active);

    @Update("update classification set name = #{title}, update_time = #{time} where id = #{id} and create_by = #{name}")
    boolean edit(@Param("id") Integer id, @Param("time") LocalDateTime time,@Param("title") String title,@Param("name") String name);

    @Update("update classification set is_active = #{active}, update_time = #{time} where id = #{id} and create_by = #{name}")
    boolean del(@Param("id") Integer id, @Param("time") LocalDateTime time,@Param("active") Integer active,@Param("name") String name);

    @Select("select * from classification where id=#{id}")
    Classification selectLevel(@Param("id")Integer id);

    @Select("select * from classification where parent_id=#{id}")
    List<Classification> selectChild(@Param("id")Integer id);

    @Select("select * from classification where id=#{parent}")
    Classification selectParent(@Param("parent")Integer parent);

    @Update("update classification set parent_id = #{parentid}, level = #{level}, update_time = #{time} where id = #{id} and create_by = #{name}")
    boolean move(@Param("id") Integer id,@Param("time") LocalDateTime time,@Param("parentid") Integer parentId,@Param("name") String name, @Param("level")String level);

    @Select("select * from classification where create_by = #{name} and is_active = #{isactive}")
    List<Classification> classiByname(@Param("name") String name,@Param("isactive") Integer isActive);

    @Select({"SELECT * FROM classification WHERE create_by=#{name} and is_active=#{active} and level=#{level} and name=#{title}"})
    List<Classification> selectByNameTitle(@Param("name") String name,@Param("active")Integer active,@Param("level")Integer level,@Param("title")String title);

    @Insert("INSERT INTO classification " +
            "(name," +
            "level," +
            "parent_id," +
            "is_active," +
            "is_query," +
            "create_by," +
            "update_by," +
            "create_time," +
            "update_time)\n" +
            "VALUES\n" +
            "(#{title}," +
            "#{level}," +
            "#{parent}," +
            "#{active}," +
            "#{level}," +
            "#{name}," +
            "#{name}," +
            "#{time}," +
            "#{time})")
    boolean addLevelFirst(@Param("name") String name,@Param("active")Integer active,@Param("level")Integer level,
                          @Param("title")String title,@Param("time")LocalDateTime time,@Param("parent")Integer parent);
}
