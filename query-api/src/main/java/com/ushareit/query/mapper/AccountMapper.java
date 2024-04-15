package com.ushareit.query.mapper;

import com.ushareit.query.bean.Account;
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
public interface AccountMapper extends CrudMapper<Account> {
    /**
     * 查询全部公共账号
     *
     */
    @Select({"SELECT * FROM account"})
    List<Account> listAll();
}
