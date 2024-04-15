package com.ushareit.query.mapper;

import com.ushareit.query.bean.ErrorInfo;
import com.ushareit.query.repositry.mapper.CrudMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * @author: tangjk
 * @create: 2022-01-19 15:24
 */
@Mapper
public interface ErrorInfoMapper extends CrudMapper<ErrorInfo> {
    /**
     * get all error_info
     *
     * @return
     */
    @Override
    @Select({"SELECT * FROM error_info"})
    List<ErrorInfo> selectAll();
}
