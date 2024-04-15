-- MySQL dump 10.13  Distrib 5.7.20, for macos10.12 (x86_64)
--
-- Host: dev.inf-common.cbs.sg2.mysql    Database: ds_task
-- ------------------------------------------------------
-- Server version	5.7.25-3-log

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
SET @MYSQLDUMP_TEMP_LOG_BIN = @@SESSION.SQL_LOG_BIN;
SET @@SESSION.SQL_LOG_BIN= 0;

--
-- GTID state at the beginning of the backup
--

SET @@GLOBAL.GTID_PURGED='deb232e6-ea41-11e9-9f39-fa163e3bacba:1-48494920';

--
-- Current Database: `query_editor`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `query_editor` /*!40100 DEFAULT CHARACTER SET utf8mb4 */;

USE `query_editor`;

--
-- Table structure for table `engine_auth`
--

DROP TABLE IF EXISTS `engine_auth`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `engine_auth` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(64) NOT NULL DEFAULT 'admin' COMMENT '用户名',
  `engine` json DEFAULT NULL COMMENT '用户有权限的引擎',
  `create_by` varchar(32) NOT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(32) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='用户引擎权限表 ';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `query_history`
--

DROP TABLE IF EXISTS `query_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `query_history` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `uuid` varchar(64) NOT NULL,
  `query_id` varchar(256) DEFAULT NULL COMMENT '查询id',
  `query_sql` varchar(10000) DEFAULT NULL COMMENT '查询语句',
  `engine` varchar(10000) DEFAULT NULL COMMENT '查询引擎',
  `user_group` varchar(64) DEFAULT NULL COMMENT '用户组',
  `status` int(11) DEFAULT NULL COMMENT '用户组',
  `start_time` datetime DEFAULT NULL COMMENT '查询时间',
  `execute_duration` float(100,0) DEFAULT NULL COMMENT '执行时间',
  `data_size` varchar(20) DEFAULT NULL COMMENT '扫描数据量',
  `cpu_time_millis` bigint(20) DEFAULT NULL COMMENT 'cpu_time_millis',
  `wall_time_millis` bigint(20) DEFAULT NULL COMMENT 'wall_time_millis',
  `queued_time_millis` bigint(20) DEFAULT NULL COMMENT 'queued_time_millis',
  `elapsed_time_millis` bigint(20) DEFAULT NULL COMMENT 'elapsed_time_millis',
  `processed_rows` bigint(20) DEFAULT NULL COMMENT 'processed_rows',
  `processed_bytes` bigint(20) DEFAULT NULL COMMENT 'processed_bytes',
  `peak_memory_bytes` bigint(20) DEFAULT NULL COMMENT 'peak_memory_bytes',
  `create_by` varchar(32) NOT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(32) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='查询历史表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `saved_query`
--

DROP TABLE IF EXISTS `saved_query`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `saved_query` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title` varchar(64) NOT NULL COMMENT '名称',
  `query_sql` varchar(10000) NOT NULL COMMENT '查询语句',
  `description` varchar(1024) DEFAULT NULL COMMENT '查询描述',
  `engine` varchar(10000) DEFAULT NULL COMMENT '查询引擎',
  `user_group` varchar(64) DEFAULT NULL COMMENT '用户组',
  `create_by` varchar(32) NOT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(32) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='保存查询表';
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2021-06-25 14:58:09
