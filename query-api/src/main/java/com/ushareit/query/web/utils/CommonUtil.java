package com.ushareit.query.web.utils;


import com.alibaba.fastjson.JSONObject;
// import com.netflix.genie.client.security.basic.impl.BasicSecurityInterceptor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.opencsv.CSVReader;
import com.ushareit.query.bean.Account;
import com.ushareit.query.constant.CommonConstant;
import com.ushareit.query.exception.ServiceException;
import io.prestosql.jdbc.PrestoResultSet;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.*;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.security.cert.X509Certificate;
import java.sql.*;
import java.util.*;

/**
 * @author wuyan
 * @date 2021/4/9
 */

@Slf4j
public class CommonUtil {
//    public static JSONObject getUserGroup(String name, List<Account> accounts, String adminUsername, String adminPassword) {
//        String group = "";
//        JSONObject groupAccount = new JSONObject();
//
//        BasicSecurityInterceptor securityInterceptor = new BasicSecurityInterceptor(adminUsername, adminPassword);
//
//        OkHttpClient client = new OkHttpClient.Builder()
//                .addInterceptor(securityInterceptor)
//                .retryOnConnectionFailure(false)
//                .connectionPool(new ConnectionPool(5,10, TimeUnit.SECONDS))
//                .build();
//
//        String url = String.format("https://ranger.ushareit.org/service/roles/roles?userName=%s", name);
//
//        Request request = new Request
//                .Builder()
//                .url(url)
//                .header("Accept", "application/json")
//                .build();
//
//        Response response = null;
//
//        try {
//            response = client.newCall(request).execute();
//            if (response.code() == 200) {
//                String resultStr = response.body().string();
//                JSONObject resultJson = JSONObject.parseObject(resultStr);
//                JSONArray vXUsers = resultJson.getJSONArray("roles");
//                if (vXUsers.size() > 0) {
//                    group = vXUsers.getJSONObject(0).getJSONArray("groups").getJSONObject(0).getString("name");
//                    log.info(String.format("user[%s] belongs to group[%s]",name, group));
//                }
//            }
//
//            String username = "";
//            String password = "";
//            for (int i = 0; i < accounts.size(); i++) {
//                if (accounts.get(i).getUserGroup().equals(group)) {
//                    username = accounts.get(i).getUsername();
//                    password = accounts.get(i).getPassword();
//                    break;
//                }
//            }
//            groupAccount.put("username", username);
//            groupAccount.put("password", password);
//            groupAccount.put("group", group);
//
//        } catch (IOException e) {
//            e.printStackTrace();
//            log.error(String.format("user[%s] failed to get group: %s", name, e.getMessage()));
//        } finally {
//            if (response != null) {
//                response.body().close();
//            }
//        }
//
//        return groupAccount;
//    }
//
//    public static JSONObject getSparkUserGroup(String name, List<Account> accounts, String adminUsername, String adminPassword) {
//        String group = "";
//        JSONObject groupAccount = new JSONObject();
//
//        BasicSecurityInterceptor securityInterceptor = new BasicSecurityInterceptor(adminUsername, adminPassword);
//
//        OkHttpClient client = new OkHttpClient.Builder()
//                .addInterceptor(securityInterceptor)
//                .retryOnConnectionFailure(false)
//                .connectionPool(new ConnectionPool(5,10, TimeUnit.SECONDS))
//                .build();
//
//        String url = "https://genie.ushareit.org" + String.format("/api/v3/users/%s/roles", name);
//
//        Request request = new Request
//                .Builder()
//                .url(url)
//                .build();
//
//        Response response = null;
//        try {
//            response = client.newCall(request).execute();
//            if (response.code() == 200) {
//                String resultStr = response.body().string();
//                JSONArray array = JSONArray.parseArray(resultStr);
//                for(int i = 0; i < array.size(); i++) {
//                    String role = array.getJSONObject(i).getString("role");
//                    if (!role.equals("view_all")) {
//                        group = role;
//                        log.info(String.format("user[%s] belongs to group[%s]", name, group));
//                        break;
//                    }
//                }
//            }
//
//            String username = "";
//            String password = "";
//            for (int i = 0; i < accounts.size(); i++) {
//                if (accounts.get(i).getUserGroup().equals("spark")) {
//                    username = accounts.get(i).getUsername();
//                    password = accounts.get(i).getPassword();
//                    break;
//                }
//            }
//            groupAccount.put("username", username);
//            groupAccount.put("password", password);
//            groupAccount.put("group", group);
//
//        } catch (IOException e) {
//            e.printStackTrace();
//            log.error(String.format("user[%s] failed to get spark group: %s", name, e.getMessage()));
//        } finally {
//            if (response != null) {
//                response.body().close();
//            }
//        }
//
//        return groupAccount;
//    }

