package com.ushareit.query.service;

import com.github.pagehelper.PageInfo;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: tianxu
 * @create: 2022-02-10
 */

public interface TaskService {
    /**
     * 执行查询任务
     *
     * @param uuid
     * @param engine
     * @param querySql
     * @param isDatabend
     * @param user
     *
     */
    HashMap<String, Object> execute(String uuid, String engine, String querySql,
    		Integer isDatabend, Integer confirmedSmart,
    		String user, String region, String catalog, String database,
    		String groupId, int tenantId, String tenantName, Integer taskId) throws ParseException, SQLException;

    /**
     * 执行异步查询任务
     *
     * @param uuid
     * @param engine
     * @param querySql
     * @param querySqlParam
     *
     * @param user
     *
     */
    @Async
    void executeMysqlAsyn(String uuid, String engine, String querySql,
    		String querySqlParam, JSONObject param,
    		String user, String region, String catalog,
    		String groupId, int tenantId, String tenantName) throws ParseException, SQLException;

    /**
     * 执行mysql查询任务，获取sample数据
     *
     * @param uuid
     * @param engine
     * @param querySql
     * @param user
     *
     */
    HashMap<String, Object> executeMysqlSample(String uuid, String engine, String querySql, String user) throws ParseException, SQLException;

    /**
     * 判断mysql查询任务是否异步执行
     *
     * @param engine
     *
     */
    Integer getMysqlAsync(String engine);

    /**
     * 保存带参数的sql
     *
     * @param uuid
     * @param querySqlParam
     * @param param
     *
     */
    void saveQuerySqlParam(String uuid, String querySqlParam, JSONObject param);

    /**
     * 取消正在执行的任务
     *
     * @param uuid
     * @param user
     *
     */
    @Async
    HashMap<String, Object> cancel(String uuid, String user, String tenantName) throws IOException, InterruptedException;

    /**
     * 下载历史数据任务
     *
     * @param uuid
     * @param user
     *
     */
    ResponseEntity<Object> download(String uuid, String user) throws Exception;

    String downloadToNative(String uuid, String user) throws Exception;

    /**
     * 查询历史任务的sql
     *
     * @param uuid
     * @param user
     *
     */
    HashMap<String, Object> historySql(String uuid, String user) throws Exception;

    /**
     * 查询历史任务的数据
     *
     * @param uuid
     * @param user
     *
     */
    HashMap<String, Object> historyData(String uuid, String user) throws Exception;

    /**
     * 获取spark查询的日志url
     *
     * @param uuid
     * @param user
     */
    HashMap<String, Object> queryLog(String uuid, String user) throws Exception;

    HashMap<String, Object> queryNewLog(String uuid, String user, Long from) throws Exception;

    /**
     * 已保存查询的分享
     *
     * @param user
     * @param ids
     */
    HashMap<String, Object> shareId(Integer ids, String user);

    /**
     * 获取mysql异步查询结束后的下载量
     *
     * @param user
     * @param uuid
     */
    HashMap<String, Object> getFileSize(String uuid, String user);

    /**
     * 获取uuidList的状态
     *
     * @param uuidList
     */
    HashMap<String, Object> getStatus(JSONArray uuidList);

    /**
     * 异步下载csv，探查数据
     * @param uuid
     * @param user
     * @param type
     * @param updateColumnType
     */
    @Async
    void probeAsyn(String uuid, String user, JSONArray type,
                   boolean updateColumnType, JSONObject sample, String tenantName);

    /*
    * 异步取消
    */
    @Async
    void cancelAsync(Map<String, String> info, String tenantName) throws InterruptedException;
    
    int getShare(String sharee, Integer gradeID);
    
    public HashMap<String, String> testErrInfo(String err);
    
    HashMap<String, Object> statsInfo(Integer step, String uuid, String query_id);
    
    Map<String, Object> getShares(int pageNum, int pageSize,
            String share_sql, String sharer, String engine,
            String region, String sharee);

    HashMap<String, Object> checkSmartEngine(String uuid, String querySql,
                                                    String user, String region);

    Map<String, String> transSQLtoSpark(String querySql, String user);

    boolean isInExpBlacklist(int exp_id, String name);
    boolean isInExpWhitelist(int exp_id, String name);
}
