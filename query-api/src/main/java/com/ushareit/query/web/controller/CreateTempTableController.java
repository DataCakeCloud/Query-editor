package com.ushareit.query.web.controller;

import com.alibaba.fastjson.JSON;
import com.ushareit.query.bean.QueryHistory;
import com.ushareit.query.constant.BaseResponseCodeEnum;
import com.ushareit.query.mapper.QueryHistoryMapper;
import com.ushareit.query.service.*;
import com.ushareit.query.web.vo.BaseResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.mock.web.MockMultipartFile;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.text.ParseException;
import java.util.*;

/**
 * @author: huyx
 * @create: 2022-05-16 15:24
 */
@Api(tags = "创建临时表")
@RestController
@RequestMapping("/tempTable")
public class CreateTempTableController extends BaseBusinessController {

    @Autowired
    private CreateTempTableService createTempTableService;

    @Autowired
    private TempTableGWService tempTableGWService;

    @Autowired
    private MetaService metaService;

    @Resource
    private CacheManager cacheManager;

    @Resource
    private QueryHistoryMapper queryHistoryMapper;

    @Autowired
    private TaskService taskervice;

    @Autowired
    private GatewayService gatewayService;

    @Override
    public BaseService getBaseService() {
        return createTempTableService;
    }

    @ApiOperation("表名重复校验")
    @GetMapping(value = "/nameCheck")
    public BaseResponse<?> nameCheck(@RequestParam("engine") String engine,
                                     @RequestParam("database") String database,
                                     @RequestParam("tableName") String tableName,
                                     @RequestParam(defaultValue = "") String region,
                                     @RequestParam(defaultValue = "") String catalog) {
        String name = getCurrentUser().getUserName();
//        String group = metaService.getUserGroup(name);
        if (null != region && region.trim().length() > 0) {
        	if (catalog.equalsIgnoreCase("iceberg")) {
        		if (region.equalsIgnoreCase("aws_ue1")) {
        			engine = "presto_aws";
        		} else if (region.equalsIgnoreCase("aws_sg")) {
        			engine = "presto_aws_sg";
        		} else if (region.equalsIgnoreCase("huawei_sg")) {
        			engine = "presto_huawei";
        		} else {
                    engine = "presto_" + region;
                }
        	} else if (catalog.equalsIgnoreCase("hive")) {
                engine = "hive_" + region;
            } else {
        		engine = catalog;
        	}
        }
        String tenantName = getCurrentUser().getTenantName();
        List<String> tableList = metaService.getMetaTable(engine, name, "hive", database, tenantName, region);
        String isExisted = createTempTableService.nameCheck(tableList, tableName);
        return BaseResponse.success(isExisted);
    }

    @ApiOperation("第一次上传文件至cloud")
    @PostMapping(value="/upload/new")
    public BaseResponse<?> uploadNew(@RequestParam @Valid Map<String, Object> params, MultipartFile file) {
        String user = getCurrentUser().getUserName();
    	int tenantId = getCurrentUser().getTenantId();
        String region = (String)params.get("region");
        String catalog = (String)params.get("catalog");
        if (null != region && region.trim().length() > 0) {
        	if (catalog.equalsIgnoreCase("iceberg")) {
                if (region.equalsIgnoreCase("aws_ue1")) {
                    params.put("engine_key", "presto_aws");
                } else if (region.equalsIgnoreCase("aws_sg")) {
                    params.put("engine_key", "presto_aws_sg");
                } else if (region.equalsIgnoreCase("huawei_sg")) {
                    params.put("engine_key", "presto_huawei");
                } else {
                    params.put("engine_key", "presto_" + region);
                }
            } else if (catalog.equalsIgnoreCase("hive")) {
                params.put("engine_key", "hive_" + region);
        	} else {
    			params.put("engine_key", catalog);
        	}
        }
        HashMap<String, Object> response = createTempTableService.uploadNew(params,
        		user, file, getUserInfo(), tenantId);
        if (response.get("code").equals(0)) {
            return BaseResponse.success(response.get("data"));
        } else {
            return BaseResponse.error(response.get("code").toString(), response.get("message").toString());
        }
    }

