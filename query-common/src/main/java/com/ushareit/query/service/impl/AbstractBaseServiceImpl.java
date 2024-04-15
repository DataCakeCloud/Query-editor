package com.ushareit.query.service.impl;

import com.ushareit.query.bean.BaseEntity;
import com.ushareit.query.service.BaseService;
import com.ushareit.query.constant.BaseResponseCodeEnum;
import com.ushareit.query.exception.ServiceException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Abstract base service
 *
 * @author wuyan
 * @date 2020/06/15
 */
public abstract class AbstractBaseServiceImpl<T extends BaseEntity> extends AbstractCrudServiceImpl<T> implements BaseService<T> {
    /**
     * @param recordFromDb  from Db
     * @param recordFromWeb from Web
     */
    public void checkOnUpdate(T recordFromDb, T recordFromWeb) {
        if (recordFromDb != null && recordFromWeb != null) {
            if (!recordFromDb.getId().equals(recordFromWeb.getId())) {
                throw new ServiceException(BaseResponseCodeEnum.NAME_IS_NOT_UNIQUE);
            }
        }
    }

    /**
     * 名字匹配正则表达式
     * @param string 要匹配的字符串
     * @param pattern 正则表达式模板
     * @return
     */
    public Boolean match(String string, String pattern) {
        return match(string,pattern,null);
    }

    public Boolean match(String string, String pattern, Integer flags) {
        Pattern r = Pattern.compile(pattern);
        if (flags != null) {
            r = Pattern.compile(pattern, flags);
        }
        Matcher m = r.matcher(string);
        return m.matches();
    }
}
