package com.ushareit.query.mapper;

import com.ushareit.query.bean.Meta;
import com.ushareit.query.repositry.mapper.CrudMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @author: huyx
 * @create: 2022-01-19 15:24
 */
@Mapper
public interface MetaMapper extends CrudMapper<Meta> {
    /**
     * 查询smart引擎
     *
     */
    @Select({"SELECT * FROM engine WHERE engine_type = 'smart' ORDER BY CONVERT(engine_name USING gbk)"})
    List<Meta> listForSmart();

    /**
     * 查询presto引擎
     *
     */
    @Select({"SELECT * FROM engine WHERE engine_type = 'presto' ORDER BY CONVERT(engine_name USING gbk)"})
    List<Meta> listForPresto();

    /**
     * 查询spark引擎
     *
     */
    @Select({"SELECT * FROM engine WHERE engine_type = 'spark' ORDER BY CONVERT(engine_name USING gbk)"})
    List<Meta> listForSpark();

    /**
     * 查询basic引擎
     *
     */
    @Select({"SELECT * FROM engine WHERE engine_type = 'ares' ORDER BY CONVERT(engine_name USING gbk)"})
    List<Meta> listForAres();

    /**
     * 根据key查询引擎信息
     *
     * @param key key
     * @return
     *
     */
    @Select({"SELECT * FROM engine WHERE engine_key = #{key}"})
    Meta listByKey(@Param("key") String key);

    /**
     * 根据type查询引擎信息
     *
     * @param type type
     * @return
     *
     */
    @Select({"SELECT * FROM engine WHERE engine_type = #{type}"})
    List<Meta> listByType(@Param("type") String type);

    /**
     * 根据全部引擎信息
     *
     *
     */
    @Select({"SELECT engine_type FROM engine GROUP BY engine_type"})
    List<String> listEngineType();

    /**
     * 根据key查询引擎region
     *
     * @param key key
     * @return
     *
     */
    @Select({"SELECT region FROM engine WHERE engine_key = #{key}"})
    String listRegionByKey(@Param("key") String key);
}
