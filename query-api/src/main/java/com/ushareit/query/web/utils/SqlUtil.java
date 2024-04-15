package com.ushareit.query.web.utils;

import com.ushareit.query.constant.CommonConstant;

/**
 * @author tianxu
 * @date 2023/10/20 16:28
 **/
public class SqlUtil {

    public static Boolean isSelect(String sql) {
        String[] split = sql.split(";");
        String lastSql = split[split.length - 1].trim().toLowerCase();
        for(int i=0; i<CommonConstant.DDL.length; ++i) {
            if (lastSql.startsWith(CommonConstant.DDL[i])) {
                return false;
            }
        }
        return true;
    }

    public static void main(String[] args) {
        String sql = "SELECT\n" +
                "        *\n" +
                "      FROM\n" +
                "        ads_oa_attendence_abnormal_df\n" +
                "      WHERE\n" +
                "         pernr = '90012230'\n" +
                "         and dt='20231029'; select 1";
        System.out.println("aa=" + isSelect(sql));
    }
}
