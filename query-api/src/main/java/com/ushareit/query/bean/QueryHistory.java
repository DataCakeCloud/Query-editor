package com.ushareit.query.bean;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.sql.Timestamp;

/**
 * @author: huyx
 * @create: 2022-01-19 15:24
 */
@Data
@Entity
@Builder
@Table(name = "query_history")
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@ApiModel("查询记录表")
public class QueryHistory extends DataEntity {
    @ApiModelProperty("任务标识")
    @Column(name = "uuid")
    private String uuid;

    @ApiModelProperty("名称")
    @Column(name = "query_id")
    private String queryId;

    @ApiModelProperty("查询下载地址的映射key")
    @Column(name = "operation_id")
    private String operationId;

    @ApiModelProperty("查询语句")
    @Column(name = "query_sql")
    private String querySql;

    @ApiModelProperty("查询引擎")
    @Column(name = "engine")
    private String engine;

    @ApiModelProperty("查询引擎中文")
    @Column(name = "engine_label")
    private String engineLabel;

    @ApiModelProperty("用户组")
    @Column(name = "user_group")
    private String userGroup;

    @ApiModelProperty("查询状态")
    @Column(name = "status")
    private Integer status;

    @ApiModelProperty("查询状态中文")
    @Column(name = "statusZh")
    private String statusZh;

    @ApiModelProperty("开始时间")
    @Column(name = "start_time")
    private Timestamp startTime;

    @ApiModelProperty("执行时间")
    @Column(name = "execute_duration")
    private Float executeDuration;

    @ApiModelProperty("计算结果存储s3文件大小")
    @Column(name = "data_size")
    private String dataSize;

    @ApiModelProperty("cpu_time_millis")
    @Column(name = "cpu_time_millis")
    private long cpuTimeMillis;

    @ApiModelProperty("wall_time_millis")
    @Column(name = "wall_time_millis")
    private long wallTimeMillis;

    @ApiModelProperty("queued_time_millis")
    @Column(name = "queued_time_millis")
    private long queuedTimeMillis;

    @ApiModelProperty("elapsed_time_millis")
    @Column(name = "elapsed_time_millis")
    private long elapsedTimeMillis;

    @ApiModelProperty("processed_rows")
    @Column(name = "processed_rows")
    private long processedRows;

    @ApiModelProperty("云存储扫描数据量原始值")
    @Column(name = "processed_bytes")
    private long processedBytes;

    @ApiModelProperty("云存储扫描数据量转换值")
    @Column(name = "scanSize")
    private String scanSize;

    @ApiModelProperty("peak_memory_bytes")
    @Column(name = "peak_memory_bytes")
    private Long peakMemoryBytes;

    @ApiModelProperty("engine_label")
    @Column(name = "engine_label")
    private String engineabel;

    @ApiModelProperty("探查状态")
    @Column(name = "probe_status")
    private Integer probeStatus;

    @ApiModelProperty("查询结果集的column类型")
    @Column(name = "column_type")
    private String columnType;

    @ApiModelProperty("查询结果集的h行数")
    @Column(name = "total")
    private Integer total;

    @ApiModelProperty("探查结果")
    @Column(name = "probe_result")
    private String probeResult;

    @ApiModelProperty("是否探查")
    @Column(name = "is_probe")
    private Integer isProbe;

    @ApiModelProperty("探查样本")
    @Column(name = "probe_sample")
    private String probeSample;

    @ApiModelProperty("api的id")
    @Column(name = "api_id")
    private Integer apiId;

    @ApiModelProperty("带参数的sql")
    @Column(name = "query_sql_param")
    private String querySqlParam;

    @ApiModelProperty("sql的参数")
    @Column(name = "param")
    private String param;

    @ApiModelProperty("mysql是否异步执行完成")
    @Column(name = "mysql_async")
    private Integer mysqlAsync;

    @ApiModelProperty("是否是databend加速任务")
    @Column(name = "is_databend")
    private Integer isDatabend;

    @ApiModelProperty("region")
    @Column(name = "region")
    private String region;

    @ApiModelProperty("catalog")
    @Column(name = "catalog")
    private String catalog;

    @ApiModelProperty("groupId")
    @Column(name = "groupId")
    private String groupId;

    @ApiModelProperty("tenantId")
    @Column(name = "tenantId")
    private int tenantId;

    @ApiModelProperty("from_olap")
    @Column(name = "from_olap")
    private Boolean fromOlap;

    @ApiModelProperty("sessionId")
    @Column(name = "sessionId")
    private String sessionId;

    @ApiModelProperty("定时任务名称")
    @Column(name = "task_id")
    private Integer taskId;

    @ApiModelProperty("spark日志状态")
    @Column(name = "spark_log_status")
    private Integer sparkLogStatus;

    public void copy(QueryHistory queryHistory) {
        this.queryId = queryHistory.getQueryId();
        this.querySql = queryHistory.getQuerySql();
        this.engine = queryHistory.getEngine();
        this.engineLabel = queryHistory.getEngineLabel();
        this.userGroup = queryHistory.getUserGroup();
        this.status = queryHistory.getStatus();
        this.statusZh = queryHistory.getStatusZh();
        this.startTime = queryHistory.getStartTime();
        this.executeDuration = queryHistory.getExecuteDuration();
        this.dataSize = queryHistory.getDataSize();
        this.cpuTimeMillis = queryHistory.getCpuTimeMillis();
        this.wallTimeMillis = queryHistory.getWallTimeMillis();
        this.queuedTimeMillis = queryHistory.getQueuedTimeMillis();
        this.elapsedTimeMillis = queryHistory.getElapsedTimeMillis();
        this.processedRows = queryHistory.getProcessedRows();
        this.processedBytes = queryHistory.getProcessedBytes();
        this.peakMemoryBytes = queryHistory.getPeakMemoryBytes();
        this.engineLabel = queryHistory.getEngineLabel();
        this.probeStatus = queryHistory.getProbeStatus();
        this.total = queryHistory.getTotal();
        this.columnType = queryHistory.getColumnType();
        this.probeResult = queryHistory.getProbeResult();
        this.isProbe = queryHistory.getIsProbe();
        this.probeSample = queryHistory.getProbeSample();
        this.apiId = queryHistory.getApiId();
        this.querySqlParam = queryHistory.getQuerySqlParam();
        this.param = queryHistory.getParam();
        this.mysqlAsync = queryHistory.getMysqlAsync();
        this.isDatabend = queryHistory.getIsDatabend();
        this.region = queryHistory.getRegion();
        this.catalog = queryHistory.getCatalog();
        this.groupId = queryHistory.getGroupId();
        this.tenantId = queryHistory.getTenantId();
        this.fromOlap = queryHistory.getFromOlap();
        this.sessionId = queryHistory.getSessionId();
        this.taskId = queryHistory.getTaskId();
        this.sparkLogStatus = queryHistory.getSparkLogStatus();
        this.setCreateBy(queryHistory.getCreateBy());
        this.setUpdateBy(queryHistory.getUpdateBy());
    }
}
