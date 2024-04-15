package com.ushareit.query.service;

import com.github.pagehelper.PageInfo;
import com.ushareit.query.bean.Chart;
import com.ushareit.query.bean.Dashboard;
import com.ushareit.query.bean.Sharebi;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import org.springframework.scheduling.annotation.Async;

/**
 * @author: wangsy1
 * @create: 2022-11-25 15:22
 */
public interface DashboardService extends BaseService<Dashboard>{

    int addShareGrade(Sharebi sh, String shareeEmail);

    String updateDash(HashMap<String, String > map, String shareid,String token,String name,String tenantName);

    @Async
    public void updateDashDE(HashMap<String, String > map, String shareid,String token,String name,String tenantName);

    void delDash (Integer id,String shareid, String token,String name,String tenantName);

    void addDash(HashMap<String, String > map, String shareid, Integer id, String token, String name, String tenantName);

    @Async
    void addDashDE(HashMap<String, String > map, String shareid, Integer id, String token, String name, String tenantName);

    @Async
    public void updateChartStatus(Integer id, String token, String name, String tenantName);

    void preCheckCommon(Dashboard dashboard, String name);

    List<Dashboard> getDash(Integer id);

    List<Chart> getChart(Integer id,String token,String name) throws IOException;

    List getTree(String name);

    PageInfo<List> getList(String type, String name, Integer pageNum, Integer pageSize);

    void addDashLog(String name, Integer id);

    void addFirstLevel(String name);

    List<Integer> getAuth(String name);
}
