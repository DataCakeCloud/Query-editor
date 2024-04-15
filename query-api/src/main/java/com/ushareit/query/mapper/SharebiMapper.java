package com.ushareit.query.mapper;

import com.ushareit.query.bean.Chart;
import com.ushareit.query.bean.Sharebi;
import com.ushareit.query.repositry.mapper.CrudMapper;
import org.apache.ibatis.annotations.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author: wangsy1
 * @create: 2022-11-30 15:24
 */
@Mapper
public interface SharebiMapper extends CrudMapper<Sharebi> {

    @Select("select * from sharebi where id=#{id} and sharee=#{sharee}")
    Sharebi selectByGradeId(@Param("id")Integer id, @Param("sharee")String name);

    @Select("select share_id from (select share_id,a.create_time ,is_active from sharebi a\n"
            + "left join chart c on c.id  =a.share_id  where a.sharee=#{name} and a.`type`=#{type} ) b where b.is_active=1 order by create_time desc")
    List<Integer> getShareChart(@Param("name")String name, @Param("type")String type);

    @Select("select share_id from (select share_id,a.create_time ,is_active from sharebi a\n"
            + "left join dashboard c on c.id  =a.share_id  where a.sharee=#{name} and a.`type`=#{type} ) b where b.is_active=1 order by create_time desc")
    List<Integer> getShareDash(@Param("name")String name, @Param("type")String type);

    @Insert("insert into sharebi(sharer,sharee,type,share_id,grade,share_url,create_time)values" +
            "(#{data.sharer},#{data.sharee},#{data.type},#{data.shareId},#{data.grade},#{data.shareUrl},#{data.createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "data.id")
    @Transactional(rollbackFor = Exception.class)
    void addNewShare(@Param("data")Sharebi sh);

    @Select("select * from sharebi where sharer=#{sharer} and sharee=#{sharee} and type=#{type} and share_id=#{shid}")
    Sharebi selectByShare(@Param("sharer")String sharer,@Param("sharee")String sharee,@Param("type")String type,@Param("shid")Integer shid);
}
