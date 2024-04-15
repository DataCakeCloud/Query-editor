package com.ushareit.query.mapper;

import com.ushareit.query.bean.Classification;
import com.ushareit.query.bean.ClassificationDash;
import com.ushareit.query.repositry.mapper.CrudMapper;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ClassificationDashMapper extends CrudMapper<ClassificationDash> {

    @Select("select * from classificationdash where create_by=#{name} and is_active=#{active}")
    List<ClassificationDash> classiByname(@Param("name")String name, @Param("active")Integer active);

    @Select("select id from classificationdash where create_by=#{name} and is_active=#{active} and level=#{level}")
    Integer getIdByCreate(@Param("name")String name, @Param("level")Integer level, @Param("active")Integer active);

    @Select({"<script>" +
            "SELECT name FROM classificationdash " +
            "WHERE name=#{title} " +
            "AND create_by=#{username}" +
            "AND is_active=#{active}" +
            "<if test='id!=null and \"\" neq id'> AND id != #{id} </if> " +
            "</script>"})
    List<String> selectByUsername(@Param("title") String title, @Param("username") String username, @Param("id") Integer id, @Param("active") Integer active);

    @Update("update classificationdash set name = #{title}, update_time = #{time} where id = #{id} and create_by = #{name}")
    boolean edit(@Param("id") Integer id, @Param("time") LocalDateTime time, @Param("title") String title, @Param("name") String name);

    @Update("update classificationdash set is_active=#{active}, update_time=#{time} where id= #{id}")
    void deleteById(@Param("id") Integer id, @Param("active")Integer active, @Param("time")LocalDateTime time);

    @Select("select * from classificationdash where id=#{parent}")
    ClassificationDash selectParent(@Param("parent")Integer parent);

    @Select("select * from classificationdash where parent_id=#{id} and is_active=#{active}")
    List<ClassificationDash> selectChild(@Param("id")Integer id,@Param("active")Integer active);

    @Update("update classificationdash set parent_id = #{parentid}, level = #{level}, update_time = #{time} where id = #{id} and create_by = #{name}")
    void move(@Param("id") Integer id,@Param("time") LocalDateTime time,@Param("parentid") Integer parentId,@Param("name") String name, @Param("level")String level);

    @Select({"SELECT * FROM classificationdash WHERE create_by=#{name} and is_active=#{active} and level=#{level} and name=#{title}"})
    List<Classification> selectByNameTitle(@Param("name") String name,@Param("active")Integer active,@Param("level")Integer level,@Param("title")String title);

    @Insert("INSERT INTO classificationdash " +
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

    @Select("select level from classificationdash where id=#{id} and is_active=#{active}")
    Integer getLevel(@Param("id") Integer id, @Param("active")Integer active);
}
