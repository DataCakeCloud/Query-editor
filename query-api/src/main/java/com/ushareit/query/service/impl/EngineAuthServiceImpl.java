package com.ushareit.query.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ushareit.query.bean.EngineAuth;
import com.ushareit.query.bean.Meta;
import com.ushareit.query.mapper.EngineAuthMapper;
import com.ushareit.query.mapper.MetaMapper;
import com.ushareit.query.repositry.mapper.CrudMapper;
import com.ushareit.query.service.EngineAuthService;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;


/**
 * @author: huyx
 * @create: 2022-01-19 15:24
 */
@Slf4j
@Service
@ConfigurationProperties(prefix = "engine")
@Setter
public class EngineAuthServiceImpl extends AbstractBaseServiceImpl<EngineAuth> implements EngineAuthService {

    @Resource
    private EngineAuthMapper engineAuthMapper;

    @Resource
    private MetaMapper metaMapper;

    @Override
    public CrudMapper<EngineAuth> getBaseMapper() { return engineAuthMapper; }

    //@Value("${system.url}")
    //private String sysUrl;

//    Map<String, ArrayList<Map<String, String>>> obj;

    @Override
    public List<String> getUserAll() {
        /*OkHttpClient client = new OkHttpClient.Builder()
                .retryOnConnectionFailure(false)
                .connectionPool(new ConnectionPool(5,10, TimeUnit.SECONDS))
                .build();
        String url = sysUrl;
        Request request = new Request
                .Builder()
                .url(url)
                .build();
        Response response = null;*/
        ArrayList users = new ArrayList<>();
        /*try {
            response = client.newCall(request).execute();
            if (response.code() == 200) {
                String resultStr = response.body().string();
                JSONObject obj = JSONObject.parseObject(resultStr);
                JSONArray data = (JSONArray)obj.get("data");
                for(int i = 0; i < data.size(); i++) {
                    String userName = data.getJSONObject(i).getString("userName");
                    users.add(userName);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (response != null) {
                response.body().close();
            }
        }*/
        return users;
    }

    @Override
    public HashMap<String, List<String>> getEngineAll() {
        HashMap<String, List<String>> enginesMap = new HashMap<>();

        List<String> engineTypeList = metaMapper.listEngineType();
        for (int i = 0; i < engineTypeList.size(); i++) {
            ArrayList engineList = new ArrayList<>();
            String engineType = engineTypeList.get(i);
            if (!engineType.equals("presto") && !engineType.equals("spark") && !engineType.equals("ares") && !engineType.equals("smart")) {
                List<Meta> engineMetaList = metaMapper.listByType(engineType);
                for (int j = 0; j < engineMetaList.size(); j++) {
                    HashMap<String, String> engine = new HashMap<>();
                    engine.put("label", engineMetaList.get(j).getEngineName());
                    engine.put("value", engineMetaList.get(j).getEngineKey());
                    engineList.add(engine);
                }
                enginesMap.put(engineType, engineList);
            }
        }

//        for (String key : obj.keySet()) {
//            ArrayList engineList = new ArrayList<>();
//            ArrayList enginesList = obj.get(key);
//            for (int i = 0; i < enginesList.size(); i++) {
//                Map<String, String> engineMap = JSONObject.parseObject(JSONObject.toJSONString(enginesList.get(i)),HashMap.class);;
//                HashMap<String, String> engine = new HashMap<>();
//                engine.put("label", engineMap.get("name"));
//                engine.put("value", engineMap.get("key"));
//                engineList.add(engine);
//            }
//            enginesMap.put(key, engineList);
//        }

        return enginesMap;
    }

    @Override
    public PageInfo<EngineAuth> getEngineAuth(int pageNum, int pageSize, String info) {
        PageHelper.startPage(pageNum, pageSize);
        List<EngineAuth> pageRecord = engineAuthMapper.listByInfo(info);
        return new PageInfo<>(pageRecord);
    }

    @Override
    public Object save(EngineAuth engineAuth) {
        //1. 校验用户名是否唯一
        preCheckCommon(engineAuth);
        //2. 保存用户配置
        String engine = engineAuth.getEngine();
        super.save(engineAuth);

        return engineAuth;
    }

    @Override
    public void update(EngineAuth testFromWeb) {
        //1. 校验用户名是否唯一
        preCheckCommon(testFromWeb);
        //2. 保存用户配置
        super.update(testFromWeb);
    }

    private void preCheckCommon(EngineAuth engineAuth) {
        //1. name不重复校验
        super.checkOnUpdate(super.getByName(engineAuth.getName()), engineAuth);
    }
}


