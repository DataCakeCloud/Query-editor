package com.ushareit.query.web.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;

@Slf4j
public class DataAPIUtil {
    public static String getAPIPath(String path, Integer id, String param) {
        ArrayList<String> pathParamList = new ArrayList<>();
        String pathParamStr = "";
        pathParamList.add(String.format("id=%s", id));

        if (!param.equals("")) {
            String[] paramList = param.split(",");
            for(int i=0;i<paramList.length;i++){
                pathParamList.add(String.format("%s={{%s}}", paramList[i], paramList[i]));
            }
        }
        pathParamStr = String.join("&", pathParamList);
        path = path + pathParamStr;
        return path;
    }
}
