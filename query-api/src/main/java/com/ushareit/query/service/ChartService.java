package com.ushareit.query.service;

import com.github.pagehelper.PageInfo;
import com.ushareit.query.bean.Chart;
import com.ushareit.query.bean.Sharebi;
import com.ushareit.query.web.vo.BaseResponse;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.scheduling.annotation.Async;

/**
 * @author: wangsy1
 * @create: 2022-11-07 15:22
 */
public interface ChartService extends BaseService<Chart> {

    Integer addChart(String shareid, HashMap<String,String> map, String token,String name) throws IOException, Exception;

    String updateChart(String shareid, HashMap<String,String> map,String token,String name,String tenantName);

    String deleteChart(String name, Integer id,String token) throws IOException;

    int addShareGrade(Sharebi sh, String shareeEmail);

    List<Chart> getChart(Integer id);

    PageInfo<List> getList(String type, String tenantName, String name, Integer pageNum, Integer pageSize,String title,String token) throws IOException;

    List<Map<String,String>> getJinja(String shareid, List<String> jinja);

    List<ResultSet> getDataback(String name, String tenantName, Map<String, Object> params, String bidt, Integer cid) throws SQLException;

    void test(Integer id) throws IOException;

    void addViewLog(String name, Integer id);

    void online(String shareid, Integer id) throws IOException;

    String selectByUsername(String title, String name, Integer active);

    String selectByUsernameUpdate(String title, String name, Integer active,Integer id);

    @Async
    void addDE(String shareid, HashMap<String,String> map, String token,String name,String tenantName)
            throws Exception;

    @Async
    void updateChartStatus(String type, String name, String title,String token);
}