    @ApiOperation("第二次上传文件至cloud")
    @PostMapping(value="/upload/overwrite")
    public BaseResponse<?> uploadRepeat(@RequestParam @Valid Map<String, Object> params, MultipartFile file) throws ParseException {
        String user = getCurrentUser().getUserName();
    	int tenantId = getCurrentUser().getTenantId();
        String tenantName = getCurrentUser().getTenantName();
        String region = (String)params.get("region");
        String catalog = (String)params.get("catalog");
        if (null != region && region.trim().length() > 0) {
        	if (catalog.equalsIgnoreCase("iceberg")) {
        		if (region.equalsIgnoreCase("aws_ue1")) {
        			params.put("engine_key", "presto_aws");
        		} else if (region.equalsIgnoreCase("aws_sg")) {
        			params.put("engine_key", "presto_aws_sg");
        		} else if (region.equalsIgnoreCase("huawei_sg")) {
        			params.put("engine_key", "presto_huawei");
        		} else {
                    params.put("engine_key", "presto_" + region);
        		}
        	} else if (catalog.equalsIgnoreCase("hive")) {
                params.put("engine_key", "hive_" + region);
            }else {
    			params.put("engine_key", catalog);
        	}
        }
        //HashMap<String, Object> response = createTempTableService.uploadRepeat(params, user,
        //        file, getUserInfo(), tenantId, tenantName);
        HashMap<String, Object> response = tempTableGWService.uploadRepeat(params,
        		file, getUserInfo(), getCurrentUser());
        if (response.get("code").equals(0)) {
            return BaseResponse.success(response.get("data"));
        } else {
            return BaseResponse.error(response.get("code").toString(), response.get("message").toString());
        }
    }

    @ApiOperation("创建临时表")
    @PostMapping(value = "/create")
    public BaseResponse<?> createTempTable(@RequestBody @Valid HashMap<String, Object> params) throws ParseException {
        String username = getCurrentUser().getUserName();
        String tenantName = getCurrentUser().getTenantName();
        String engine = (String)params.get("engine");
        String database = (String)params.get("database");
        String tableName = (String)params.get("tableName");
        String comment = (String)params.get("comment");
        String location =(String)params.get("location");
        String region = (String)params.get("region");
        String catalog = (String)params.get("catalog");
        List<Object> meta = (List<Object>)params.get("meta");
        if (null != region && region.trim().length() > 0) {
        	if (catalog.equalsIgnoreCase("iceberg")) {
        		if (region.equalsIgnoreCase("aws_ue1")) {
        			engine = "presto_aws";
        		} else if (region.equalsIgnoreCase("aws_sg")) {
        			engine = "presto_aws_sg";
        		} else if (region.equalsIgnoreCase("huawei_sg")) {
        			engine = "presto_huawei";
        		} else {
                    engine = "presto_" + region;
        		}
        	} else if (catalog.equalsIgnoreCase("hive")) {
                engine = "hive_" + region;
            } else {
        		engine = catalog;
        	}
        }

        //HashMap<String, Object> response = createTempTableService.execute(username, engine, database,
        //       tableName, comment, location, meta, tenantName, region);
        HashMap<String, Object> response = tempTableGWService.execute(database,
        		tableName, comment, location, meta, engine, region, getCurrentUser());
        if (response.get("code").equals(0)){
            System.out.println("Start clean temp database cache");

            CaffeineCache caffeineCache = (CaffeineCache) cacheManager.getCache("metadata");
            com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();
            Set<Object> cacheMetadataKey = nativeCache.asMap().keySet();
            for (Object metadataKey: cacheMetadataKey) {
                String metadataKeyString = metadataKey.toString();
                if (metadataKeyString.contains(database)) {
                    nativeCache.invalidate(metadataKey);
                }
            }
            System.out.println("Clean temp database cache successfully");

            return BaseResponse.success(response.get("data"));
        } else {
            return BaseResponse.error(response.get("code").toString(), response.get("message").toString());
        }
    }

