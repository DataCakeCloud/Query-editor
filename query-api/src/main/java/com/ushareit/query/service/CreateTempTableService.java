package com.ushareit.query.service;

import com.ushareit.query.bean.CreateTempTable;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: huyx
 * @create: 2022-01-19 15:24
 */

public interface CreateTempTableService extends BaseService<CreateTempTable>{

    /**
     * 表名重复校验
     *
     * @param tableList
     * @param tableName
     */
    String nameCheck(List<String> tableList, String tableName);

    /**
     * 第一次上传文件至cloud
     *
     * @param params
     * @param user
     * @param file
     */
    HashMap<String, Object> uploadNew(Map<String, Object> params, String user, MultipartFile file,
    		String userInfo, int tenantId);

    /**
     * 第二次上传文件至cloud
     *
     * @param params
     * @param user
     * @param file
     */
    HashMap<String, Object> uploadRepeat(Map<String, Object> params, String user, MultipartFile file,
    		String userInfo, int tenantId, String tenantName);

    /**
     * 创建临时表
     *
     * @param username
     * @param engine
     * @param database
     * @param table
     * @param comment
     * @param location
     * @param field
     */
    HashMap<String, Object> execute(String username, String engine, String database, String table, String comment, String location, List<Object> field, String tenantName, String region);
}
