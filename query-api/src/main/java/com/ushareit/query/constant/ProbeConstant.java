package com.ushareit.query.constant;

import java.util.HashMap;
import java.util.Map;

public class ProbeConstant {
    public static final Map<Integer, String> columnTypeMap = new HashMap<Integer, String>();
    static {
        // top2
        columnTypeMap.put(1, "string");         // CHAR
        columnTypeMap.put(12, "string");        // VARCHAR
        columnTypeMap.put(-1, "string");        // LONGVARCHAR
        columnTypeMap.put(91, "date");          // DATE

        // 直方图
        columnTypeMap.put(-6, "integer");       // TINYINT
        columnTypeMap.put(5, "integer");        // SMALLINT
        columnTypeMap.put(4, "integer");        // INTEGER
        columnTypeMap.put(-5, "integer");       // BIGINT
        columnTypeMap.put(8, "double");         // DOUBLE
        columnTypeMap.put(6, "float");          // FLOAT
        columnTypeMap.put(3, "float");          // DECIMAL

        // 饼图
        columnTypeMap.put(16, "boolean");       // BOOLEAN

        // 其他类型
        columnTypeMap.put(2003, "array");       // ARRAY
        columnTypeMap.put(2000, "object");      // JAVA_OBJECT
        columnTypeMap.put(92, "timestamp");     // TIME
        columnTypeMap.put(93, "timestamp");     // TIMESTAMP
        columnTypeMap.put(-2, "binary");        // BINARY
        columnTypeMap.put(-3, "binary");        // VARBINARY
        columnTypeMap.put(-4, "binary");        // LONGVARBINARY
        columnTypeMap.put(2004, "blob");        // BLOB 存储文件的字节上限，最大65k，使用二进制保存数据，如保存位图
        columnTypeMap.put(2005, "clob");        // CLOB 字符大型对象，与LONG数据类型类似，只不过CLOB用于存储数据库中的大型单字节字符数据块， 最大4G。使用时不需要设置长度，直接使用，使用char保存数据，如XMl文档
    }
}
