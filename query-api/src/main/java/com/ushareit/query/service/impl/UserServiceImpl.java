package com.ushareit.query.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ushareit.query.bean.Account;
import com.ushareit.query.bean.Meta;
import com.ushareit.query.bean.Region;
import com.ushareit.query.bean.User;
import com.ushareit.query.mapper.AccountMapper;
import com.ushareit.query.mapper.MetaMapper;
import com.ushareit.query.mapper.UserMapper;
import com.ushareit.query.mapper.RegionMapper;
import com.ushareit.query.repositry.mapper.CrudMapper;
import com.ushareit.query.service.UserService;
import com.ushareit.query.web.utils.ClusterManagerUtil;
import com.ushareit.query.web.utils.CommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * @author: huyx
 * @create: 2022-01-19 15:24
 */
@Slf4j
@Service
public class UserServiceImpl extends AbstractBaseServiceImpl<User> implements UserService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private MetaMapper metaMapper;

    @Resource
    private AccountMapper accountMapper;

    @Resource
    private RegionMapper regionMapper;

    @Override
    public CrudMapper<User> getBaseMapper() { return userMapper; }

    @Value("${cluster-manager.url}")
    private String urlClusterManager;


//    @Override
//    public String getUserGroup(String name) {
//        List<Account> account = accountMapper.listAll();
//        JSONObject groupAccount = CommonUtil.getUserGroup(name, account, adminUsername, adminPassword);
//        String group = groupAccount.getString("group");
//
//        return group;
//
//    }

    @Override
    public List<String> getUserEngine(String name, String region) {
        ArrayList userEngine = new ArrayList<>();
        ArrayList userEngineOther = new ArrayList<>();
        if (null == region) {
        	region = "";
        } else {
        	region = region.trim();
        }

        List<Meta> userEngineSmartList = metaMapper.listForSmart();
        for (int i = 0; i < userEngineSmartList.size(); i++) {
            Meta engineSmart = userEngineSmartList.get(i);
            if (!region.equalsIgnoreCase("") && !region.equals(engineSmart.getRegion())) {
            	continue;
            }
            HashMap<String, String> userEngineSmart = new HashMap<>();
            userEngineSmart.put("label", engineSmart.getEngineName());
            userEngineSmart.put("value", engineSmart.getEngineKey());
            userEngineSmart.put("database", "");
            userEngine.add(userEngineSmart);
        }

        List<Meta> userEnginePrestoList = metaMapper.listForPresto();
        for (int i = 0; i < userEnginePrestoList.size(); i++) {
            Meta enginePresto = userEnginePrestoList.get(i);
            if (!region.equalsIgnoreCase("") && !region.equalsIgnoreCase(enginePresto.getRegion())) {
            	continue;
            }
            HashMap<String, String> userEnginePresto = new HashMap<>();
            userEnginePresto.put("label", enginePresto.getEngineName());
            userEnginePresto.put("value", enginePresto.getEngineKey());
            userEnginePresto.put("database", "");
            userEngine.add(userEnginePresto);
        }

        List<Meta> userEngineSparkList = metaMapper.listForSpark();
        for (int i = 0; i < userEngineSparkList.size(); i++) {
            Meta engineSpark = userEngineSparkList.get(i);
            if (!region.equalsIgnoreCase("") && !region.equalsIgnoreCase(engineSpark.getRegion())) {
            	continue;
            }
            HashMap<String, String> userEngineSpark = new HashMap<>();
            userEngineSpark.put("label", engineSpark.getEngineName());
            userEngineSpark.put("value", engineSpark.getEngineKey());
            userEngineSpark.put("database", "");
            userEngine.add(userEngineSpark);
        }

        List<Meta> userEngineAresList = metaMapper.listForAres();
        for (int i = 0; i < userEngineAresList.size(); i++) {
            Meta engineAres = userEngineAresList.get(i);
            if (!region.equalsIgnoreCase("") && !region.equalsIgnoreCase(engineAres.getRegion())) {
            	continue;
            }
            HashMap<String, String> userEngineAres = new HashMap<>();
            userEngineAres.put("label", engineAres.getEngineName());
            userEngineAres.put("value", engineAres.getEngineKey());
            userEngineAres.put("database", "");
            userEngine.add(userEngineAres);
        }

        String otherEngines = userMapper.selectEngineByName(name);
        JSONArray engines = JSONArray.parseArray(otherEngines);
        if (engines != null) {
            for(int i = 0; i < engines.size(); i++) {
                HashMap<String, String> userEngineOtherMap = new HashMap<>();
                JSONObject engine = engines.getJSONObject(i);
                String engineValue = engine.getString("value");
                String engineLabel = engine.getString("label");
                String engineDatabase = "";
                if (!region.equalsIgnoreCase("") && !region.equalsIgnoreCase(metaMapper.listRegionByKey(engineValue))) {
                	continue;
                }

                if (!engineValue.startsWith("presto") && !engineValue.startsWith("spark") && !engineValue.startsWith("ares")) {
                    engineDatabase = metaMapper.listByKey(engineValue).getEngineDatabase();
                }

                userEngineOtherMap.put("label", engineLabel);
                userEngineOtherMap.put("value", engineValue);
                userEngineOtherMap.put("database", engineDatabase);
                userEngineOther.add(userEngineOtherMap);
            }
            Collections.sort(userEngineOther, new Comparator<Map<String, Object>>() {
                @Override
                public int compare(Map<String, Object> o1, Map<String, Object> o2) {
                    String label1 = (String) o1.get("label");
                    String label2 = (String) o2.get("label");
                    return label1.compareToIgnoreCase(label2);
//                    return label1.compareTo(label2);//升序
                }
            });
        userEngine.addAll(userEngineOther);}
        return userEngine;
    }
    
    /*@Override
    public List<String> getRegions() {
    	ArrayList region_list = new ArrayList<>();
    	List<Region> regions = regionMapper.selectAll();
    	for (int i = 0; i < regions.size(); i++) {
    		Region region = regions.get(i);
            HashMap<String, String> cur_region = new HashMap<>();
            cur_region.put("name", region.getName());
            cur_region.put("name_zh", region.getNameZh());
            region_list.add(cur_region);
        }
    	return region_list;
    }*/
    
    @Override
    public List<HashMap<String, String>> getRegions(String userInfo, int tenantId) {
    	ArrayList<HashMap<String, String>> region_list = new ArrayList<HashMap<String, String>>();
    	try {
            String url = urlClusterManager + "/cluster-service/cloud/resource/search?&pageNum=1&pageSize=100";
            String resInfo = ClusterManagerUtil.getClusterManagerInfo(url, userInfo);
            Map content = JSON.parseObject(resInfo, Map.class);
            Map data = JSON.parseObject(content.get("data").toString(), Map.class);
            List listTenant = JSON.parseObject(data.get("list").toString(), List.class);
            for (int i = 0; i < listTenant.size(); ++i) {
                Map tenant = JSON.parseObject(listTenant.get(i).toString(), Map.class);
                //if (Integer.valueOf((String)tenant.get("tenantId")) == tenantId) {
                    String provider = (String)tenant.get("provider");
                    String region = (String)tenant.get("region");
                    String alias = (String)tenant.get("regionAlias");
                    String description = (String)tenant.get("description");
                    String name = provider + "_" + region;
                    HashMap<String, String> cur_region = new HashMap<>();
                    cur_region.put("name", name);
                    cur_region.put("name_zh", description);
                    cur_region.put("sort", "0");
                    if (alias.equalsIgnoreCase("ue1")) {
                        cur_region.put("name", "aws_ue1");
                        cur_region.put("name_zh", "AWS 美东");
                        cur_region.put("sort", "1");
                    } else if (alias.equalsIgnoreCase("sg1")) {
                        cur_region.put("name", "aws_sg");
                        cur_region.put("name_zh", "AWS 新加坡");
                        cur_region.put("sort", "2");
                    } else if (alias.equalsIgnoreCase("sg2")) {
                        cur_region.put("name", "huawei_sg");
                        cur_region.put("name_zh", "华为云 新加坡");
                        cur_region.put("sort", "3");
                    }
                    region_list.add(cur_region);
                //}
            }
    	} catch (Exception e) {
            log.error(String.format("There is an exception occurred while parse cluster info: %s",
            		CommonUtil.printStackTraceToString(e)));
            throw e;
        }
        Collections.sort(region_list, new Comparator<HashMap<String, String>>() {
            @Override
            public int compare(HashMap<String, String> r1, HashMap<String, String> r2) {
                return r1.get("sort").compareTo(r2.get("sort"));
            }
        });
    	return region_list;
    }
}
