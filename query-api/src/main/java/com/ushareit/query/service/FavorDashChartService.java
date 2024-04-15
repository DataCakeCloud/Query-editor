package com.ushareit.query.service;

import com.ushareit.query.bean.Classification;
import com.ushareit.query.bean.FavorDashChart;
import org.springframework.stereotype.Service;

/**
 * @author: wangsy1
 * @create: 2022-11-29 15:22
 */
public interface FavorDashChartService extends BaseService<FavorDashChart>{

    String del(Integer id,String name, String type);

    FavorDashChart exist(FavorDashChart favorDashChart, String name);

    void updateStatus(Integer id);

    void updateChartFavor(Integer id);

    void updateDashFavor(Integer id);

    void delChartFavor(Integer id);

    void delDashFavor(Integer id);
}
