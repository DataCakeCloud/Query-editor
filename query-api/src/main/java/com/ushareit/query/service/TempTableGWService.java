package com.ushareit.query.service;

import com.ushareit.query.bean.CreateTempTable;
import com.ushareit.query.bean.CurrentUser;
import org.springframework.web.multipart.MultipartFile;

import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: huyx
 * @create: 2022-01-19 15:24
 */

public interface TempTableGWService extends BaseService<CreateTempTable>{
    /**
     * 第二次上传文件至cloud
     *
     * @param params
     * @param file
     */
    HashMap<String, Object> uploadRepeat(Map<String, Object> params, MultipartFile file,
                                         String userInfo, CurrentUser currentUser) throws ParseException;

    /**
     * 创建临时表
     *
     * @param database
     * @param table
     * @param comment
     * @param location
     * @param field
     */
    HashMap<String, Object> execute(String database, String table, String comment,
                                    String location, List<Object> field, String engine,
                                    String region, CurrentUser currentUser) throws ParseException;
}

