package com.ushareit.query.service.impl;

import com.alibaba.fastjson.JSON;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ushareit.query.bean.DataAPI;
import com.ushareit.query.bean.Meta;
import com.ushareit.query.bean.QueryHistory;
import com.ushareit.query.bean.User;
import com.ushareit.query.mapper.QueryHistoryMapper;
import com.ushareit.query.web.utils.*;
import com.ushareit.query.constant.BaseResponseCodeEnum;
import com.ushareit.query.exception.ServiceException;
import com.ushareit.query.mapper.DataAPIMapper;
import com.ushareit.query.mapper.MetaMapper;
import com.ushareit.query.mapper.UserMapper;
import com.ushareit.query.repositry.mapper.CrudMapper;
import com.ushareit.query.service.DataAPIService;
import io.swagger.models.auth.In;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.*;


/**
 * @author: huyx
 * @create: 2022-02-08 15:24
 */
@Slf4j
@Service
@Setter
public class DataAPIServiceImpl extends AbstractBaseServiceImpl<DataAPI> implements DataAPIService {
    @Resource
    private DataAPIMapper dataAPIMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private MetaMapper metaMapper;

    @Resource
    private QueryHistoryMapper queryHistoryMapper;

    @Override
    public CrudMapper<DataAPI> getBaseMapper() { return dataAPIMapper; }

    @Value("${api.basePath}")
    private String basePath;

    @Override
    public Object save(DataAPI dataAPI) {
        // 保存部分查询信息
        super.save(dataAPI);

        return dataAPI;
    }

    @Override
    public void update(DataAPI dataAPI){
        // 更新部分查询信息
        super.update(dataAPI);
    }

    @Override
    public PageInfo<DataAPI> getDataAPI(Integer pageNum, Integer pageSize,
    		String title, String path, String querySql,
    		String engineZh, String param, String createBy,
    		Integer status, String user, String region) {
        if (status == 9) {
            status = null;
        }
        User userInfo = userMapper.selectByName(user);
        Integer isAdmin = 0;
        if (userInfo != null) {
            isAdmin = userInfo.getIsAdmin();
        }
        if (isAdmin == 1) {
            PageHelper.startPage(pageNum, pageSize);
            List<DataAPI> pageRecord = dataAPIMapper.listForAdmin(title, path, querySql,
            		engineZh, param, createBy, status, region);
            return new PageInfo<>(pageRecord);
        } else {
            PageHelper.startPage(pageNum, pageSize);
            List<DataAPI> pageRecord = dataAPIMapper.listByInfo(title, path, querySql,
            		engineZh, param, createBy, status, user, region);
            return new PageInfo<>(pageRecord);
        }
    }

    @Override
    public Map<String, Object> setCountInfo(PageInfo<DataAPI> apiPage) {
        ArrayList<Object> apiList = new ArrayList<>();
        List<DataAPI> pageList = apiPage.getList();
        Map<String, Object> pageObject = JSON.parseObject(JSON.toJSONString(apiPage));
        
        ArrayList<Integer> ids = new ArrayList<Integer>();
        ids.add(0);
        for (int i = 0; i < pageList.size(); i++) {
            ids.add(pageList.get(i).getId());
        }

        ArrayList<QueryHistory> historys = queryHistoryMapper.selectByApiIds(ids);
        Map<Integer, ArrayList<QueryHistory>> mapHis =
        		new HashMap<Integer, ArrayList<QueryHistory>>();
        if (null != historys) {
            for (int i = 0; i < historys.size(); ++i) {
                Integer apiId = historys.get(i).getApiId();
                if (mapHis.containsKey(apiId)) {
                	mapHis.get(apiId).add(historys.get(i));
                } else {
                	ArrayList<QueryHistory> countInfo = new ArrayList<>();
                	countInfo.add(historys.get(i));
                	mapHis.put(apiId, countInfo);
                }
            }
        }
        
        for (int i = 0; i < pageList.size(); i++) {
            Integer count = 0;
            ArrayList<QueryHistory> countInfo = new ArrayList<>();
            Map<String, Object > data = JSON.parseObject(JSON.toJSONString(pageList.get(i)));
            Integer apiId = pageList.get(i).getId();
            if (mapHis.containsKey(apiId)) {
                countInfo = mapHis.get(apiId);
                count = countInfo.size();
            }
            data.put("count", count);
            data.put("countInfo", countInfo);
            apiList.add(data);
        }
        pageObject.put("list", apiList);
        return pageObject;
    }

