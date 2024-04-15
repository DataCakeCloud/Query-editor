package com.ushareit.query.web.controller;

import com.alibaba.fastjson.JSON;
import com.ushareit.query.bean.FavorDashChart;
import com.ushareit.query.constant.BaseResponseCodeEnum;
import com.ushareit.query.service.BaseService;
import com.ushareit.query.service.FavorDashChartService;
import com.ushareit.query.web.vo.BaseResponse;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.HashMap;

/**
 * @author: wangsy1
 * @create: 2022-11-28 15:22
 */
@Api(tags = "收藏相关")
@RestController
@RequestMapping("/favordach")
public class FavorDashChartController extends BaseBusinessController<FavorDashChart>{

    @Autowired
    private FavorDashChartService favorDashChartService;

    @Override
    public BaseService<FavorDashChart> getBaseService(){ return favorDashChartService; }

    @PostMapping("/addfavor")
    public BaseResponse addfavor(@RequestBody @Valid FavorDashChart favorDashChart){
        String name = getCurrentUser().getUserName();
        FavorDashChart exist = favorDashChartService.exist(favorDashChart,name);
        if (favorDashChart.getType().equals("chart")){
            favorDashChartService.updateChartFavor(favorDashChart.getFavorId());
        }
        if (favorDashChart.getType().equals("dashboard")){
            favorDashChartService.updateDashFavor(favorDashChart.getFavorId());
        }
        if (exist == null){
            return super.add(favorDashChart);
        }else {
            favorDashChartService.updateStatus(exist.getId());
            return BaseResponse.success();
        }
    }

    @PostMapping("/delete")
    public BaseResponse delete(@RequestBody @Valid String params){
        try {
            HashMap<String, String > map = JSON.parseObject(params, HashMap.class);
            String name = getCurrentUser().getUserName();
            String result = favorDashChartService.del(Integer.parseInt(map.get("id")),name,map.get("type"));
            if (map.get("type").equals("chart")){
                favorDashChartService.delChartFavor(Integer.parseInt(map.get("id")));
            }
            if (map.get("type").equals("dashboard")){
                favorDashChartService.delDashFavor(Integer.parseInt(map.get("id")));
            }
            if (result == "true"){
                return BaseResponse.success(result);
            }else {
                return BaseResponse.error("1",result);
            }
        }catch (Exception e){
            return BaseResponse.error(BaseResponseCodeEnum.DELETE_FAIL,e);
        }
    }
}
