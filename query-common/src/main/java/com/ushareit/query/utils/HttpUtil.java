package com.ushareit.query.utils;

import com.alibaba.fastjson.JSON;
import com.ushareit.query.constant.BaseConstant;
import com.ushareit.query.constant.BaseResponseCodeEnum;
import com.ushareit.query.web.vo.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.*;

/**
 * @program: hebe
 * @description:
 * @author: wuyan
 * @create: 2020-04-28 14:19
 **/
@Slf4j
public class HttpUtil {

    private static RequestConfig requestConfig = RequestConfig.custom()
            .setSocketTimeout(100000)
            .setConnectTimeout(100000)
            .setConnectionRequestTimeout(100000)
            .build();

    public static BaseResponse put(String url, String json) {
        return put(url,json,null);
    }

    public static BaseResponse put(String url, String json, Map<String, String> headers) {
        return put(url,json,headers,null);
    }

    public static BaseResponse put(String url, String json, Map<String, String> headers, String encode) {
        if (encode == null) {
            encode = "utf-8";
        }
        CloseableHttpClient closeableHttpClient = HttpClients.createDefault();
        HttpPut httpPut = new HttpPut(url);
        httpPut.setConfig(requestConfig);

        //设置header
        if (headers != null && headers.size() > 0) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                httpPut.setHeader(entry.getKey(), entry.getValue());
            }
        }
        ContentType contentType = ContentType.create("text/plain", "UTF-8");
        httpPut.setHeader(new BasicHeader("Content-Type", "application/json;charset=utf-8"));
        try {
            httpPut.setEntity(new StringEntity(json,contentType));
        } catch (Exception e1) {
            log.error(e1.getMessage(),e1);
        }

        return execute(closeableHttpClient, httpPut, encode);
    }

    public static BaseResponse doPost(String url, Map<String, String> params) {
        return doPost(url, params, null);
    }

    public static BaseResponse doPost(String url, Map<String, String> params, Map<String, String> headers) {
        return doPost(url, params, headers, null);
    }

    public static BaseResponse doPost(String url, Map<String, String> params, Map<String, String> headers, String encode) {
        return doPost(url, params, null, headers, encode);
    }

    public static BaseResponse doPostWithJar(String url, String jarFile) {
        return doPostWithJar(url, null, jarFile);
    }

    public static BaseResponse doPostWithJar(String url,Map<String, String> params, String jarFile) {
        return doPost(url, params, jarFile, null, null);
    }

    public static BaseResponse doPost(String url, Map<String, String> params, String jarFile, Map<String, String> headers, String encode) {
        if (encode == null) {
            encode = "utf-8";
        }
        CloseableHttpClient closeableHttpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(url);
        httpPost.setConfig(requestConfig);

        //设置header
        if (headers != null && headers.size() > 0) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                httpPost.setHeader(entry.getKey(), entry.getValue());
            }
        }
        //组织请求参数，并解决中文乱码
        MultipartEntityBuilder builder = MultipartEntityBuilder.create().setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        builder.setCharset(Charset.forName(encode));
        ContentType contentType = ContentType.create("text/plain", "UTF-8");
        if (params != null && params.size() > 0) {
            Set<String> keySet = params.keySet();
            for (String key : keySet) {
                builder.addPart(key, new StringBody(params.get(key), contentType));
            }
        }
        //设置jarfile
        if (jarFile != null) {
            String filename = jarFile.substring(jarFile.lastIndexOf(System.getProperty("file.separator")) + 1);
            try {
                builder.addBinaryBody(
                        "jarFile",
                        new FileInputStream(jarFile),
                        ContentType.create("application/x-java-archive"),
                        filename
                );
            } catch (FileNotFoundException e) {
                log.error(e.getMessage(),e);
            }
            builder.setContentType(ContentType.MULTIPART_FORM_DATA);
        }

        HttpEntity multipart = builder.build();
        httpPost.setEntity(multipart);

        return execute(closeableHttpClient, httpPost, encode);
    }

    public static BaseResponse postWithJson(String url, String json) {
        return postWithJson(url,json,null);
    }

    public static BaseResponse postWithJson(String url, String json, Map<String, String> headers) {
        return postWithJson(url,json,headers,null);
    }
    /**
     * HttpPost请求 用json 不限时
     *
     * @param url     http的url
     * @param json
     * @param headers
     * @param encode
     * @return
     */
    public static BaseResponse postWithJson(String url, String json, Map<String, String> headers, String encode) {
        if (encode == null) {
            encode = "utf-8";
        }
        CloseableHttpClient closeableHttpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(url);
        httpPost.setConfig(requestConfig);
        //设置header
        if (headers != null && headers.size() > 0) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                httpPost.setHeader(entry.getKey(), entry.getValue());
            }
        }
        ContentType contentType = ContentType.create("text/plain", "UTF-8");
        httpPost.setHeader(new BasicHeader("Content-Type", "application/json;charset=utf-8"));
        try {
            httpPost.setEntity(new StringEntity(json,contentType));
        } catch (Exception e1) {
            log.error(e1.getMessage(),e1);
        }
        return execute(closeableHttpClient, httpPost, encode);
    }

    public static BaseResponse delete(String url) {
        return delete(url,null);
    }

    public static BaseResponse delete(String url, Map<String, String> params) {
        return delete(url,params,null);
    }

    public static BaseResponse delete(String url, Map<String, String> params, Map<String, String> headers) {
        return delete(url,params,headers,null);
    }

    public static BaseResponse delete(String url, Map<String, String> params, Map<String, String> headers, String encode) {
        HttpDelete httpDelete = new HttpDelete();
        return urlRequestBase(httpDelete, url, params, headers, encode);
    }

    public static BaseResponse get(String url) {
        return get(url,null);
    }

    public static BaseResponse get(String url, Map<String, String> params) {
        return get(url,params,null);
    }

    public static BaseResponse get(String url, Map<String, String> params, Map<String, String> headers) {
        return get(url,params,headers,null);
    }

    public static BaseResponse get(String url, Map<String, String> params, Map<String, String> headers, String encode) {
        HttpGet httpGet = new HttpGet();
        return urlRequestBase(httpGet, url, params, headers, encode);
    }

    public static BaseResponse patch(String url) {
        return patch(url,null);
    }

    public static BaseResponse patch(String url, Map<String, String> params) {
        return patch(url,params,null);
    }

    public static BaseResponse patch(String url, Map<String, String> params, Map<String, String> headers) {
        return patch(url,params,headers,null);
    }

    public static BaseResponse patch(String url, Map<String, String> params, Map<String, String> headers, String encode) {
        HttpPatch httpPatch = new HttpPatch();
        return urlRequestBase(httpPatch, url, params, headers, encode);
    }


    private static BaseResponse<String> execute(CloseableHttpClient closeableHttpClient, HttpUriRequest request, String encode) {
        CloseableHttpResponse httpResponse = null;
        BaseResponse<String> instance;
        try {
            httpResponse = closeableHttpClient.execute(request);
            instance = setResponse(httpResponse, encode);
        } catch (Exception e) {
            log.error(e.getMessage(),e);
            return BaseResponse.error(BaseResponseCodeEnum.SYS_UNA, String.format("等待服务可用中,失败原因:s%",e));
        } finally {
            try {
                if (httpResponse != null) {
                    httpResponse.close();
                }
            } catch (IOException e) {
                log.error(e.getMessage(),e);
            }
        }
        try {  //关闭连接、释放资源
            closeableHttpClient.close();
        } catch (IOException e) {
            log.error(e.getMessage(),e);
            return BaseResponse.error(BaseResponseCodeEnum.SYS_ERR, "closeableHttpClient关闭失败");
        }

        return instance;
    }

    private static BaseResponse setResponse(CloseableHttpResponse httpResponse, String encode) throws IOException {
        int statusCode = httpResponse.getStatusLine().getStatusCode();
        HttpEntity entity = httpResponse.getEntity();
        String content = null;
        Map<String, Object> map = null;
        try {
            if(entity != null){
                content = EntityUtils.toString(entity, encode);
                map = JSON.parseObject(content, Map.class);
            }
        } catch (Exception e) {
        }

        BaseResponse instance;
        switch (statusCode) {
            case 200:
            case 204:
                if (map != null && map.size() != 0 && map.get(BaseConstant.RES_CODE) != null && map.get(BaseConstant.RES_MESSAGE) != null) {
                    String code = String.valueOf(map.get(BaseConstant.RES_CODE)) ;
                    String message = (String) map.get(BaseConstant.RES_MESSAGE);
                    instance = BaseResponse.getInstance(code, message, map.get(BaseConstant.RES_DATA));
                } else {
                    instance = BaseResponse.getInstance(BaseResponseCodeEnum.SUCCESS.name(), BaseResponseCodeEnum.SUCCESS.getMessage(), content);
                }
                break;
            case 202:
                map.put("statusCode", "202");
                instance = BaseResponse.getInstance(BaseResponseCodeEnum.SUCCESS.name(), BaseResponseCodeEnum.SUCCESS.getMessage(), JSON.toJSONString(map));
                break;
            case 400:
                instance = BaseResponse.getInstance(BaseResponseCodeEnum.REQUEST_ILLEGAL.name(), BaseResponseCodeEnum.REQUEST_ILLEGAL.getMessage(), JSON.toJSONString(map));
                break;
            default:
                instance = BaseResponse.getInstance(BaseResponseCodeEnum.SYS_UNA.name(), BaseResponseCodeEnum.SYS_UNA.getMessage(), JSON.toJSONString(map));
                break;
        }
        return instance;
    }

    private static BaseResponse urlRequestBase(HttpRequestBase httpbase, String url, Map<String, String> params, Map<String, String> headers, String encode) {
        if (encode == null) {
            encode = "utf-8";
        }
        CloseableHttpClient closeableHttpClient = HttpClients.createDefault();
        //组织请求参数
        List<NameValuePair> paramList = new ArrayList<NameValuePair>();
        if (params != null && params.size() > 0) {
            Set<String> keySet = params.keySet();
            for (String key : keySet) {
                paramList.add(new BasicNameValuePair(key, params.get(key)));
            }
        }
        String getQuery = null;
        try {
            getQuery = EntityUtils.toString(new UrlEncodedFormEntity(paramList, encode));
        } catch (Exception e1) {
            log.error(e1.getMessage(),e1);
        }

        if (getQuery != null && !"".equals(getQuery)) {
            url = url + "?" + getQuery;
        }

        httpbase.setURI(URI.create(url));
        httpbase.setConfig(requestConfig);
        //设置header
        if (headers != null && headers.size() > 0) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                httpbase.setHeader(entry.getKey(), entry.getValue());
            }
        }

        return execute(closeableHttpClient, httpbase, encode);
    }
}