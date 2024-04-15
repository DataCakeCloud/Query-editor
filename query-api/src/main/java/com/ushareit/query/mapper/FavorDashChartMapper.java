package com.ushareit.query.mapper;

import com.ushareit.query.bean.FavorDashChart;
import com.ushareit.query.repositry.mapper.CrudMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface FavorDashChartMapper extends CrudMapper<FavorDashChart> {

    @Update("update favordashchart set is_active = #{active}, update_time = #{time} where favor_id = #{id} and create_by = #{name} and type=#{type}")
    boolean del(@Param("id") Integer id, @Param("time") LocalDateTime time, @Param("active") Integer active, @Param("name") String name, @Param("type")String type);

    @Select("select favor_id from (select favor_id,a.create_time,c.is_active from favordashchart a\n"
            + "left join chart c on c.id  =a.favor_id  \n"
            + "where a.create_by=#{name} and a.`type`=#{type} and a.is_active=#{active})b where b.is_active=#{active} order by create_time desc")
    List<Integer> getFavorChart(@Param("name")String name, @Param("type")String type, @Param("active")Integer active);

    @Select("select favor_id from (select favor_id,a.create_time,c.is_active from favordashchart a\n"
            + "left join dashboard c on c.id  =a.favor_id  \n"
            + "where a.create_by=#{name} and a.`type`=#{type} and a.is_active=#{active})b where b.is_active=#{active} order by create_time desc")
    List<Integer> getFavorDash(@Param("name")String name, @Param("type")String type, @Param("active")Integer active);

    @Select("select * from favordashchart where create_by=#{name} and type=#{type} and is_active=#{active} and favor_id = #{favorid}")
    FavorDashChart getfadc(@Param("name")String name, @Param("active")Integer active, @Param("type")String type, @Param("favorid")Integer favorid);

    @Update("update favordashchart set is_active=#{active}, update_time = #{time} where id=#{id}")
    void updateStatus(@Param("id") Integer id, @Param("time") LocalDateTime time, @Param("active") Integer active);
}
