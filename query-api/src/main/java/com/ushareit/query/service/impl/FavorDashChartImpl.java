package com.ushareit.query.service.impl;

import com.ushareit.query.bean.Classification;
import com.ushareit.query.bean.FavorDashChart;
import com.ushareit.query.constant.BaseResponseCodeEnum;
import com.ushareit.query.exception.ServiceException;
import com.ushareit.query.mapper.ChartMapper;
import com.ushareit.query.mapper.DashboardMapper;
import com.ushareit.query.mapper.FavorDashChartMapper;
import com.ushareit.query.repositry.mapper.CrudMapper;
import com.ushareit.query.service.FavorDashChartService;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@Setter
public class FavorDashChartImpl extends AbstractBaseServiceImpl<FavorDashChart> implements FavorDashChartService {

    @Resource
    private FavorDashChartMapper favorDashChartMapper;

    @Resource
    private ChartMapper chartMapper;

    @Resource
    private DashboardMapper dashboardMapper;

    @Override
    public CrudMapper<FavorDashChart> getBaseMapper() { return favorDashChartMapper; }

    @Override
    public FavorDashChart exist(FavorDashChart favorDashChart, String name){
        String type = favorDashChart.getType();
        Integer favorId = favorDashChart.getFavorId();
        FavorDashChart fadc = favorDashChartMapper.getfadc(name, 0, type, favorId);
        return fadc;
    }

    @Override
    public String del(Integer id,String name, String type){
        Integer active = 0;
        LocalDateTime updateTime = LocalDateTime.now();
        boolean result = favorDashChartMapper.del(id,updateTime,active,name, type);
        return String.valueOf(result);
    }

    @Override
    public void updateStatus(Integer id){
        Integer active = 1;
        LocalDateTime updateTime = LocalDateTime.now();
        favorDashChartMapper.updateStatus(id,updateTime,active);
    }

    @Override
    public void updateChartFavor(Integer id){
        chartMapper.updateChartFavor(id,1);
    }

    @Override
    public void updateDashFavor(Integer id){
        dashboardMapper.updateDashFavor(id,1);
    }

    @Override
    public void delChartFavor(Integer id){
        chartMapper.delChartFavor(id,0);
    }

    @Override
    public void delDashFavor(Integer id){
        dashboardMapper.delDashFavor(id,0);
    }

}
