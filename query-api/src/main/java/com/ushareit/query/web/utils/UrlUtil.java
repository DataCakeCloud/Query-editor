package com.ushareit.query.web.utils;

import com.ushareit.query.constant.BaseResponseCodeEnum;
import com.ushareit.query.constant.DsTaskConstant;
import com.ushareit.query.exception.ServiceException;
import com.ushareit.query.trace.holder.InfTraceContextHolder;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author: wuyan
 * @create: 2020-05-10 10:42
 **/
public class UrlUtil {
    /**
     * 将一个URL路径字符串转成file路径字符串
     * 典型的URL和File路径的比较：
     * URL：file:/D:/my%20java/URL&FILE/%e5%9b%be%e7%89%87/tongji.jpg
     * File：D:/my java/URL&FILE/图片/tongji.jpg
     *
     * @param url
     * @return
     */
    public static String urlToAbsPathString(String url) {
        try {
            URL url2 = new URL(url);
            try {
                File file = new File(url2.toURI());
                String absolutePath = file.getAbsolutePath();
                return absolutePath;
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Matcher getMatcher(String url, String pattern) {
        // 创建 Pattern 对象
        Pattern r = Pattern.compile(pattern);

        // 现在创建 matcher 对象
        Matcher matcher = r.matcher(url);
        if (!matcher.find()) {
            throw new ServiceException(BaseResponseCodeEnum.UNKOWN);
        }
        return matcher;
    }

    /*public static String getSchedulerUrl() {
        return MessageFormat.format(DsTaskConstant.SCHEDULER_URL, InfTraceContextHolder.get().getEnv());
    }*/
}