package com.ushareit.query.web.controller;

import com.alibaba.fastjson.JSON;
import com.github.pagehelper.PageInfo;
import com.ushareit.query.bean.Chart;
import com.ushareit.query.bean.Dashboard;
import com.ushareit.query.bean.Sharebi;
import com.ushareit.query.constant.BaseResponseCodeEnum;
import com.ushareit.query.service.BaseService;
import com.ushareit.query.service.DashboardService;
import com.ushareit.query.web.vo.BaseResponse;
import com.ushareit.query.web.utils.CommonUtil;
import io.swagger.annotations.Api;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import javax.validation.Valid;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: wangsy1
 * @create: 2022-12-08 15:22
 */
@Slf4j
@Api(tags = "dashboard相关")
@RestController
@EnableAsync
@RequestMapping("/dashboard")
public class DashboardController extends BaseBusinessController<Dashboard>{

    @Autowired
    private DashboardService dashboardService;

    @Override
    public BaseService<Dashboard> getBaseService(){ return dashboardService; }

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
        sh.setType("dashboard");
        sh.setGrade(1);
        sh.setShareUrl(map.get("shareUrl"));
        sh.setCreateTime(new Timestamp(System.currentTimeMillis()));
        sh.setShareId(Integer.parseInt(map.get("shareid")));
        int id = dashboardService.addShareGrade(sh, map.get("shareeEmail"));
        if (-1 == id) {
            return BaseResponse.error(BaseResponseCodeEnum.SYS_ERR);
        }
        HashMap<String, Integer> ret_info = new HashMap<>();
        ret_info.put("BIID", id);
        return BaseResponse.success(ret_info);
    }

    @PostMapping("/update")
    public BaseResponse update(@RequestBody @Valid String params,@RequestHeader("Authentication")String token){
        HashMap<String, String > map = JSON.parseObject(params, HashMap.class);
        String name = getCurrentUser().getUserName();
        String tenantName = getCurrentUser().getTenantName();
        String result = new String();
        if (name.contains(".")){
            String shareid = name.replace(".","_");
            result = dashboardService.updateDash(map,shareid,token,name,tenantName);
            dashboardService.updateDashDE(map, shareid, token, name, tenantName);
        }else{
            result = dashboardService.updateDash(map,name,token,name,tenantName);
            dashboardService.updateDashDE(map, name, token, name, tenantName);
        }
        return BaseResponse.success();
    }

    @PostMapping("/delete")
    public BaseResponse delete(@RequestBody @Valid String params,@RequestHeader("Authentication")String token){
        HashMap<String, String > map = JSON.parseObject(params, HashMap.class);
        String name = getCurrentUser().getUserName();
        String tenantName = getCurrentUser().getTenantName();
        String result = new String();
        if (name.contains(".")){
            String shareid = name.replace(".","_");
            dashboardService.delDash(Integer.parseInt(map.get("id")),shareid,token,name,tenantName);
        }else {
            dashboardService.delDash(Integer.parseInt(map.get("id")),name,token,name,tenantName);
        }
        return BaseResponse.success();
    }

    @PostMapping("/adddash")
    public BaseResponse adddash(@RequestBody @Valid String params,@RequestHeader("Authentication")String token){
        HashMap<String, String > map = JSON.parseObject(params, HashMap.class);
        String name = getCurrentUser().getUserName();
        String tenantName = getCurrentUser().getTenantName();
        Dashboard dashboard = new Dashboard();
        dashboard.setIsShare(0);
        dashboard.setIsActive(1);
        dashboard.setName(map.get("name"));
        dashboard.setDescribeDash(map.get("describe"));
        dashboard.setCrontab(map.get("crontab"));
        dashboard.setIsFavorate(0);
        dashboard.setClassId(Integer.parseInt(map.get("classid")));
        dashboard.setIsSchedule(Integer.parseInt(map.get("is_schedule")));
        dashboard.setParam(map.get("params"));
        dashboardService.preCheckCommon(dashboard, name);
        super.add(dashboard);
        Integer id = dashboard.getId();
        Integer sche = Integer.parseInt(map.get("is_schedule"));
        if (name.contains(".")){
            String shareid = name.replace(".","_");
            dashboardService.addDash(map,shareid,id,token,name,tenantName);
            if (sche.equals(1)){
                dashboardService.addDashDE(map, shareid, id, token, name, tenantName);
            }
        }else {
            dashboardService.addDash(map,name,id,token,name,tenantName);
            if (sche.equals(1)){
                dashboardService.addDashDE(map, name, id, token, name, tenantName);
            }
        }
        Map data = new HashMap<>();
        data.put("id",id);
        data.put("classId",Integer.parseInt(map.get("classid")));
        return BaseResponse.success(data);
    }

    @GetMapping("/view")
    public BaseResponse view(@RequestParam Integer id,@RequestHeader("Authentication")String token){
        String name = getCurrentUser().getUserName();
        try {
            List<Integer> auth = dashboardService.getAuth(name);
            if (!auth.contains(id)){
                return BaseResponse.error("500","您没有该看板权限");
            }
            dashboardService.addDashLog(name, id);
            List<Dashboard> getDash = dashboardService.getDash(id);
            if (getDash.size()==0){
                return BaseResponse.error("500","该看板已删除，请刷新页面");
            }
            List<Chart> getChart = dashboardService.getChart(id,token,name);
            Map dashchart = new HashMap();
            dashchart.put("classId",getDash.get(0).getClassId());
            dashchart.put("createBy",getDash.get(0).getCreateBy());
            dashchart.put("createTime",getDash.get(0).getCreateTime());
            dashchart.put("describeDash",getDash.get(0).getDescribeDash());
            dashchart.put("id",getDash.get(0).getId());
            dashchart.put("isActive",getDash.get(0).getIsActive());
            dashchart.put("isSchedule",getDash.get(0).getIsSchedule());
            dashchart.put("isShare",getDash.get(0).getIsShare());
            dashchart.put("name",getDash.get(0).getName());
            dashchart.put("updateBy",getDash.get(0).getUpdateBy());
            dashchart.put("updateTime",getDash.get(0).getUpdateTime());
            dashchart.put("params",getDash.get(0).getParam());
            dashchart.put("crontab",getDash.get(0).getCrontab());
            dashchart.put("chartlist",getChart);
            List data = new ArrayList();
            data.add(dashchart);
            dashboardService.updateChartStatus(id, token, name, getCurrentUser().getTenantName());
            return BaseResponse.success(data);
        }catch (Exception e){
            log.error(String.format("There is a stack err when %s view dashboard[id=%d]: %s",
                name, id, CommonUtil.printStackTraceToString(e)));
            return BaseResponse.error("500",e.getMessage());
        }
    }

    @GetMapping("/sumlist")
    public BaseResponse sumlist(){
        String name = getCurrentUser().getUserName();
        try {
//            String name = "ext.huangkai";
            dashboardService.addFirstLevel(name);
            List tree = dashboardService.getTree(name);
            return BaseResponse.success(tree);
        }catch (Exception e){
            log.error(String.format("There is a stack err when %s sublist dashboard: %s",
                name, CommonUtil.printStackTraceToString(e)));
            return BaseResponse.error("1",e.toString());
        }
    }

    @GetMapping("/listdash")
    public BaseResponse listdash(@RequestParam String type,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "50") Integer pageSize){
        String name = getCurrentUser().getUserName();
        PageInfo<List> data = dashboardService.getList(type,name,pageNum,pageSize);
        return BaseResponse.success(data);
    }
}
