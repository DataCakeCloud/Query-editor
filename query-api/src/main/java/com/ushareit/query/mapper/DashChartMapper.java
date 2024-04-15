package com.ushareit.query.mapper;

import com.ushareit.query.bean.DashChart;
import com.ushareit.query.repositry.mapper.CrudMapper;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author: wangsy1
 * @create: 2022-11-25 15:22
 */
@Mapper
public interface DashChartMapper extends CrudMapper<DashChart> {

    @Select("select * from dash_chart where chart_id=#{id} and create_by=#{name} and is_active=#{active}")
    List<DashChart> selectByChartId(@Param("id")Integer id, @Param("name")String name,@Param("active")Integer active);

    @Update("update dash_chart set is_active=#{active} and update_time=#{time} where chart_id=#{id}")
    boolean del(@Param("id")Integer id, @Param("time") LocalDateTime time, @Param("active")Integer active);

    @Select("select * from dash_chart where dashboard_id=#{id} and chart_id=#{chartid}  and is_active=#{active}")
    DashChart selectByIdChartId(@Param("id")Integer id, @Param("chartid")Integer chartId,@Param("active")Integer active);

    @Select("select chart_id from dash_chart where dashboard_id=#{dsid} and is_active=#{active}")
    List<Integer> selectByIdDashId(@Param("dsid")Integer dsid,@Param("active")Integer active);

    @Select("select dashboard_id from dash_chart where chart_id=#{cid} and dashboard_id!=#{did} and is_active=#{active}")
    List<Integer> selectByIdChartDashId(@Param("cid")Integer cid, @Param("did")Integer did,@Param("active")Integer active);

    @Insert("insert into dash_chart" +
            "(dashboard_id, " +
            "chart_id, " +
            "is_active, " +
            "create_by, " +
            "update_by, " +
            "create_time, " +
            "update_time) " +
            "values(" +
            "#{did}, " +
            "#{cid}, " +
            "#{active}, " +
            "#{create}, " +
            "#{create}, " +
            "#{time}, " +
            "#{time})")
    void addDashChart(@Param("did")Integer did, @Param("cid")Integer cid, @Param("active")Integer active,
                      @Param("create")String name, @Param("time")LocalDateTime time);

    @Select("select * from dash_chart where chart_id=#{id} and is_active=#{active}")
    List<DashChart> seldash(@Param("id")Integer id, @Param("active")Integer active);

    @Select("update dash_chart set is_active=#{active} where dashboard_id=#{did} and chart_id=#{cid}")
    void updateActive(@Param("did")Integer did,@Param("cid")Integer cid,@Param("active")Integer active);
}
