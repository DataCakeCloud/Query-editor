package com.ushareit.query.mapper;

import com.ushareit.query.bean.Meta;
import com.ushareit.query.bean.ShareGrade;
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
public interface ShareGradeMapper extends CrudMapper<ShareGrade> {
	@Select({"<script>" +
                 "SELECT * FROM share_grade WHERE sharee = #{sharee}" + 
                 "<if test='sharer!=null and \"\" neq sharer'> AND sharer LIKE BINARY CONCAT('%',#{sharer},'%') </if>" +
                 "</script>"})
    List<ShareGrade> listBySharee(@Param("sharee") String sharee,
    		@Param("sharer") String sharer);
}
