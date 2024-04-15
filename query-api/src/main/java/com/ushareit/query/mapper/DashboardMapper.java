package com.ushareit.query.mapper;

import com.ushareit.query.bean.Chart;
import com.ushareit.query.bean.ClassificationDash;
import com.ushareit.query.bean.Dashboard;
import com.ushareit.query.repositry.mapper.CrudMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author: wangsy1
 * @create: 2022-11-25 15:22
 */
@Mapper
public interface DashboardMapper extends CrudMapper<Dashboard> {

    @Select("select is_schedule from dashboard where id=#{id} and create_by=#{name} and is_active=#{active}")
    Integer selectDashById(@Param("id")Integer id, @Param("name")String name, @Param("active")Integer active);

    @Select("select * from dashboard where id=#{id} and is_active=#{active}")
    Dashboard selectById(@Param("id")Integer id, @Param("active")Integer active);

    @Update("update dashboard set is_active=#{active} where id=#{id}")
    void updateByActive(@Param("id")Integer id, @Param("active")Integer active);

    @Update("update dashboard set is_active=#{active},is_schedule=#{active} where id=#{id}")
    void updateByActiveSche(@Param("id")Integer id, @Param("active")Integer active);

    @Select("select is_schedule from dashboard where id=#{id} and is_active=#{active}")
    Integer selectScheById(@Param("id")Integer id, @Param("active")Integer active);

    @Select({"<script>" +
            "SELECT name FROM dashboard " +
            "WHERE name=#{title} " +
            "AND create_by=#{username}" +
            "AND is_active=#{active}" +
            "<if test='id!=null and \"\" neq id'> AND id != #{id} </if> " +
            "</script>"})
    List<String> selectByUsername(@Param("title") String title, @Param("username") String username, @Param("id") Integer id, @Param("active") Integer active);

    @Select("select * from dashboard where id=#{id} and is_active=#{active}")
    List<Dashboard> selectDash(@Param("id")Integer id, @Param("active")Integer active);

    @Select("select * from dashboard where create_by=#{name} and is_active=#{active}")
    List<Dashboard> classiByname(@Param("name")String name, @Param("active")Integer active);

    @Select("select * from dashboard where create_by=#{name} and is_active=#{active}")
    List<Dashboard> getDashByCreat(@Param("name")String name, @Param("active")Integer active);

    @Select("select * from dashboard where classid=#{clid} and is_active=#{active}")
    List<Dashboard> selectDashByClassid(@Param("clid")Integer clid, @Param("active")Integer active);

    @Update("update dashboard set update_time=#{time}, classid=#{clid} where id=#{id} and create_by=#{name}")
    void moveClassId(@Param("id")Integer id, @Param("clid")Integer clid, @Param("name")String name, @Param("time")LocalDateTime time);

    @Update("update dashboard set name=#{data.name}, describe_dash=#{data.describeDash}, is_active=#{data.isActive}, is_share=#{data.isShare}, " +
            "is_schedule=#{data.isSchedule},param=#{data.param}, classid=#{data.classId}, crontab=#{data.crontab},update_time=#{time} where id=#{id}")
    void updateByDash(@Param("data")Dashboard dashboard,@Param("time")LocalDateTime time ,@Param("id")Integer id);

    @Update("update dashboard set is_favorate=#{active} where id=#{id}")
    void updateDashFavor(@Param("id")Integer id,@Param("active")Integer active);

    @Update("update dashboard set is_favorate=#{active} where id=#{id}")
    void delDashFavor(@Param("id")Integer id,@Param("active")Integer active);

    @Update("update dashboard set name = #{title}, update_time = #{time} where id = #{id} and create_by = #{name}")
    boolean editName(@Param("id") Integer id, @Param("time") LocalDateTime time, @Param("title") String title, @Param("name") String name);

    @Select("select crontab from dashboard where id=#{id} and is_active = #{active} and is_schedule = #{sche}")
    String getCrontab(@Param("id")Integer id,@Param("active")Integer active,@Param("sche")Integer sche);

}
