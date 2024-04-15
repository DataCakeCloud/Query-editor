package com.ushareit.query.service.impl;

import com.ushareit.query.bean.DashChart;
import com.ushareit.query.mapper.DashChartMapper;
import com.ushareit.query.repositry.mapper.CrudMapper;
import com.ushareit.query.service.DashChartService;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author: wangsy1
 * @create: 2022-11-25 15:22
 */
@Slf4j
@Service
@Setter
public class DashChartImpl extends AbstractBaseServiceImpl<DashChart> implements DashChartService {

    @Resource
    private DashChartMapper dashChartMapper;

    @Override
    public CrudMapper<DashChart> getBaseMapper() { return dashChartMapper; }
}