    public static ArrayList getQueryResults(String username, String password, String url, String sql, String provider) {
        Properties properties = new Properties();
        properties.setProperty("user", username);

        if (!provider.equals("huawei") && password.length() > 0) {
            properties.setProperty("password", password);
            properties.setProperty("SSL","true");
        } else {
            properties.setProperty("SSL","false");
        }

        Connection connection = null;
        Statement statement = null;
        ResultSet rs = null;
        ArrayList results = new ArrayList<>();

        try{
            connection = DriverManager.getConnection(url, properties);
            statement = connection.createStatement();
            rs = statement.executeQuery(sql);
            String queryId = rs.unwrap(PrestoResultSet.class).getQueryId();
            System.out.println("queryId:"+queryId);
            int colNum = rs.getMetaData().getColumnCount();
            while(rs.next()){
//                        lines++;
//                        System.out.println(rs.getString(1));
                if (provider.equals("aws_sg")) {
                    HashMap<String, String> columns = new HashMap<>();
                    String columnName = rs.getString(1).replace("\"", "").replace(" ", "");
                    String columnType = "";
                    columns.put("columnName", columnName);
                    columns.put("columnType", columnType);
                    results.add(columns);
                } else {
                    if (colNum == 1) { // databases or tables
                        if (!rs.getString(1).equals("system") && !rs.getString(1).equals("tpcds")) {
                            results.add(rs.getString(1));
                        }
                    } else if (colNum > 3) {  // columns
                        HashMap<String, String> columns = new HashMap<>();
                        String columnName = rs.getString(1);
                        String columnType = rs.getString(2);
                        String columnComment = rs.getString(4);
                        columns.put("columnName", columnName);
                        columns.put("columnType", columnType);
                        columns.put("columnComment", columnComment);
                        results.add(columns);
                    } else if (colNum > 2) { // desc extended
                        String col_name = rs.getString(1);
                        if (0 == col_name.compareToIgnoreCase("Owner")) {
                            results.add(rs.getString(2));
                            break;
                        }
                    }
                }
            }
        }catch(Exception e){
            try{
                throw e;
            }catch(SQLException se){
                se.printStackTrace();
            }
        }finally{
            try{
                if(rs != null){
                    rs.close();
                }
                if(statement != null){
                    statement.close();
                    connection.close();
                }
            }catch(SQLException se){
                se.printStackTrace();
            }
        }

        return results;
    }

    public static JSONObject getUsernameAndPassword(List<Account> account, String group, String engine, String awsSGUrl, String awsUrl, String huaweiUrl) {
        JSONObject connectInfo = new JSONObject();
        String username = "";
        String password = "";
        for (int i = 0; i < account.size(); i++) {
            if (account.get(i).getUserGroup().equals(group)) {
                username = account.get(i).getUsername();
                password = account.get(i).getPassword();
                break;
            }
        }

        String url = "";
        String provider = "aws";

        if (engine.equals("presto_aws_sg") || engine.equals("spark-submit-sql-3_aws_ap-southeast-1") || engine.equals("ares_ap1") || engine.equals("smart_aws_sg")) {
            url = awsSGUrl;
        } else if (engine.equals("presto_huawei") || engine.equals("spark-submit-sql-3_huawei_ap-southeast-3") || engine.equals("smart_huawei")) {
            url = huaweiUrl;
            provider = "huawei";
        } else {
            url = awsUrl;
        }

        connectInfo.put("username", username);
        connectInfo.put("password", password);
        connectInfo.put("url", url);
        connectInfo.put("provider", provider);

        return connectInfo;
    }

