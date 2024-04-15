package com.ushareit.query.mapper;

import com.ushareit.query.bean.Chart;
import com.ushareit.query.bean.QueryData;
import com.ushareit.query.repositry.mapper.CrudMapper;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author: wangsy1
 * @create: 2022-11-25 15:22
 */
@Mapper
public interface QueryDataMapper extends CrudMapper<QueryData> {

    @Select("select * from query_data where uuid=#{uuid} and is_active=#{active}")
    List<QueryData> selectUuidForDe(@Param("uuid")String paramUuid,@Param("active")Integer active);

    @Select("select detaskid from query_data where uuid=#{uuid} and is_active=#{active}")
    Integer selectDeTaskId(@Param("uuid")String paramUuid, @Param("active")Integer active);

    @Update("update query_data set is_active=#{active} where uuid=#{uuid}")
    boolean del(@Param("uuid")String uuid, @Param("active")Integer active);

    @Insert("insert into query_data " +
            "(uuid," +
            "region," +
            "db," +
            "detaskid," +
            "is_active," +
            "create_time) " +
            "VALUES(" +
            "#{uuid}," +
            "#{region}," +
            "#{db}," +
            "#{taskid}," +
            "#{active}," +
            "#{time})")
    boolean addTaskIdToQueryData(@Param("taskid")Integer taskid, @Param("uuid")String uuid, @Param("region")String region,
                                 @Param("db")String db, @Param("time")LocalDateTime time, @Param("active")Integer active);

    @Select("select * from query_data where uuid=#{uuid}")
    QueryData getQd(@Param("uuid")String paramUuid);

}