    @ApiOperation("创建临时表")
    @PostMapping(value = "/createFromResult")
    public BaseResponse<?> createFromResult(@RequestBody @Valid String str_params) throws Exception {
        String username = getCurrentUser().getUserName();
        int tenantId = getCurrentUser().getTenantId();
        String tenantName = getCurrentUser().getTenantName();
        HashMap<String, Object > params = JSON.parseObject(str_params, HashMap.class);
        String engine = (String)params.get("engine");
        String database = (String)params.get("database");
        String tableName = (String)params.get("tableName");
        String comment = (String)params.get("comment");
        String uuid =(String)params.get("uuid");
        String region = (String)params.get("region");
        String catalog = (String)params.get("catalog");
        if (null != region && region.trim().length() > 0) {
            if (catalog.equalsIgnoreCase("iceberg")) {
                if (region.equalsIgnoreCase("aws_ue1")) {
                    engine = "presto_aws";
                } else if (region.equalsIgnoreCase("aws_sg")) {
                    engine = "presto_aws_sg";
                } else if (region.equalsIgnoreCase("huawei_sg")) {
                    engine = "presto_huawei";
        	} else {
                    engine = "presto_" + region;
                }
            } else if (catalog.equalsIgnoreCase("hive")) {
                engine = "hive_" + region;
            } else {
                engine = catalog;
            }
        }

        params.put("engine_key", engine);
        QueryHistory queryHistory = queryHistoryMapper.selectByUuid(uuid);
        if (queryHistory == null || 0 == queryHistory.getId()) {
            return BaseResponse.error(BaseResponseCodeEnum.CLI_PARAM_ILLEGAL);
        }

        List<Object> meta = new ArrayList<Object>();
        String column_type = queryHistory.getColumnType();
        List listColumns = JSON.parseObject(column_type, List.class);
        for (int i = 0; i < listColumns.size(); ++i) {
            Map column = JSON.parseObject(listColumns.get(i).toString(), Map.class);
            Iterator<Map.Entry<String, String>> it = column.entrySet().iterator();
            if (it.hasNext()) {
                Map<String, String> mapColumn = new HashMap<String, String>();
                Map.Entry<String, String> entry = it.next();
                String name = entry.getKey();
                int lidx = name.indexOf('.');
                if (lidx != -1) {
                    name = name.substring(lidx + 1);
                }
                mapColumn.put("name", name);
                mapColumn.put("type", entry.getValue());
                meta.add(JSON.toJSONString(mapColumn));
            }
        }

        String csvFile;
        String engineTemp = (String)params.get("engine");
        if (null == engineTemp || !engineTemp.isEmpty()) {
            engineTemp = engine;
        }
        //if (engineTemp.startsWith("presto")) {
        //    csvFile = taskervice.downloadToNative(uuid, username);
        //} else {
            csvFile = gatewayService.downloadToNative(uuid, username, getUserInfo(), queryHistory, "csv");
        //}
        Path path = Paths.get(csvFile);
        String f_name = csvFile.substring(csvFile.lastIndexOf('/') + 1);
        String originalFileName = f_name;
        String contentType = "text/csv";
        byte[] content = Files.readAllBytes(path);
        MultipartFile m_file = new MockMultipartFile(f_name,
                originalFileName, contentType, content);
        HashMap<String, Object> up_response = createTempTableService.uploadNew(params,
                username, m_file, getUserInfo(), tenantId);
        if (!up_response.get("code").equals(0)) {
            return BaseResponse.error(up_response.get("code").toString(),
                    up_response.get("message").toString());
        }

        String location = ((HashMap<String, Object>)up_response.get("data")).get("location").toString();
        //HashMap<String, Object> response = createTempTableService.execute(username, engine, database,
        //        tableName, comment, location, meta, tenantName, region);
        HashMap<String, Object> response = tempTableGWService.execute(database,
                tableName, comment, location, meta, engine, region, getCurrentUser());
        if (response.get("code").equals(0)){
            System.out.println("Start clean temp database cache");

            CaffeineCache caffeineCache = (CaffeineCache) cacheManager.getCache("metadata");
            com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();
            Set<Object> cacheMetadataKey = nativeCache.asMap().keySet();
            for (Object metadataKey: cacheMetadataKey) {
                String metadataKeyString = metadataKey.toString();
                if (metadataKeyString.contains(database)) {
                    nativeCache.invalidate(metadataKey);
                }
            }
            System.out.println("Clean temp database cache successfully");

            return BaseResponse.success(response.get("data"));
        } else {
            return BaseResponse.error(response.get("code").toString(), response.get("message").toString());
        }
    }
}
