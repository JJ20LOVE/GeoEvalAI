-- geollm_clean.sql (整理版)
-- MySQL 8+
DROP DATABASE IF EXISTS geollm;
CREATE DATABASE IF NOT EXISTS `geollm`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci
  DEFAULT ENCRYPTION='N';

USE `geollm`;

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `recommendation_feedback`;
DROP TABLE IF EXISTS `wrongbook`;
DROP TABLE IF EXISTS `ocr`;
DROP TABLE IF EXISTS `answersheet_detail`;
DROP TABLE IF EXISTS `exam_detail`;
DROP TABLE IF EXISTS `answersheet`;
DROP TABLE IF EXISTS `exam_answer`;
DROP TABLE IF EXISTS `exam_question`;
DROP TABLE IF EXISTS `exam`;
DROP TABLE IF EXISTS `student`;
DROP TABLE IF EXISTS `user`;
DROP TABLE IF EXISTS `class`;
DROP TABLE IF EXISTS `exam_ocr_roi`;

SET FOREIGN_KEY_CHECKS = 1;

-- ----------------------------
-- Table: class
-- ----------------------------
CREATE TABLE `class` (
  `class_id` int NOT NULL AUTO_INCREMENT,
  `class_name` varchar(255) NOT NULL,
  PRIMARY KEY (`class_id`),
  UNIQUE KEY `class_name` (`class_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table: user
-- ----------------------------
CREATE TABLE `user` (
  `user_id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `email` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table: student
-- ----------------------------
CREATE TABLE `student` (
  `id` int NOT NULL AUTO_INCREMENT,
  `student_id` varchar(255) NOT NULL,
  `student_name` varchar(255) NOT NULL,
  `class_id` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `student_id` (`student_id`),
  CONSTRAINT `student_ibfk_1`
    FOREIGN KEY (`class_id`) REFERENCES `class` (`class_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table: exam
-- ----------------------------
CREATE TABLE `exam` (
  `exam_id` int NOT NULL AUTO_INCREMENT,
  `title` varchar(255) NOT NULL COMMENT '试卷标题',
  `create_date` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `creater` int DEFAULT NULL,
  `type` int DEFAULT NULL,
  `qnumber` int DEFAULT '0',
  PRIMARY KEY (`exam_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table: answersheet
-- ----------------------------
CREATE TABLE `answersheet` (
  `id` int NOT NULL AUTO_INCREMENT,
  `student_id` int NOT NULL,
  `exam_id` int NOT NULL,
  `total_grade` int DEFAULT '0',
  `is_eva` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `exam_id` (`exam_id`),
  KEY `student_id` (`student_id`),
  CONSTRAINT `answersheet_ibfk_2`
    FOREIGN KEY (`exam_id`) REFERENCES `exam` (`exam_id`) ON DELETE CASCADE,
  CONSTRAINT `answersheet_ibfk_3`
    FOREIGN KEY (`student_id`) REFERENCES `student` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='答题卡表';

-- ----------------------------
-- Table: exam_question
-- ----------------------------
CREATE TABLE `exam_question` (
  `exam_id` int NOT NULL,
  `question` text NOT NULL,
  PRIMARY KEY (`exam_id`),
  CONSTRAINT `exam_question_ibfk_1`
    FOREIGN KEY (`exam_id`) REFERENCES `exam` (`exam_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table: exam_answer
-- ----------------------------
CREATE TABLE `exam_answer` (
  `exam_id` int NOT NULL,
  `answer` text NOT NULL,
  PRIMARY KEY (`exam_id`),
  CONSTRAINT `exam_answer_ibfk_1`
    FOREIGN KEY (`exam_id`) REFERENCES `exam` (`exam_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table: exam_detail
-- ----------------------------
CREATE TABLE `exam_detail` (
  `id` int NOT NULL AUTO_INCREMENT,
  `exam_id` int NOT NULL,
  `qid` int NOT NULL,
  `point` int DEFAULT '0',
  `tihao` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `exam_id` (`exam_id`),
  CONSTRAINT `exam_detail_ibfk_1`
    FOREIGN KEY (`exam_id`) REFERENCES `exam` (`exam_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table: answersheet_detail
-- ----------------------------
CREATE TABLE `answersheet_detail` (
  `aid` int NOT NULL,
  `qid` int NOT NULL,
  `result` text,
  `point` int DEFAULT '0',
  `comment` text,
  `structure` int DEFAULT NULL,
  PRIMARY KEY (`aid`, `qid`),
  CONSTRAINT `answersheet_detail_ibfk_1`
    FOREIGN KEY (`aid`) REFERENCES `answersheet` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table: ocr
-- ----------------------------
CREATE TABLE `ocr` (
  `aid` int NOT NULL,
  `qid` int NOT NULL,
  `result` text,
  PRIMARY KEY (`aid`, `qid`),
  CONSTRAINT `ocr_ibfk_1`
    FOREIGN KEY (`aid`) REFERENCES `answersheet` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='OCR识别结果表';

-- ----------------------------
-- Table: wrongbook
-- ----------------------------
CREATE TABLE `wrongbook` (
  `wrong_id` int NOT NULL AUTO_INCREMENT,
  `student_id` int NOT NULL,
  `exam_id` int NOT NULL,
  `question_id` int NOT NULL,
  `question_text` text NOT NULL,
  `student_answer` text NOT NULL,
  `correct_answer` text NOT NULL,
  `analysis` text,
  `knowledge_point` varchar(255) DEFAULT NULL,
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`wrong_id`),
  UNIQUE KEY `unique_wrong_record` (`student_id`, `exam_id`, `question_id`),
  KEY `exam_id` (`exam_id`),
  CONSTRAINT `wrongbook_ibfk_2`
    FOREIGN KEY (`exam_id`) REFERENCES `exam` (`exam_id`) ON DELETE CASCADE,
  CONSTRAINT `wrongbook_ibfk_3`
    FOREIGN KEY (`student_id`) REFERENCES `student` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='错题本表';

-- ----------------------------
-- Table: recommendation_feedback
-- ----------------------------
CREATE TABLE `recommendation_feedback` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '反馈主键ID',
  `student_id` int NOT NULL COMMENT '反馈学生ID',
  `wrong_id` int NOT NULL COMMENT '关联错题ID（wrongbook.wrong_id）',
  `question_id` int NOT NULL COMMENT '推荐题编号（当前为返回结果中的编号）',
  `feedback` varchar(50) NOT NULL COMMENT '反馈内容（如有用/没用）',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '反馈创建时间',
  PRIMARY KEY (`id`),
  KEY `student_id` (`student_id`),
  KEY `wrong_id` (`wrong_id`),
  CONSTRAINT `recommendation_feedback_ibfk_1`
    FOREIGN KEY (`student_id`) REFERENCES `student` (`id`) ON DELETE CASCADE,
  CONSTRAINT `recommendation_feedback_ibfk_2`
    FOREIGN KEY (`wrong_id`) REFERENCES `wrongbook` (`wrong_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='推荐反馈表';

-- ----------------------------
-- Table: recommendation_batch
-- ----------------------------
CREATE TABLE `recommendation_batch` (
  `batch_id` bigint NOT NULL AUTO_INCREMENT COMMENT '推荐批次ID（一次推荐请求）',
  `student_id` int NOT NULL COMMENT '发起推荐的学生ID',
  `wrong_id` int NOT NULL COMMENT '关联错题ID（wrongbook.wrong_id）',
  `source_question_text` text COMMENT '推荐时使用的原题题干快照',
  `source_knowledge_point` varchar(255) DEFAULT NULL COMMENT '推荐时使用的知识点快照',
  `model_name` varchar(100) DEFAULT NULL COMMENT '生成该批次的模型名称',
  `request_limit` int NOT NULL COMMENT '本次请求期望返回题目数量',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '批次创建时间',
  PRIMARY KEY (`batch_id`),
  KEY `idx_rb_student_wrong` (`student_id`, `wrong_id`),
  KEY `idx_rb_wrong` (`wrong_id`),
  CONSTRAINT `recommendation_batch_ibfk_1`
    FOREIGN KEY (`student_id`) REFERENCES `student` (`id`) ON DELETE CASCADE,
  CONSTRAINT `recommendation_batch_ibfk_2`
    FOREIGN KEY (`wrong_id`) REFERENCES `wrongbook` (`wrong_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='推荐批次表（一次请求生成多道推荐题）';

-- ----------------------------
-- Table: recommendation_item
-- ----------------------------
CREATE TABLE `recommendation_item` (
  `item_id` bigint NOT NULL AUTO_INCREMENT COMMENT '推荐题主键ID',
  `batch_id` bigint NOT NULL COMMENT '所属推荐批次ID（recommendation_batch.batch_id）',
  `item_no` int NOT NULL COMMENT '批次内序号（从1开始）',
  `question_text` text NOT NULL COMMENT '推荐题题干',
  `knowledge_point` varchar(255) DEFAULT NULL COMMENT '推荐题知识点',
  `difficulty` varchar(20) DEFAULT NULL COMMENT '难度（简单/中等/困难）',
  `candidate_ref` text COMMENT '候选题来源信息（可选）',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '题项创建时间',
  PRIMARY KEY (`item_id`),
  UNIQUE KEY `uk_batch_itemno` (`batch_id`, `item_no`),
  KEY `idx_ri_batch` (`batch_id`),
  CONSTRAINT `recommendation_item_ibfk_1`
    FOREIGN KEY (`batch_id`) REFERENCES `recommendation_batch` (`batch_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='推荐题明细表（每个批次的具体题目）';

CREATE TABLE `exam_ocr_roi` (
                                `exam_id` INT NOT NULL,
                                `qid` INT NOT NULL,
                                `page_index` INT NOT NULL,
                                `x1` DOUBLE NOT NULL,
                                `y1` DOUBLE NOT NULL,
                                `x2` DOUBLE NOT NULL,
                                `y2` DOUBLE NOT NULL,
                                PRIMARY KEY (`exam_id`, `qid`, `page_index`),
                                CONSTRAINT `exam_ocr_roi_ibfk_1`
                                    FOREIGN KEY (`exam_id`) REFERENCES `exam` (`exam_id`)
                                        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Seed data (来自原 geollm_clean.sql)
-- ----------------------------
INSERT INTO `user` (`user_id`, `username`, `password`, `email`) VALUES
  (1, 'admin', '1234567mm', '123@qq.com'),
  (2, 'dingding', 'n0WY5Kk8r+T02g==', '123@qq.com'),
  (3, 'test', 'n0WY5Kk8r+T02g==', '111@dnidd.com');

INSERT INTO `class` (`class_id`, `class_name`) VALUES
  (1, '高一1班');

INSERT INTO `student` (`id`, `student_id`, `student_name`, `class_id`) VALUES
  (1, '12333333333', '张三', 1),
  (2, '10225101537', '李四', 1),
  (3, '10245101480', '王五', 1),
  (4, '10245101481', '赵六', 1),
  (5, '10245101482', '钱七', 1);

INSERT INTO `exam` (`exam_id`, `title`, `create_date`, `creater`, `type`, `qnumber`) VALUES
  (1, '地理考试1', '2025-10-13 18:18:56', 1, 1, 2),
  (14, '地理考试2', '2025-10-23 20:22:15', 1, 0, 10);

INSERT INTO `answersheet` (`id`, `student_id`, `exam_id`, `total_grade`, `is_eva`) VALUES
  (1, 1, 1, 80, 0),
  (31, 3, 14, 0, 0);

