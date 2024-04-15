package com.ushareit.query.web.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

import org.springframework.stereotype.Service;
import com.alibaba.fastjson.JSON;
import com.ushareit.query.constant.CommonConstant;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ClusterManagerUtil {

    public static String getClusterManagerInfo(String url, String userInfo) {
        BufferedReader in = null;
        String response = "";
        try {
            URL realUrl = new URL(url);
            URLConnection connection = realUrl.openConnection();
            connection.setRequestProperty("method", "GET");
            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            connection.setRequestProperty(CommonConstant.CURRENT_LOGIN_USER, userInfo);
            connection.connect();

            in = new BufferedReader(new InputStreamReader(
                    connection.getInputStream(), "UTF-8"));
            String line = "";
            while ((line = in.readLine()) != null) {
                response += line;
            }
            log.info(String.format("get cluster info %s", response));
        } catch (Exception e) {
            log.error(String.format("There is an exception occurred while get cluster info[%s]: %s",
            		url, CommonUtil.printStackTraceToString(e)));
            // throw e;
        }

        finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e2) {
                log.error(String.format("There is an exception occurred while get cluster info[%s]",
                		url, CommonUtil.printStackTraceToString(e2)));
                // throw e2;
            }
        }
        return response;
    }
}
