package com.ushareit.query.mapper;

import com.ushareit.query.bean.CreateTempTable;
import com.ushareit.query.bean.User;
import com.ushareit.query.repositry.mapper.CrudMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * @author: huyx
 * @create: 2022-01-19 15:24
 */
@Mapper
public interface CreateTempTableMapper extends CrudMapper<CreateTempTable> {
}
