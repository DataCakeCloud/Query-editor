package com.ushareit.query.mapper;

import com.ushareit.query.bean.AIChat;
import com.ushareit.query.repositry.mapper.CrudMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @author: tangjk
 * @create: 2022-01-19 15:24
 */
@Mapper
public interface AIChatMapper extends CrudMapper<AIChat> {
    @Insert("insert into ai_chat" +
            "(uuid, " +
            "user_name, " +
            "content, " +
            "reply) " +
            "values(" +
            "#{uuid}, " +
            "#{user_name}, " +
            "#{content}, " +
            "#{reply})")
    void addChat(@Param("uuid")String uuid,
                     @Param("user_name")String user_name,
                     @Param("content")String content,
                     @Param("reply")String reply);
}
