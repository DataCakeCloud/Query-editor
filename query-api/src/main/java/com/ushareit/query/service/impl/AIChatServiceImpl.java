package com.ushareit.query.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ushareit.query.bean.AIChat;
import com.ushareit.query.bean.CurrentUser;
import com.ushareit.query.mapper.AIChatMapper;
import com.ushareit.query.repositry.mapper.CrudMapper;
import com.ushareit.query.service.AIChatService;
import com.ushareit.query.service.MetaService;
import com.ushareit.query.service.MetaServiceGateway;
import com.ushareit.query.web.utils.CommonUtil;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Setter
public class AIChatServiceImpl extends AbstractBaseServiceImpl<AIChat> implements AIChatService {
    @Value("${aiservice.ai_chat}")
    private String aiChatUrl;

    @Resource
    private AIChatMapper aiChatMapper;

    @Resource
    private MetaServiceGateway metaService;

    @Override
    public CrudMapper<AIChat> getBaseMapper() { return aiChatMapper; }

    @Override
    public Map<String, Object> chat(String content, String uuid, String user) {
        Map<String, String> mapRequest = new HashMap<>();
        mapRequest.put("content", content);
        mapRequest.put("uuid", uuid);
        String body = JSONObject.toJSONString(mapRequest);
        String resInfo = CommonUtil.httpResult(aiChatUrl, false, body, null, uuid);
        Map<String, Object> result = JSON.parseObject(resInfo, Map.class);
        aiChatMapper.addChat(uuid, user, content, result.get("answer").toString());
        return result;
    }

    public Map<String, Object> tableInfo(List<Map<String, String>> tables, String uuid,
                                         String user, String tenantName,
                                         CurrentUser currentUser, String token) throws ParseException {
        Map<String, String> mapRequest = new HashMap<>();
        String content = String.format("我有一张表，表名是%s.%s，包含如下字段：",
                tables.get(0).get("database"),
                tables.get(0).get("table"));
        if (tables.size() > 1) {
            content = String.format("我有%d张表，", tables.size());
        }

        for (int i = 0; i < tables.size(); ++i) {
            String region = tables.get(i).get("region");
            String catalog = tables.get(i).get("catalog");
            String database = tables.get(i).get("database");
            String table = tables.get(i).get("table");
            String engine;
            if (catalog.equalsIgnoreCase("hive") || catalog.equalsIgnoreCase("iceberg")) {
                if (region.equalsIgnoreCase("aws_ue1")) {
                    engine = "presto_aws";
                } else if (region.equalsIgnoreCase("aws_sg")) {
                    engine = "presto_aws_sg";
                } else if (region.equalsIgnoreCase("sg3")) {
                    engine = "presto_sg3";
                } else {
                    engine = "presto_huawei";
                }
            } else {
                engine = catalog;
            }
            List<Map<String, String>> columns = metaService.getMetaColumn(engine,
                    user, catalog, database, table, tenantName, region, currentUser, token);
            if (0 == columns.size()) {
                Map<String, Object> result = new HashMap();
                result.put("code", "404");
                result.put("message", "get table info failed");
                return result;
            }
            if (tables.size() > 1) {
                content += String.format("第%d张表名是%s.%s，包含如下字段：", i + 1, database, table);
            }
            for (int j = 0; j < columns.size(); ++j) {
                String columnName = columns.get(j).get("columnName");
                if (tables.size() == 1) {  // only a table
                    String columnType = columns.get(j).get("columnType");
                    String columnComment = "";
                    if (null != columns.get(j).get("columnName")) {
                        columnComment = columns.get(j).get("columnComment");
                    }
                    String column_info = String.format("%s %s \"%s\"",
                            columnName, columnType, columnComment);
                    content += column_info;
                    if (j < columns.size() - 1) {
                        content += ",";
                    }
                } else {
                    content += columnName;
                    if (j < columns.size() - 1) {
                        content += ",";
                    } else if (i < tables.size() - 1) {
                        content += "; ";
                    }
                }
            }
        }
        mapRequest.put("content", content);
        mapRequest.put("uuid", uuid);
        String body = JSONObject.toJSONString(mapRequest);
        String resInfo = CommonUtil.httpResult(aiChatUrl, false, body, null, uuid);
        Map<String, Object> result = JSON.parseObject(resInfo, Map.class);
        aiChatMapper.addChat(uuid, user, content, result.get("answer").toString());
        return result;
    }
}