    @Override
    public String addDataApi(String title, String engine, String querySql,
    		String uuid, String param, String user, String region, String catalog) {
        String path = "";
        String engineZh = engine;
        if (engineZh.startsWith("presto")) {
            engineZh = "Ares";
        }
        DataAPI dataAPI = new DataAPI();

        ArrayList titleList = dataAPIMapper.selectTitleByUser(user);

        if (!titleList.contains(title)) {
            Meta engineInfo = metaMapper.listByKey(engine);
            if (null != engineInfo) {
                engineZh = engineInfo.getEngineName();  // 获取引擎标签
            }

            dataAPI.setTitle(title);
            dataAPI.setEngine(engine);
            dataAPI.setEngineZh(engineZh);
            dataAPI.setQuerySql(querySql);
            dataAPI.setUuid(uuid);
            dataAPI.setParam(param);
            dataAPI.setStatus(0);
            dataAPI.setCreateBy(user);
            dataAPI.setUpdateBy(user);
            dataAPI.setRegion(region);
            dataAPI.setCatalog(catalog);
            dataAPI.setCreateTime(new Timestamp(System.currentTimeMillis()));
            dataAPI.setUpdateTime(new Timestamp(System.currentTimeMillis()));

            super.save(dataAPI);

            Integer apiID = dataAPI.getId();

            path = DataAPIUtil.getAPIPath(basePath, apiID, param);

            dataAPI.setPath(path);
            update(dataAPI);

            return path;
        } else {
            return "1";
        }

    }

    @Override
    public String updateDataApi(Integer id, String title, String engine, String querySql,
    		String uuid, String param, String user, String region, String catalog) {
        String path = "";
        String engineZh = engine;
        if (engineZh.startsWith("presto")) {
            engineZh = "Ares";
        }

        ArrayList titleList = dataAPIMapper.selectTitleByUserWithoutId(user, id);

        if (!titleList.contains(title)) {
            DataAPI dataAPI = dataAPIMapper.selectByID(id);

            Meta engineInfo = metaMapper.listByKey(engine);
            if (null != engineInfo) {
                engineZh = engineInfo.getEngineName();
            }

            path = DataAPIUtil.getAPIPath(basePath, id, param);

            dataAPI.setTitle(title);
            dataAPI.setEngine(engine);
            dataAPI.setEngineZh(engineZh);
            dataAPI.setQuerySql(querySql);
            dataAPI.setUuid(uuid);
            dataAPI.setParam(param);
            dataAPI.setStatus(0);
            dataAPI.setPath(path);
            dataAPI.setUpdateBy(user);
            dataAPI.setRegion(region);
            dataAPI.setCatalog(catalog);
            dataAPI.setUpdateTime(new Timestamp(System.currentTimeMillis()));
            super.update(dataAPI);

            return path;
        } else {
            return "1";
        }
    }

    @Override
    public String setAPIStatus(Integer id, Integer status) {
        DataAPI savedApi = getDataAPIByID(id);
        savedApi.setStatus(status);
        super.update(savedApi);
        return "success";
    }

    @Override
    public DataAPI getDataAPIByID(Integer id) {
        return dataAPIMapper.selectByID(id);
    }

    @Override
    public String deleteBatch(String id) {
        String delimeter = ",";
        String[] idList = id.split(delimeter);
        for(int i =0; i < idList.length ; i++){
            super.delete(idList[i]);
        }
        return "success";
    }

    @Override
    public ArrayList<String> getTitleList(String user) {
        ArrayList titleList = dataAPIMapper.selectTitleByUser(user);
        return titleList;
    }

    @Override
    public String nameCheck(ArrayList<String> titleList, String title) {
        String isExisted = "0";

        if (titleList.contains(title)) {
            isExisted = "1";
        }

        return isExisted;
    }

    @Override
    public String getUuid() {
        return UUID.randomUUID().toString().replace("-", "").toLowerCase();
    }

    @Override
    public String getQuerySql(Map<String, Object> map, String querySql, String param) {
        Set<String> keys = map.keySet();
        for(String key :keys){
            if (!key.equals("id")) {
                String temp = "\\{" + "\\{" + key + "}}";
                querySql = querySql.replaceAll("\\{" + "\\{" + key + "}}", map.get(key).toString());
                querySql = querySql.replaceAll("\\{" + "\\{" + " " + key + " " + "}}", map.get(key).toString());
//                String strStart = "{{";
//                String strEnd = "}}";
//                int strStartIndex = querySql.indexOf(strStart);
//                int strEndIndex = querySql.indexOf(strEnd);
//                if (strStartIndex > 0 && strEndIndex > 0) {
//                    String tempParam = querySql.substring(strStartIndex+2, strEndIndex+2);
//                    if (tempParam.contains(key)) {
//                        querySql = querySql.replaceAll("\\{" + "\\{" + tempParam, map.get(key).toString());
//                    }
//                }
            }
        }
        return querySql;
    }

    @Override
    public void updateApiIdToHistory(Integer id, String uuid, String user) {
        queryHistoryMapper.updateApiId(uuid, id, user);
    }
}
