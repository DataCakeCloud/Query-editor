package com.ushareit.query.service;

import com.github.pagehelper.PageInfo;
import com.ushareit.query.bean.DataAPI;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: huyx
 * @create: 2022-01-19 15:24
 */
public interface DataAPIService extends BaseService<DataAPI> {
    /**获取保存的API列表
     *
     */
    PageInfo<DataAPI> getDataAPI(Integer pageNum, Integer pageSize, String title,
    		String path, String querySql, String engineZh, String param,
    		String createBy, Integer status, String user, String region);

    /**API列表增加count和countInfo
     *
     */
    Map<String, Object> setCountInfo(PageInfo<DataAPI> apiPage);

    /**生成API并保存
     *
     */
    String addDataApi(String title, String engine, String querySql,
    		String uuid, String param, String user,
    		String region, String catalog);

    /**更新API
     *
     */
    String updateDataApi(Integer id, String title, String engine,
    		String querySql, String uuid,
    		String param, String user, String region, String catalog);

    /**更新API上线/下线
     *
     */
    String setAPIStatus(Integer id, Integer status);

    /**根据ID获取保存的API
     *
     */
    DataAPI getDataAPIByID(Integer id);

    /**批量删除保存的API
     *
     */
    String deleteBatch(String id);

    /**获取当前用户的API名称
     *
     */
    ArrayList<String> getTitleList(String user);

    /**API名称唯一性校验
     *
     */
    String nameCheck(ArrayList<String> titleList, String title);

    /**生成api查询的uuid
     *
     */
    String getUuid();

    /**拼接参数生成可查询的querySql
     *
     */
    String getQuerySql(Map<String, Object> map, String querySql, String param);

    /**在历史记录中给api调用的查询添加api的id
     *
     */
    void updateApiIdToHistory(Integer id, String uuid, String user);
}
