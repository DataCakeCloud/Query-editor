-- MySQL dump 10.14  Distrib 5.5.68-MariaDB, for Linux (x86_64)
--
-- Host: 127.0.0.1    Database: query_editor_payment
-- ------------------------------------------------------
-- Server version	5.7.38-220701-log

/*!40101 SET @OLD_CHARACTER_SET_CLIENT = @@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS = @@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION = @@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE = @@TIME_ZONE */;
/*!40103 SET TIME_ZONE = '+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS = @@UNIQUE_CHECKS, UNIQUE_CHECKS = 0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS = @@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS = 0 */;
/*!40101 SET @OLD_SQL_MODE = @@SQL_MODE, SQL_MODE = 'NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES = @@SQL_NOTES, SQL_NOTES = 0 */;

--
-- Table structure for table `chart`
--

/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS query_editor_${tenantName}.`chart`
(
    `id`             int(11)      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`           varchar(64)  NOT NULL COMMENT '名称',
    `type`           varchar(64)  NOT NULL COMMENT 'chart类型',
    `describe_chart` varchar(64)           DEFAULT NULL COMMENT '描述',
    `is_active`      int(11)      NOT NULL COMMENT '活跃判断',
    `is_share`       int(11)      NOT NULL COMMENT '分享判断',
    `is_favorate`    int(11)      NOT NULL,
    `query_sql`      text         NOT NULL COMMENT 'sql记录',
    `param`          longtext COMMENT 'chart参数',
    `uuid`           varchar(64)  NOT NULL,
    `engine`         varchar(64)           DEFAULT NULL,
    `create_by`      varchar(32)  NOT NULL COMMENT '创建人',
    `update_by`      varchar(100) NOT NULL COMMENT '更新人',
    `create_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    datetime              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `content`        text COMMENT 'ds调度的加密sql',
    `status`         varchar(100)          DEFAULT NULL COMMENT 'DE调度状态',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `classification`
--

/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS query_editor_${tenantName}.`classification`
(
    `id`          int(11)      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        varchar(64)  NOT NULL COMMENT '名称',
    `level`       varchar(64)  NOT NULL COMMENT '文件夹等级',
    `parent_id`   int(11)               DEFAULT NULL COMMENT '父级id',
    `is_active`   int(11)               DEFAULT NULL COMMENT '活跃判断',
    `is_query`    int(11)               DEFAULT NULL COMMENT '查询判断',
    `create_by`   varchar(32)  NOT NULL COMMENT '创建人',
    `update_by`   varchar(100) NOT NULL COMMENT '更新人',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 336
  DEFAULT CHARSET = utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `classificationdash`
--

/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS query_editor_${tenantName}.`classificationdash`
(
    `id`          int(11)      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        varchar(64)  NOT NULL COMMENT '名称',
    `level`       int(11)      NOT NULL COMMENT '文件夹等级',
    `parent_id`   int(11)               DEFAULT NULL COMMENT '父级id',
    `is_active`   int(11)      NOT NULL COMMENT '活跃判断',
    `is_query`    int(11)               DEFAULT NULL COMMENT '查询判断',
    `create_by`   varchar(32)  NOT NULL COMMENT '创建人',
    `update_by`   varchar(100) NOT NULL COMMENT '更新人',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 4
  DEFAULT CHARSET = utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dash_chart`
--

/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS query_editor_${tenantName}.`dash_chart`
(
    `id`           int(11)      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `dashboard_id` int(11)      NOT NULL COMMENT 'dashboard的id',
    `chart_id`     int(11)      NOT NULL COMMENT 'chart的id',
    `is_active`    int(11)      NOT NULL COMMENT '活跃判断',
    `create_by`    varchar(32)  NOT NULL COMMENT '创建人',
    `update_by`    varchar(100) NOT NULL COMMENT '更新人',
    `create_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  datetime              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dashboard`
--

/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS query_editor_${tenantName}.`dashboard`
(
    `id`            int(11)      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`          varchar(64)  NOT NULL COMMENT '名称',
    `describe_dash` text COMMENT '描述',
    `is_active`     int(11)      NOT NULL COMMENT '活跃判断',
    `is_share`      int(11)      NOT NULL COMMENT '分享判断',
    `is_schedule`   int(11)      NOT NULL COMMENT '调度判断',
    `param`         longtext COMMENT 'chart参数',
    `classid`       int(11)               DEFAULT NULL COMMENT '看板所属文件夹',
    `create_by`     varchar(32)  NOT NULL COMMENT '创建人',
    `update_by`     varchar(100) NOT NULL COMMENT '更新人',
    `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   datetime              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `crontab`       varchar(100)          DEFAULT NULL COMMENT 'crontab表达式',
    `is_favorate`   int(11)               DEFAULT NULL COMMENT '收藏标识',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `data_api`
--

/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS query_editor_${tenantName}.`data_api`
(
    `id`          int(11)     NOT NULL AUTO_INCREMENT COMMENT '主键',
    `title`       varchar(256)         DEFAULT NULL COMMENT 'API名称',
    `path`        varchar(10000)       DEFAULT NULL COMMENT 'API路径',
    `query_sql`   mediumtext COMMENT '查询sql',
    `engine`      varchar(256)         DEFAULT NULL COMMENT '查询引擎',
    `engineZh`    varchar(256)         DEFAULT NULL COMMENT '查询引擎中文',
    `param`       varchar(10000)       DEFAULT NULL COMMENT '查询sql中的参数',
    `status`      int(11)              DEFAULT NULL COMMENT 'API状态',
    `uuid`        varchar(64) NOT NULL,
    `create_by`   varchar(32) NOT NULL COMMENT '创建人',
    `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   varchar(32)          DEFAULT NULL COMMENT '更新人',
    `update_time` datetime             DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `region`      varchar(32)          DEFAULT NULL COMMENT 'region',
    `catalog`     varchar(32)          DEFAULT '' COMMENT 'catalog',
    PRIMARY KEY (`id`),
    KEY `uuidIndex` (`uuid`),
    KEY `createByIndex` (`create_by`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 59
  DEFAULT CHARSET = utf8 COMMENT ='数据API表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `data_engineer`
--

/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS query_editor_${tenantName}.`data_engineer`
(
    `id`          int(11)     NOT NULL AUTO_INCREMENT COMMENT '主键',
    `uuid`        varchar(64) NOT NULL,
    `engine`      varchar(256)         DEFAULT NULL COMMENT '查询引擎',
    `query_sql`   mediumtext COMMENT '查询sql',
    `create_by`   varchar(32) NOT NULL COMMENT '创建人',
    `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   varchar(32)          DEFAULT NULL COMMENT '更新人',
    `update_time` datetime             DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `uuidIndex` (`uuid`),
    KEY `createByIndex` (`create_by`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 54
  DEFAULT CHARSET = utf8 COMMENT ='数据调度信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `engine_auth`
--

/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS query_editor_${tenantName}.`engine_auth`
(
    `id`          int(11)     NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        varchar(64) NOT NULL DEFAULT 'admin' COMMENT '用户名',
    `engine`      json                 DEFAULT NULL COMMENT '用户有权限的引擎',
    `create_by`   varchar(32) NOT NULL COMMENT '创建人',
    `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   varchar(32)          DEFAULT NULL COMMENT '更新人',
    `update_time` datetime             DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 123
  DEFAULT CHARSET = utf8 COMMENT ='用户引擎权限表 ';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `favordashchart`
--

/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS query_editor_${tenantName}.`favordashchart`
(
    `id`          int(11)      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `favor_id`    int(11)      NOT NULL COMMENT '收藏id',
    `type`        varchar(64)  NOT NULL COMMENT '收藏类型',
    `is_active`   int(11)      NOT NULL COMMENT '活跃判断',
    `create_by`   varchar(32)  NOT NULL COMMENT '创建人',
    `update_by`   varchar(100) NOT NULL COMMENT '更新人',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `favortable`
--

/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS query_editor_${tenantName}.`favortable`
(
    `id`          int(11)      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        varchar(64)  NOT NULL COMMENT '名称',
    `is_active`   varchar(100) NOT NULL COMMENT '活跃判断',
    `region`      varchar(100) NOT NULL COMMENT '数据区域',
    `catalog`     varchar(100) NOT NULL COMMENT '数据源',
    `db`          varchar(100) NOT NULL COMMENT '数据表',
    `create_by`   varchar(32)  NOT NULL COMMENT '创建人',
    `update_by`   varchar(100) NOT NULL COMMENT '更新人',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 45
  DEFAULT CHARSET = utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `logview`
--

/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS query_editor_${tenantName}.`logview`
(
    `id`          int(11)     NOT NULL AUTO_INCREMENT,
    `type`        varchar(64) NOT NULL COMMENT '类型',
    `view_id`     int(11)  DEFAULT NULL,
    `create_time` datetime DEFAULT NULL,
    `create_by`   varchar(32) NOT NULL COMMENT '创建人',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `query_data`
--

/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS query_editor_${tenantName}.`query_data`
(
    `id`          int(11)      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `uuid`        varchar(64)  NOT NULL,
    `region`      varchar(64)  NOT NULL COMMENT '数据区域',
    `db`          varchar(100) NOT NULL COMMENT '数据表名',
    `detaskid`    int(11)      NOT NULL COMMENT 'de任务id',
    `is_active`   int(11)      NOT NULL COMMENT '活跃判断',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 2
  DEFAULT CHARSET = utf8 COMMENT ='uuid数据对照表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `query_history`
--

/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS query_editor_${tenantName}.`query_history`
(
    `id`                  int(11)     NOT NULL AUTO_INCREMENT COMMENT '主键',
    `uuid`                varchar(64) NOT NULL,
    `query_id`            varchar(256)         DEFAULT NULL COMMENT '查询id',
    `query_sql`           mediumtext,
    `engine`              varchar(10000)       DEFAULT NULL COMMENT '查询引擎',
    `user_group`          varchar(64)          DEFAULT NULL COMMENT '用户组',
    `status`              int(11)              DEFAULT NULL COMMENT '用户组',
    `start_time`          datetime             DEFAULT NULL COMMENT '查询时间',
    `execute_duration`    float(100, 3)        DEFAULT NULL,
    `data_size`           varchar(20)          DEFAULT NULL COMMENT '扫描数据量',
    `cpu_time_millis`     bigint(20)           DEFAULT NULL COMMENT 'cpu_time_millis',
    `wall_time_millis`    bigint(20)           DEFAULT NULL COMMENT 'wall_time_millis',
    `queued_time_millis`  bigint(20)           DEFAULT NULL COMMENT 'queued_time_millis',
    `elapsed_time_millis` bigint(20)           DEFAULT NULL COMMENT 'elapsed_time_millis',
    `processed_rows`      bigint(20)           DEFAULT NULL COMMENT 'processed_rows',
    `processed_bytes`     bigint(20)           DEFAULT NULL COMMENT 'processed_bytes',
    `scanSize`            varchar(20)          DEFAULT NULL COMMENT '扫描数据量转换后的值',
    `peak_memory_bytes`   bigint(20)           DEFAULT NULL COMMENT 'peak_memory_bytes',
    `create_by`           varchar(32) NOT NULL COMMENT '创建人',
    `create_time`         datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`           varchar(32)          DEFAULT NULL COMMENT '更新人',
    `update_time`         datetime             DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `engine_label`        varchar(100)         DEFAULT NULL,
    `statusZh`            text,
    `column_type`         text COMMENT '查询结果集中每一列的字段类型',
    `probe_result`        text COMMENT '探查结果',
    `probe_status`        int(11)              DEFAULT '0' COMMENT '探查状态',
    `total`               int(11)     NOT NULL DEFAULT '0' COMMENT '探查结果集的长度',
    `is_probe`            int(11)     NOT NULL DEFAULT '0' COMMENT '是否探查',
    `probe_sample`        mediumtext CHARACTER SET utf8mb4,
    `query_sql_param`     mediumtext COMMENT '带参数的sql',
    `param`               text COMMENT 'sql的参数',
    `api_id`              int(11)              DEFAULT NULL COMMENT '对应api的id',
    `mysql_async`         int(11)              DEFAULT NULL COMMENT 'mysql是否异步执行完成',
    `is_databend`         int(11)              DEFAULT NULL COMMENT '是否是databend加速任务',
    `region`              varchar(32)          DEFAULT NULL COMMENT 'region',
    `catalog`             varchar(32)          DEFAULT '' COMMENT 'catalog',
    `groupId`             varchar(320)         DEFAULT NULL COMMENT 'groupId',
    `tenantId`            int(5)               DEFAULT NULL COMMENT 'tenantId',
    `sessionId`           varchar(64)          DEFAULT NULL,
    `from_olap`           BOOLEAN              default null,
    `task_id`             int(11)              DEFAULT NULL COMMENT '定时任务id',
    PRIMARY KEY (`id`),
    KEY `uuidIndex` (`uuid`),
    KEY `createByIndex` (`create_by`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 407380
  DEFAULT CHARSET = utf8 COMMENT ='查询历史表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `saved_query`
--

/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS query_editor_${tenantName}.`saved_query`
(
    `id`          int(11)     NOT NULL AUTO_INCREMENT COMMENT '主键',
    `title`       varchar(64) NOT NULL COMMENT '名称',
    `query_sql`   text,
    `description` varchar(1024)        DEFAULT NULL COMMENT '查询描述',
    `engine`      varchar(10000)       DEFAULT NULL COMMENT '查询引擎',
    `user_group`  varchar(64)          DEFAULT NULL COMMENT '用户组',
    `create_by`   varchar(32) NOT NULL COMMENT '创建人',
    `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   varchar(32)          DEFAULT NULL COMMENT '更新人',
    `update_time` datetime             DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `engine_zh`   varchar(256)         DEFAULT NULL,
    `param`       text COMMENT 'sql的参数',
    `folderID`    int(11)              DEFAULT NULL COMMENT 'fold ID',
    `region`      varchar(32)          DEFAULT NULL COMMENT 'region',
    `catalog`     varchar(32)          DEFAULT '' COMMENT 'catalog',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 407358
  DEFAULT CHARSET = utf8 COMMENT ='保存查询表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `share_grade`
--

/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS query_editor_${tenantName}.`share_grade`
(
    `id`          int(11)      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `sharer`      varchar(32)  NOT NULL COMMENT '分享人',
    `sharee`      varchar(32)  NOT NULL COMMENT '被分享人',
    `grade`       int(3)       NOT NULL COMMENT '权限级别，1为编辑，2为运行，3为查看',
    `shareUrl`    varchar(300) NOT NULL COMMENT '分享URL',
    `sql_name`    varchar(32) COMMENT 'sql name',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 119
  DEFAULT CHARSET = utf8 COMMENT ='分享用户权限表 ';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sharebi`
--

/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS query_editor_${tenantName}.`sharebi`
(
    `id`          int(11)     NOT NULL AUTO_INCREMENT COMMENT '主键',
    `sharer`      varchar(64) NOT NULL COMMENT '分享人',
    `sharee`      varchar(64) NOT NULL COMMENT '被分享人',
    `type`        varchar(64) NOT NULL COMMENT '分享类型',
    `share_id`    int(11)     NOT NULL COMMENT '被分享chart_id或dashboard_id',
    `grade`       int(3)      NOT NULL COMMENT '权限级别，1为查看',
    `share_url`   text        NOT NULL COMMENT '分享url',
    `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 65
  DEFAULT CHARSET = utf8 COMMENT ='分享用户权限表 ';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user`
--

/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS query_editor_${tenantName}.`user`
(
    `id`          int(11)     NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        varchar(64) NOT NULL,
    `is_admin`    int(11)              DEFAULT '0' COMMENT '是否是管理员',
    `create_by`   varchar(32) NOT NULL COMMENT '创建人',
    `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   varchar(32)          DEFAULT NULL COMMENT '更新人',
    `update_time` datetime             DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 24
  DEFAULT CHARSET = utf8 COMMENT ='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `query_result`
--

/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS query_editor_${tenantName}.`query_result`
(
    `id`           int(11)    NOT NULL AUTO_INCREMENT COMMENT '主键',
    `query_inc_id` int(11)    NOT NULL COMMENT 'increasing query id',
    `result_str`   mediumtext NOT NULL COMMENT 'the result of query',
    `columns_str`  text       NOT NULL COMMENT 'columns info of result',
    `create_time`  datetime   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `inc_id_idx` (`query_inc_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8 COMMENT ='query result table';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `trans_sql`
--

/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS query_editor_${tenantName}.`trans_sql`
(
    `id`          int(11)     NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_name`   varchar(32) NOT NULL COMMENT '用户名',
    `origin_sql`  mediumtext  NOT NULL COMMENT '原sql',
    `origin_type` varchar(32) NOT NULL DEFAULT 'trino' COMMENT '原sql类型',
    `target_sql`  mediumtext COMMENT 'target sql',
    `target_type` varchar(32)          DEFAULT 'spark' COMMENT 'target type',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8 COMMENT ='sql转换表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE = @OLD_TIME_ZONE */;

--
-- Table structure for table `ai_chat`
--

/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS query_editor_${tenantName}.`ai_chat`
(
    `id`          int(11)      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `uuid`        varchar(40)  NOT NULL COMMENT 'uuid',
    `user_name`   varchar(100) NOT NULL COMMENT 'user_name',
    `content`     mediumtext   NOT NULL COMMENT 'content',
    `reply`       mediumtext   NOT NULL COMMENT 'reply',
    `create_time` timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8 COMMENT ='AI chat table';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE = @OLD_TIME_ZONE */;

--
-- Table structure for table `cron_query` 定时查询任务
--

/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS query_editor_${tenantName}.`cron_query`
(
    `id`          int(11)     NOT NULL AUTO_INCREMENT COMMENT '主键',
    `task_id`     int(11)     NOT NULL COMMENT 'DE任务Id',
    `task_name`   varchar(64) NOT NULL COMMENT '任务名称',
    `schedule`    varchar(64) NOT NULL COMMENT '调度频率',
    `email`       varchar(64) COMMENT '邮箱地址',
    `start_time`  varchar(64) COMMENT '任务开始时间',
    `end_time`    varchar(64) COMMENT '任务结束时间',
    `origin_sql`  text        NOT NULL COMMENT '原始带参数SQL语句',
    `user_name`   varchar(64) NOT NULL COMMENT 'owner',
    `user_group`  varchar(64) NOT NULL COMMENT '用户组',
    `engine`      varchar(64) COMMENT '执行引擎',
    `region`      varchar(64) NOT NULL COMMENT '区域',
    `catalog`     varchar(64) NOT NULL COMMENT '数据源',
    `db`          varchar(64) NOT NULL COMMENT '数据库',
    `status`      int(11)     NOT NULL COMMENT '任务状态',
    `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 2
  DEFAULT CHARSET = utf8 COMMENT ='定时任务数据表';
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40101 SET SQL_MODE = @OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS = @OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS = @OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT = @OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS = @OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION = @OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES = @OLD_SQL_NOTES */;

-- Dump completed on 2023-01-06 10:46:49
