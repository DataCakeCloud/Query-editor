package com.ushareit.query.service;

import com.ushareit.query.bean.CurrentUser;
import com.ushareit.query.bean.QueryHistory;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface GatewayService {
    HashMap<String, Object> execute(String uuid, String engine, String querySql,
                                    String region, String catalog, String database,
                                    CurrentUser currentUser, String token,
                                    String userInfo, Integer taskId, String executionDate) throws ParseException, SQLException;

    @Async
    HashMap<String, Object> cancel(String uuid, String user, String tenantName) throws IOException, InterruptedException;

    HashMap<String, String> getQueryId(String uuid);

    ResponseEntity<Object> download(String uuid, String user, String userInfo, String id, QueryHistory queryHistory) throws Exception;

    ResponseEntity<Object> downloadPdf(String uuid, String user, String userInfo, String id, QueryHistory queryHistory) throws Exception;

    String downloadToNative(String uuid, String user, String userInfo, QueryHistory queryHistory, String type) throws Exception;

    List downloadLink(String uuid, String user, String userInfo) throws Exception;

    HashMap<String, Object> historyData(String uuid, CurrentUser currentUser) throws Exception;

    Map<String, Object> queryLogText(String uuid, int from, int size, String user) throws Exception;

    HashMap<String, Object> getFileSize(String uuid, String user, String userInfo);

    HashMap<String, Object> statsInfo(Integer step, String uuid, String query_id);

    boolean fromOlap(String uuid);
}
