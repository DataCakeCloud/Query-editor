package com.ushareit.query.mapper;

import com.ushareit.query.bean.Chart;
import com.ushareit.query.bean.FavorTable;
import com.ushareit.query.repositry.mapper.CrudMapper;
import org.apache.ibatis.annotations.*;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author: wangsy1
 * @create: 2022-11-02 15:22
 */
@Mapper
public interface ChartMapper extends CrudMapper<Chart> {

    @Insert("insert into chart " +
            "(name," +
            "type," +
            "describe_chart," +
            "is_active," +
            "is_share," +
            "is_favorate," +
            "query_sql," +
            "param," +
            "uuid," +
            "engine," +
            "create_by," +
            "update_by," +
            "create_time," +
            "update_time,content,status)" +
            "VALUES(" +
            "#{data.name}," +
            "#{data.type}," +
            "#{data.describeChart}," +
            "#{data.isActive}," +
            "#{data.isShare}," +
            "#{data.isFavorate}," +
            "#{data.querySql}," +
            "#{data.param}," +
            "#{data.uuid}," +
            "#{data.engine}," +
            "#{data.createBy}," +
            "#{data.updateBy}," +
            "#{data.createTime}," +
            "#{data.updateTime},#{data.content},#{data.status})")
    @Options(useGeneratedKeys = true, keyProperty = "data.id")
    @Transactional(rollbackFor = Exception.class)
    boolean addChart(@Param("data")Chart addChartToSet);

    @Select("select uuid from chart where id=#{id} and create_by=#{name}")
    String selectUuidById(@Param("id")Integer id, @Param("name")String name);

    @Update("update chart set type=#{type}, param=#{params}, name=#{title}, " +
            "describe_chart=#{describe}, query_sql=#{sql}, engine=#{engine}, update_time=#{time} where id=#{id} and create_by=#{name}")
    boolean updateResult(@Param("type")String type, @Param("params")String params, @Param("title")String title,
                         @Param("describe")String describe, @Param("sql")String chartSql,
                         @Param("sql")String engine, @Param("time")LocalDateTime time,
                         @Param("id")Integer id, @Param("name")String name);

    @Update("update chart set uuid=#{uuid}, type=#{type}, param=#{params}, name=#{title}, " +
            "describe_chart=#{describe}, query_sql=#{sql}, engine=#{engine}, update_time=#{time} where id=#{id} and create_by=#{name}")
    boolean updateResultUuid(@Param("uuid")String uuid, @Param("type")String type, @Param("params")String params, @Param("title")String title,
                         @Param("describe")String describe, @Param("sql")String chartSql,
                         @Param("sql")String engine,  @Param("time")LocalDateTime time,
                         @Param("id")Integer id, @Param("name")String name);

    @Select("select * from chart where uuid=#{uuid} and is_active=#{active} and id!=#{cid}")
    List<Chart> selectListByUuid(@Param("uuid")String uuid,@Param("active")Integer active,@Param("cid")Integer cid);

    @Update("update chart set is_active=#{active} and update_time=#{time} where id=#{id} and create_by=#{name}")
    boolean del(@Param("id")Integer id, @Param("time")LocalDateTime time, @Param("active")Integer active, @Param("name")String name);

    @Select("select * from chart where id=#{id} and is_active=#{active}")
    List<Chart> getChart(@Param("id")Integer id, @Param("active")Integer active);

    @Select("<script>" +
            "select * from chart where create_by=#{name} and is_active=#{active} " +
            "<if test='title!=null and \"\" neq title'> AND name LIKE BINARY CONCAT('%',#{title},'%') </if> "
            + "order by create_time desc " +
            "</script>")
    List<Chart> getChartByCreat(@Param("name")String name, @Param("active")Integer active,@Param("title")String title);

    @Select("select uuid from chart where id=#{id} and is_active=#{active}")
    String getUuid(@Param("id")Integer id, @Param("active")Integer active);

    @Select("select * from chart where id=#{id} and is_active=#{active}")
    Chart getChartForView(@Param("id")Integer id, @Param("active")Integer active);

    @Select({"<script>" +
            "SELECT name FROM chart " +
            "WHERE name=#{title} " +
            "AND create_by=#{username}" +
            "AND is_active=#{active} " +
            "</script>"})
    List<String> selectByUsername(@Param("title") String title, @Param("username") String username, @Param("active") Integer active);

    @Select({"<script>" +
            "SELECT name FROM chart " +
            "WHERE name=#{title} " +
            "AND create_by=#{username}" +
            "AND is_active=#{active} " +
            "AND id!=#{id}" +
            "</script>"})
    List<String> selectByUsernameUpdate(@Param("title") String title, @Param("username") String username, @Param("active") Integer active,@Param("id")Integer id);

    @Update("update chart set is_favorate=#{active} where id=#{id}")
    void updateChartFavor(@Param("id")Integer id,@Param("active")Integer active);

    @Update("update chart set is_favorate=#{active} where id=#{id}")
    void delChartFavor(@Param("id")Integer id,@Param("active")Integer active);

    @Update("update chart set status=#{status}where id= #{id}")
    void updateStatus(@Param("id")Integer id, @Param("status")String status);
}
