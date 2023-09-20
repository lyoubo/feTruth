/*
 Navicat Premium Data Transfer

 Source Server         : local
 Source Server Type    : MySQL
 Source Server Version : 80026
 Source Host           : localhost:3306
 Source Schema         : fse2023

 Target Server Type    : MySQL
 Target Server Version : 80026
 File Encoding         : 65001

 Date: 29/06/2023 14:15:29
*/

SET NAMES utf8mb4;
SET
FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for decision_feature
-- ----------------------------
DROP TABLE IF EXISTS `decision_feature`;
CREATE TABLE `decision_feature`
(
    `id`                      int NOT NULL AUTO_INCREMENT,
    `source_invocation_nums`  int NOT NULL,
    `target_invocation_nums`  int NOT NULL,
    `matched_invocation_nums` int NOT NULL,
    `source_code_elements`    int NOT NULL,
    `target_code_elements`    int NOT NULL,
    `matched_code_elements`   int NOT NULL,
    `refactoring_id`          int NOT NULL,
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 43809 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

SET
FOREIGN_KEY_CHECKS = 1;
