package com.ushareit.query.web.controller;

import com.alibaba.fastjson.JSON;
import com.github.pagehelper.PageInfo;
import com.ushareit.query.bean.Chart;
import com.ushareit.query.bean.CurrentUser;
import com.ushareit.query.bean.Sharebi;
import com.ushareit.query.constant.BaseResponseCodeEnum;
import com.ushareit.query.service.BaseService;
import com.ushareit.query.service.ChartService;
import com.ushareit.query.service.UserService;
import com.ushareit.query.web.vo.BaseResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.Set;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: wangsy1
 * @create: 2022-11-25 15:22
 */
@Api(tags = "chart相关")
@RestController
@Setter
@RequestMapping("/chart")
public class ChartController extends BaseBusinessController<Chart> {

    @Autowired
    private ChartService chartService;

    @Resource
    private CacheManager cacheManager;

    @Override
    public BaseService<Chart> getBaseService(){ return chartService; }

    @PostMapping("/addchart")
    public BaseResponse addchart(@RequestBody @Valid String params,
                                 @RequestHeader("Authentication")String token, HttpServletRequest request){
        try {
            HashMap<String,String> map = JSON.parseObject(params, HashMap.class);
            String name = getCurrentUser().getUserName();
            String tenantName = getCurrentUser().getTenantName();
            String checkName = chartService.selectByUsername(map.get("title"),name,1);
            if (checkName.equals("名字重复")){
                return BaseResponse.error("1",checkName);
            }
//            String result = new String();
            Integer id = null;
            if (name.contains(".")){
                String shareid = name.replace(".","_");
                id = chartService.addChart(shareid,map,token,name);
                chartService.addDE(shareid,map,token,name,tenantName);
            }else {
                id = chartService.addChart(name,map,token,name);
                chartService.addDE(name,map,token,name,tenantName);
            }
            Map data = new HashMap();
            data.put("id",id);
            return BaseResponse.success(data);
        }catch (Exception e){
            return BaseResponse.error(BaseResponseCodeEnum.valueOf(e.toString()));
        }
    }

    @PostMapping("/update")
    public BaseResponse update(@RequestBody @Valid String params,@RequestHeader("Authentication")String token){
        try {
            HashMap<String,String> map = JSON.parseObject(params, HashMap.class);
            String name = getCurrentUser().getUserName();
            String tenantName = getCurrentUser().getTenantName();
            String checkName = chartService.selectByUsernameUpdate(map.get("title"),name,1,Integer.parseInt(map.get("id")));
            if (checkName.equals("名字重复")){
                return BaseResponse.error("1",checkName);
            }
            String result = new String();
            if (name.contains(".")){
                String shareid = name.replace(".","_");
                result = chartService.updateChart(shareid,map,token,name,tenantName);
            }else {
                result = chartService.updateChart(name,map,token,name,tenantName);
            }
            if (result.contains("失败")){
                return BaseResponse.error(BaseResponseCodeEnum.valueOf(result));
            }else {
                return BaseResponse.success(result);
            }
        }catch (Exception e){
            return BaseResponse.error(BaseResponseCodeEnum.APP_UPDATE_SUCCESS,e);
        }
    }

    @PostMapping("/delete")
    public BaseResponse delete(@RequestBody @Valid String params,@RequestHeader("Authentication")String token){
        try {
            HashMap<String, String > map = JSON.parseObject(params, HashMap.class);
            String name = getCurrentUser().getUserName();
            String result = chartService.deleteChart(name,Integer.parseInt(map.get("id")),token);
            if (result.equals("删除失败：该图表已被仪表盘引用")){
                return BaseResponse.error("500",result);
            }else {
                return BaseResponse.success(result);
            }
        }catch (Exception e){
            return BaseResponse.error(BaseResponseCodeEnum.DELETE_FAIL,e);
        }
    }

