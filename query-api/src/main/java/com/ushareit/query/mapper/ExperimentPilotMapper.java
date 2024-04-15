package com.ushareit.query.mapper;

import com.ushareit.query.bean.ExperimentPilot;
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
public interface ExperimentPilotMapper extends CrudMapper<ExperimentPilot> {
}
