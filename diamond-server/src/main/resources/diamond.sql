/*
Navicat MySQL Data Transfer
*/
CREATE DATABASE diamond;

USE diamond;

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for config_info
-- ----------------------------
DROP TABLE IF EXISTS config_info;
CREATE TABLE config_info (
  id bigint(64) NOT NULL auto_increment,
  data_id varchar(255) NOT NULL DEFAULT ' ',
  group_id varchar(128) NOT NULL DEFAULT ' ',
  content longtext NOT NULL,
  md5 varchar(32) NOT NULL DEFAULT ' ',
  gmt_create datetime NOT NULL DEFAULT '2010-05-05 00:00:00',
  gmt_modified datetime NOT NULL DEFAULT '2010-05-05 00:00:00',
  PRIMARY KEY (id),
  UNIQUE KEY uk_config_datagroup (data_id,group_id)
);
