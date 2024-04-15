package com.ushareit.query.web.controller;

import com.alibaba.fastjson.JSONObject;
import com.ushareit.query.service.MetaService;
import com.ushareit.query.constant.BaseResponseCodeEnum;
import com.ushareit.query.service.BaseService;
import com.ushareit.query.service.MetaServiceGateway;
import com.ushareit.query.web.vo.BaseResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.text.ParseException;
import java.util.*;

/**
 * @author: huyx
 * @create: 2022-01-19 15:24
 */
@Api(tags = "获取元数据")
@RestController
//@ConfigurationProperties(prefix = "engine")
@Setter
@RequestMapping("/metadata")
public class MetaController extends BaseBusinessController {
    @Autowired
    private MetaService metaService;

    @Autowired
    private MetaServiceGateway metaServiceGateway;

    @Override
    public BaseService getBaseService() {
        return metaService;
    }

    @Resource
    private CacheManager cacheManager;

    @ApiOperation("查询catalog列表")
    @GetMapping(value = "/catalog")
//    @Cacheable(cacheNames = {"metadata"}, key = "#engine")
    public BaseResponse<?> getMetaCatalog(@RequestParam("engine") String engine,
    		@RequestParam(defaultValue = "") String region) {
        String name = getCurrentUser().getUserName();
//        String group = metaService.getUserGroup(name);
//        if (group.equals("")) {
//            return BaseResponse.error("500", "无数据源权限，请前往OA审批'DataStudio数据分析权限申请'");
//        }
        List<String> result = metaService.getMetaCatalog(engine, name, region);
        return BaseResponse.success(result);
    }

