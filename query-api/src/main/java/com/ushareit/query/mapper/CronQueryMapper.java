package com.ushareit.query.mapper;

import com.github.pagehelper.Page;
import com.ushareit.query.bean.CronQuery;
import com.ushareit.query.repositry.mapper.CrudMapper;
import org.apache.ibatis.annotations.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Mapper
public interface CronQueryMapper extends CrudMapper<CronQuery> {
    @Insert("insert into cron_query" +
            "(task_id, " +
            "task_name, " +
            "schedule, " +
            "email, " +
            "start_time, " +
            "end_time, " +
            "origin_sql, " +
            "user_name, " +
            "user_group, " +
            "engine, " +
            "region, " +
            "catalog, " +
            "db, " +
            "status, " +
            "create_time) " +

            "values(" +
            "#{task_id}, " +
            "#{task_name}, " +
            "#{schedule}, " +
            "#{email}, " +
            "#{start_time}, " +
            "#{end_time}, " +
            "#{origin_sql}, " +
            "#{user_name}, " +
            "#{user_group}, " +
            "#{engine}, " +
            "#{region}, " +
            "#{catalog}, " +
            "#{db}, " +
            "#{status}, " +
            "#{create_time})")
    boolean insertHistory(@Param("task_id") Integer task_id,
                          @Param("task_name") String task_name,
                          @Param("schedule") String schedule,
                          @Param("email") String email,
                          @Param("start_time") String start_time,
                          @Param("end_time") String end_time,
                          @Param("origin_sql") String origin_sql,
                          @Param("user_name") String user_name,
                          @Param("user_group") String user_group,
                          @Param("engine") String engine,
                          @Param("region") String region,
                          @Param("catalog") String catalog,
                          @Param("db") String database,
                          @Param("status") Integer status,
                          @Param("create_time") LocalDateTime create_time);

    @Select({"SELECT * FROM cron_query " +
            "WHERE task_id = #{task_id}"})
    CronQuery selectTaskInfo(@Param("task_id") Integer taskId);

    @Select({"<script>" +
            "SELECT * FROM cron_query " +
            "WHERE status != 2 AND user_group = #{curUserGroup} " +
            "<if test='owner != null and owner != \"\"'> AND user_name = #{owner} </if>" +
            "<if test='task_id != null and task_id != \"\"'> AND task_id LIKE BINARY CONCAT('%',#{task_id},'%') </if>" +
            "<if test='origin_sql != null and origin_sql != \"\"'> AND LOWER(origin_sql) LIKE BINARY LOWER(CONCAT('%',#{origin_sql},'%')) </if>" +
            "<if test='user_name != null and user_name != \"\"'> AND LOWER(user_name) LIKE BINARY LOWER(CONCAT('%',#{user_name},'%')) </if>" +
            "<if test='task_name != null and task_name != \"\"'> AND LOWER(task_name) LIKE BINARY LOWER(CONCAT('%',#{task_name},'%')) </if>" +
            "ORDER BY create_time DESC " + //
            "</script>"})
    Page<CronQuery> selectHistory(@Param("task_id") Integer task_id,
                                  @Param("origin_sql") String origin_sql,
                                  @Param("user_name") String user_name,
                                  @Param("task_name") String task_name,
                                  @Param("owner") String owner,
                                  @Param("curUserGroup") String curUserGroup);

    @Update("UPDATE cron_query " +
            "SET status = #{newStatus} " +
            "WHERE task_id = #{task_id}")
    boolean changeStatus(@Param("task_id") Integer task_id,
                      @Param("newStatus") Integer newStatus);
}