    public static String printStackTraceToString(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw, true));
        return sw.getBuffer().toString().trim();
    }

    public static String renderSql(String url, String token, String sql, String uuid, String executionDate) {
        String renderUrl = url + CommonConstant.RENDER_SQL;
        Map<String, String> heads = new HashMap<>();
        heads.put("Authentication", token);
        heads.put("Content-Type", "application/json");
        Map<String, String> params = new HashMap<>();
        params.put("sql", sql);
        params.put("executionDate", executionDate);
        String param = JSONObject.toJSONString(params);
        return httpResult(renderUrl, false, param, heads, uuid);
    }

    public static String httpResult(String url, boolean isGet, String postData, Map<String, String> heads, String uuid) {
        BufferedReader in = null;
        BufferedWriter writer = null;
        log.info(String.format("[uuid=%s]httpResult url=%s", uuid, url));
        try {
            URL realUrl = new URL(url);
            URLConnection connection = realUrl.openConnection();
            if (isGet) {
                connection.setRequestProperty("method", "GET");
            } else {
                connection.setRequestProperty("method", "POST");
                connection.setDoOutput(true);
            }
            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("Content-Type", "text/plain");
            connection.setConnectTimeout(900000);
            connection.setReadTimeout(900000);
            if (null != heads) {
                for (Map.Entry<String, String> entry : heads.entrySet()) {
                    connection.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }
            if (!isGet) {
                OutputStream outputStream = connection.getOutputStream();
                writer = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));
                writer.write(postData);
                writer.close();
                writer = null;
            }
            connection.connect();

            in = new BufferedReader(new InputStreamReader(
                    connection.getInputStream(), "UTF-8"));
            String line = "";
            StringBuilder response = new StringBuilder();
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        } catch (Exception e) {
            log.error(String.format("[uuid=%s]There is an exception occurred while [%s] for http request: %s",
                    uuid, url, CommonUtil.printStackTraceToString(e)));
            throw new ServiceException(e.getMessage(), e.getMessage());
        } finally {
            try {
                if (null != in) {
                    in.close();
                }
                if (null != writer) {
                    writer.close();
                }
            } catch (Exception e2) {
                log.error(String.format("[uuid=%s]There is an exception occurred while [%s] for closing http: %s",
                        uuid, url, CommonUtil.printStackTraceToString(e2)));
            }
        }
    }


    public static String httpsReuslt(String url, boolean isGet, String postData, Map<String, String> heads) {
        BufferedReader in = null;
        BufferedWriter writer = null;
        try {
            // Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {

                        }
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {

                        }
                    }
            };

            // Install the all-trusting trust manager
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            // Create all-trusting host name verifier
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };

            // Install the all-trusting host verifier
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

            URL realUrl = new URL(url);
            URLConnection connection = realUrl.openConnection();
            if (isGet) {
                connection.setRequestProperty("method", "GET");
            } else {
                connection.setRequestProperty("method", "POST");
                connection.setDoOutput(true);
            }
            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("Content-Type", "text/plain");
            connection.setConnectTimeout(900000);
            connection.setReadTimeout(900000);
            if (null != heads) {
                for (Map.Entry<String, String> entry : heads.entrySet()) {
                    connection.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }
            if (!isGet) {
                OutputStream outputStream = connection.getOutputStream();
                writer = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));
                writer.write(postData);
                writer.close();
                writer = null;
            }
            connection.connect();

            in = new BufferedReader(new InputStreamReader(
                    connection.getInputStream(), "UTF-8"));
            String line = "";
            String response = "";
            while ((line = in.readLine()) != null) {
                response += line;
            }
            log.info(response);
            return response;
        } catch (Exception e) {
            log.error(String.format("There is an exception occurred while [%s] for https request: %s",
                    url, CommonUtil.printStackTraceToString(e)));
        }

        finally {
            try {
                if (null != in) {
                    in.close();
                }
                if (null != writer) {
                    writer.close();
                }
            } catch (Exception e2) {
                log.error(String.format("There is an exception occurred while [%s] for closing https: %s",
                        url, CommonUtil.printStackTraceToString(e2)));
            }
        }
        return "-1";
    }

    public static ArrayList getQueryResultsFromGW(Properties properties, String url, String sql, String provider) {
        Connection connection = null;
        Statement statement = null;
        ResultSet rs = null;
        log.error("url " + url);
        ArrayList results = new ArrayList<>();
        try{
            connection = DriverManager.getConnection(url, properties);
            statement = connection.createStatement();
            rs = statement.executeQuery(sql);
            // String queryId = rs.unwrap(PrestoResultSet.class).getQueryId();
            String queryId = "aq2124423dd";
            System.out.println("queryId:"+queryId);
            int colNum = rs.getMetaData().getColumnCount();
            while(rs.next()){
//                        lines++;
//                        System.out.println(rs.getString(1));
                if (provider.equals("aws_sg")) {
                    HashMap<String, String> columns = new HashMap<>();
                    String columnName = rs.getString(1).replace("\"", "").replace(" ", "");
                    String columnType = "";
                    columns.put("columnName", columnName);
                    columns.put("columnType", columnType);
                    results.add(columns);
                } else {
                    if (colNum == 1) { // databases or tables
                        if (!rs.getString(1).equals("system") && !rs.getString(1).equals("tpcds")) {
                            results.add(rs.getString(1));
                        }
                    } else if (colNum > 3) {  // columns
                        HashMap<String, String> columns = new HashMap<>();
                        String columnName = rs.getString(1);
                        String columnType = rs.getString(2);
                        String columnComment = rs.getString(4);
                        columns.put("columnName", columnName);
                        columns.put("columnType", columnType);
                        columns.put("columnComment", columnComment);
                        results.add(columns);
                    } else if (colNum > 2) { // desc extended
                        String col_name = rs.getString(1);
                        if (0 == col_name.compareToIgnoreCase("Owner")) {
                            results.add(rs.getString(2));
                            break;
                        }
                    }
                }
            }
        }catch(Exception e){
            try{
                throw e;
            }catch(SQLException se){
                se.printStackTrace();
            }
        }finally{
            try{
                if(rs != null){
                    rs.close();
                }
                if(statement != null){
                    statement.close();
                    connection.close();
                }
            }catch(SQLException se){
                se.printStackTrace();
            }
        }

        return results;
    }

    public static void transCsv2Pdf(String csvFilePath, String pdfFilePath) {
        try {
            /* Step -1 : Read input CSV file in Java */
            CSVReader reader = new CSVReader(new FileReader(csvFilePath));
            /* Variables to loop through the CSV File */
            String [] nextLine; /* for every line in the file */
            int lnNum = 0; /* line number */
            /* Step-2: Initialize PDF documents - logical objects */
            Document my_pdf_data = new Document();
            PdfWriter.getInstance(my_pdf_data, new FileOutputStream(pdfFilePath));
            my_pdf_data.open();
            PdfPTable my_first_table = null;
            int num_count = 0;
            /* Step -3: Loop through CSV file and populate data to PDF table */
            while ((nextLine = reader.readNext()) != null) {
                if (null == my_first_table) {
                    num_count = nextLine.length;
                    my_first_table = new PdfPTable(num_count);
                }
                lnNum++;
                for (int i = 0; i < nextLine.length && i < num_count; ++i) {
                    PdfPCell table_cell = new PdfPCell(new Phrase(nextLine[i]));
                    my_first_table.addCell(table_cell);
                }
            }
            /* Step -4: Attach table to PDF and close the document */
            my_pdf_data.add(my_first_table);
            my_pdf_data.close();
        } catch (Exception e) {
            log.error(String.format("tanns csv to pdf failed, %s %s",
                    e.getMessage(), CommonUtil.printStackTraceToString(e)));
        }
    }
}
