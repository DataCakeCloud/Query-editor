package com.ushareit.query.mapper;

import com.ushareit.query.bean.DataEngineer;
import com.ushareit.query.repositry.mapper.CrudMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: huyx
 * @create: 2022-01-19 15:24
 */
@Mapper
public interface DataEngineerMapper extends CrudMapper<DataEngineer> {
    /**
     * 根据uuid查询
     *
     * @param uuid uuid
     * @return
     */
    @Select({"SELECT * FROM data_engineer WHERE uuid=#{uuid} ORDER BY update_time desc LIMIT 1"})
    List<DataEngineer> getByUuid(@Param("uuid") String uuid);
}
