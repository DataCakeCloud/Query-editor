package com.ushareit.query.mapper;

import com.ushareit.query.bean.LogView;
import com.ushareit.query.repositry.mapper.CrudMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface LogViewMapper extends CrudMapper<LogView> {

    @Insert("insert into logview(type,view_id,create_by,create_time)values(#{type}, #{viewid}, #{name}, #{time})")
    void addLog(@Param("name")String name, @Param("viewid")Integer viewid, @Param("time")LocalDateTime time, @Param("type")String type);
}
