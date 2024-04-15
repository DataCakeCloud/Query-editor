package com.ushareit.query.utils;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

/**
 * @author tianxu
 * @date 2023/12/14 19:31
 **/
public class DDL {
    public static Set<String> SAFE_DDL_SQL_KEY = ImmutableSet.of("CREATE", "ALTER", "DESC", "DESCRIBE", "MSCK", "DROP");

    public static Set<String> DML_SQL_KEY = ImmutableSet.of("SELECT", "LOAD", "INSERT");
    public static boolean isSafeDDLSQL(String newSql) {
        boolean safe = false;
        String[] words = newSql.split(" ");
        for(String word : words){
            for (String dml : DML_SQL_KEY) {
                if (word.trim().equals(dml)) {
                    return false; //if sql is DML return false
                }
            }
        }
        for(String word : words){
            for (String ddl : SAFE_DDL_SQL_KEY) {
                if (word.equals(ddl)) {
                    safe = true;
                    break;
                }
            }
        }

        if (newSql.contains("SHOW ") && newSql.contains("PARTITIONS ")) {
            return true;
        }
//        for (String ddl : DANGER_DDL_SQL_KEY) {
//            if (newSql.contains(ddl)) {
//                safe = false;
//                break;
//            }
//        }
        return safe;
    }
}