    @ApiOperation("查询database列表")
    @GetMapping(value = "/database")
//    @Cacheable(cacheNames = {"metadata"}, key = "#engine+'-'+#catalog+'-'+#group")
    public BaseResponse<?> getMetaDatabase(@RequestHeader("Authentication") String token,
                                           @RequestParam("engine") String engine,
                                           @RequestParam("catalog") String catalog,
                                   		   @RequestParam(defaultValue = "") String region) throws ParseException  {
        String name = getCurrentUser().getUserName();
        if (null != region && region.trim().length() > 0) {
        	if (catalog.equalsIgnoreCase("iceberg")) {
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
        }
//        String group = metaService.getUserGroup(name);
//        if (group.equals("")) {
//            return BaseResponse.error("500", "无数据库权限，请前往OA审批'DataStudio数据分析权限申请'");
//        }
        String tenantName = getCurrentUser().getTenantName();
        try {
            List<String> result = metaServiceGateway.getMetaDatabase(engine, name, catalog, tenantName, region, getCurrentUser(), token);
            return BaseResponse.success(result);
        } catch (Exception e) {
            return BaseResponse.error("500", e.getMessage());
        }
    }

    @ApiOperation("查询table列表")
    @GetMapping(value = "/table")
//    @Cacheable(cacheNames = {"metadata"}, key = "#engine+'-'+#catalog+'-'+#database+'-'+#group")
    public BaseResponse<?> getMetaTable(@RequestHeader("Authentication") String token,
                                        @RequestParam("engine") String engine,
                                        @RequestParam("catalog") String catalog,
                                        @RequestParam("database") String database,
                                		@RequestParam(defaultValue = "") String region) throws ParseException {
        String name = getCurrentUser().getUserName();
        if (null != region && region.trim().length() > 0) {
        	if (catalog.equalsIgnoreCase("iceberg")) {
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
        }
//        String group = metaService.getUserGroup(name);
//        if (group.equals("")) {
//            return BaseResponse.error("500", "无数据表权限，请前往OA审批'DataStudio数据分析权限申请'");
//        }
        String tenantName = getCurrentUser().getTenantName();
        try {
            List<String> result = metaServiceGateway.getMetaTable(engine, name, catalog, database, tenantName, region, getCurrentUser(), token);
            return BaseResponse.success(result);
        } catch (Exception e) {
            return BaseResponse.error("500", e.getMessage());
        }
    }

    @ApiOperation("查询column列表")
    @GetMapping(value = "/column")
//    @Cacheable(cacheNames = {"metadata"}, key = "#engine+'-'+#catalog+'-'+#database+'-'+#table+'-'+#group")
    public BaseResponse<?> getMetaColumn(@RequestHeader("Authentication") String token,
                                        @RequestParam("engine") String engine,
                                        @RequestParam("catalog") String catalog,
                                        @RequestParam("database") String database,
                                        @RequestParam("table") String table,
                                		@RequestParam(defaultValue = "") String region) throws ParseException {
        String name = getCurrentUser().getUserName();
        if (null != region && region.trim().length() > 0) {
        	if (catalog.equalsIgnoreCase("iceberg")) {
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
        }
//        String group = metaService.getUserGroup(name);
//        if (group.equals("")) {
//            return BaseResponse.error("500", "无当前表的查看权限，请前往OA审批'DataStudio数据分析权限申请'");
//        }
        String tenantName = getCurrentUser().getTenantName();
        try {
            return BaseResponse.success(metaServiceGateway.getMetaColumn(engine, name, catalog, database,
                    table, tenantName, region, getCurrentUser(), token));
        } catch (Exception e) {
            return BaseResponse.error("500", e.getMessage());
        }
    }

    @ApiOperation("清理缓存")
    @GetMapping(value = "/clearCache")
    public BaseResponse<?> clearCache(@RequestParam(defaultValue = "") String engine,
                                      @RequestParam(defaultValue = "") String catalog,
                                      @RequestParam(defaultValue = "") String database,
                                      @RequestParam(defaultValue = "") String table,
                                      @RequestParam(defaultValue = "") String region,
                                      @RequestParam(defaultValue = "0") Integer forward) {
        System.out.println("Clean cache");

        String name = getCurrentUser().getUserName();
        String tenantName = getCurrentUser().getTenantName();
//        String group = metaService.getUserGroup(name);

        /*if (0 == forward) {
            List<String> nodes = metaService.getClusterNodeFromK8s();
            if (null != nodes) {
                String user_info = getUserInfo();
                for (int i = 0; i < nodes.size(); ++i) {
                    metaService.forwardRequest(nodes.get(i),
                            "/metadata/clearCache?forward=1",
                            user_info);
                }
            }
            return BaseResponse.success("缓存清理完毕");
        }*/

        if (null != region && region.trim().length() > 0) {
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
        }

        CaffeineCache caffeineCache = (CaffeineCache) cacheManager.getCache("metadata");
        com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();
        if (!table.isEmpty()) {
            String cache_name = tenantName+'-'+engine+'-'+catalog+'-'+database+'-'+table;
            nativeCache.invalidate(cache_name + "-columns");
            nativeCache.invalidate(cache_name + "-tableOwner");
            return BaseResponse.success("缓存清理完毕");
        }

        Set<Object> cacheMetadataKey = nativeCache.asMap().keySet();
        for (Object metadataKey: cacheMetadataKey) {
            String metadataKeyString = metadataKey.toString();
            if (metadataKeyString.indexOf(tenantName) != 0) {
            	continue;
            }
            if (metadataKeyString.contains(name)) {
                nativeCache.invalidate(metadataKey);
            }
            if (metadataKeyString.contains("columns")) {
                nativeCache.invalidate(metadataKey);
            }
            if (metadataKeyString.contains("tables")) {
                nativeCache.invalidate(metadataKey);
            }
            if (metadataKeyString.contains("tableOwner")) {
                nativeCache.invalidate(metadataKey);
            }
        }

        return BaseResponse.success("缓存清理完毕");
    }

    @ApiOperation("获取数据源和引擎列表")
    @GetMapping(value = "/catalogAndEngine")
//    @Cacheable(cacheNames = {"metadata"}, key = "#engine")
    public BaseResponse<?> getMetaCatalogEngines(@RequestHeader("Authentication") String token,
                                                 @RequestParam(defaultValue = "") String region) {
    	if (null == region || 0 == region.trim().length()) {
    		return BaseResponse.error(BaseResponseCodeEnum.CLI_PARAM_ILLEGAL);
    	}
        String name = getCurrentUser().getUserName();
        String tenantName = getCurrentUser().getTenantName();
        String groupUuid = getCurrentUser().getGroupUuid();
        boolean isAdmin = getCurrentUser().isAdmin();
//        String group = metaService.getUserGroup(name);
//        if (group.equals("")) {
//            return BaseResponse.error("500", "无数据源权限，请前往OA审批'DataStudio数据分析权限申请'");
//        }
        ArrayList<Object> result = metaService.getMetaCatalogAndEngineFromDS(
                name, region, tenantName, token, groupUuid, isAdmin);
        return BaseResponse.success(result);
    }


    @ApiOperation("查询column列表")
    @GetMapping(value = "/tableOwner")
//    @Cacheable(cacheNames = {"metadata"}, key = "#engine+'-'+#catalog+'-'+#database+'-'+#table+'-'+#group")
    public BaseResponse<?> getTableOwner(@RequestParam(defaultValue = "") String engine,
                                         @RequestParam("catalog") String catalog,
                                         @RequestParam("database") String database,
                                         @RequestParam("table") String table,
                                         @RequestParam(defaultValue = "") String region) throws ParseException {
        String name = getCurrentUser().getUserName();
        if (null != region && region.trim().length() > 0) {
            if (catalog.equalsIgnoreCase("iceberg")) {
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
        }
        String tenantName = getCurrentUser().getTenantName();
        return BaseResponse.success(metaServiceGateway.getTableOwner(engine, name, catalog, database,
                table, tenantName, region, getCurrentUser()));
    }
}