    @PostMapping("/addShare")
    public BaseResponse addShare(@RequestBody @Valid String params){
        Sharebi sh = new Sharebi();
        HashMap<String, String > map = JSON.parseObject(params, HashMap.class);
        String sharer = map.get("sharer").toString();
        if (sharer.equalsIgnoreCase("admin")) {
            sharer = getCurrentUser().getUserName();
        }
        sh.setSharer(sharer);
        sh.setSharee(map.get("sharee"));
        sh.setType("chart");
        sh.setGrade(1);
        sh.setShareUrl(map.get("shareUrl"));
        sh.setCreateTime(new Timestamp(System.currentTimeMillis()));
        sh.setShareId(Integer.parseInt(map.get("shareid")));
        int id = chartService.addShareGrade(sh, map.get("shareeEmail"));
        if (-1 == id) {
            return BaseResponse.error(BaseResponseCodeEnum.SYS_ERR);
        }
        HashMap<String, Integer> ret_info = new HashMap<>();
        ret_info.put("BIID", id);
        return BaseResponse.success(ret_info);
    }

    @GetMapping("/view")
    public BaseResponse view(@RequestParam Integer id){
        try {
            String name = getCurrentUser().getUserName();
            chartService.addViewLog(name, id);
            List<Chart> getChart = chartService.getChart(id);
            return BaseResponse.success(getChart);
        }catch (Exception e){
            return BaseResponse.error(BaseResponseCodeEnum.valueOf(e.toString()));
        }
    }

    @GetMapping("/chartlist")
    public BaseResponse chartlist(@RequestParam String type,
                             @RequestParam String title,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "50") Integer pageSize,
                            @RequestHeader("Authentication")String token) throws IOException {
        String name = getCurrentUser().getUserName();
        String tenantName = getCurrentUser().getTenantName();
//        String name ="ext.huangkai";
        PageInfo<List> data = chartService.getList(type, tenantName, name, pageNum, pageSize, title, token);
        chartService.updateChartStatus(type, name, title, token);
        return BaseResponse.success(data);
    }

    @GetMapping("/jinja")
    public BaseResponse jinja(@RequestParam List<String> jinja){
        String name = getCurrentUser().getUserName();
        List<Map<String,String>> getJinja = chartService.getJinja(name, jinja);
        return BaseResponse.success(getJinja.get(0));
    }

    @GetMapping("/databack")
    public BaseResponse databack(@RequestParam Map<String, Object> params){
        String name = getCurrentUser().getUserName();
        List result = new ArrayList();
        try {
            String bidt = (String) params.get("bidt");
            Integer cid = Integer.parseInt((String) params.get("id"));
            result = chartService.getDataback(name, getCurrentUser().getTenantName(), params, bidt, cid);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return BaseResponse.success(result);
    }

    @GetMapping("/test")
    public BaseResponse test(@RequestParam Integer id) throws IOException {
        chartService.test(id);
        return BaseResponse.success();
    }

    @GetMapping("/online")
    public  BaseResponse online(@RequestParam Integer id)throws IOException{
        String name = getCurrentUser().getUserName();
        chartService.online(name, id);
        return  BaseResponse.success();
    }

    @ApiOperation("清理缓存")
    @GetMapping(value = "/clearCache")
    public BaseResponse<?> clearCache() {
        System.out.println("Clean cache");

        String name = getCurrentUser().getUserName();
//        String name ="ext.huangkai";
//        String group = metaService.getUserGroup(name);

        CaffeineCache caffeineCache = (CaffeineCache) cacheManager.getCache("chart");
        com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();
        Set<Object> cacheMetadataKey = nativeCache.asMap().keySet();
        for (Object metadataKey: cacheMetadataKey) {
            String metadataKeyString = metadataKey.toString();
            if (metadataKeyString.contains(name)) {
                nativeCache.invalidate(metadataKey);
            }
        }

        return BaseResponse.success("缓存清理完毕");
    }
}
