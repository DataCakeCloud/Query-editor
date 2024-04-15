package com.ushareit.query.constant;

/**
 * @author: licg
 * @create: 2021-05-12 15:24
 */
public enum SourceTypeEnum {
    /**
     * 数据源类型
     */
    kafka_topic("kafka"),
    rdbms_table("mysql"),
    file("file"),
    es("es"),
    sharestore("sharestore"),
    iceberg("iceberg"),
    hive_table("hive"),
    clickhouse("clickhouse"),
    metis_pro("metis");

    private String type;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    SourceTypeEnum(String type) {
        this.type = type;
    }

    public static boolean isValid(String type) {
        for (SourceTypeEnum tmpType : SourceTypeEnum.values()) {
            if (tmpType.type.equalsIgnoreCase(type)) {
                return true;
            }
        }

        return false;
    }
}
