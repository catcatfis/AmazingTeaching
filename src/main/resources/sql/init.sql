-- ============================================================
-- AmazingTeaching 教学平台数据库初始化脚本
-- 10张表：核心业务表、权限表、日志表
-- 满足第三范式(3NF)，一对一、一对多、多对多关系正确
-- ============================================================

CREATE DATABASE IF NOT EXISTS amazing_teaching DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE amazing_teaching;

-- ============================================================
-- 1. sys_user 用户表（核心业务表）
-- ============================================================
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '用户ID',
    `username`      VARCHAR(50)     NOT NULL                 COMMENT '用户名',
    `password`      VARCHAR(128)    NOT NULL                 COMMENT '密码（MD5+盐加密）',
    `real_name`     VARCHAR(50)     DEFAULT NULL             COMMENT '真实姓名',
    `email`         VARCHAR(100)    DEFAULT NULL             COMMENT '邮箱',
    `phone`         VARCHAR(20)     DEFAULT NULL             COMMENT '手机号',
    `avatar`        VARCHAR(255)    DEFAULT NULL             COMMENT '头像URL',
    `gender`        TINYINT(1)      DEFAULT 0                COMMENT '性别：0-未知，1-男，2-女',
    `user_type`     TINYINT(1)      NOT NULL DEFAULT 1       COMMENT '用户类型：1-管理员，2-教师，3-学生',
    `status`        TINYINT(1)      NOT NULL DEFAULT 1       COMMENT '状态：0-禁用，1-正常',
    `last_login_time` DATETIME      DEFAULT NULL             COMMENT '最后登录时间',
    `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       TINYINT(1)      NOT NULL DEFAULT 0        COMMENT '逻辑删除：0-正常，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_phone` (`phone`),
    KEY `idx_email` (`email`),
    KEY `idx_user_type` (`user_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================================
-- 2. sys_role 角色表（权限表）
-- ============================================================
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '角色ID',
    `role_name`     VARCHAR(50)     NOT NULL                 COMMENT '角色名称',
    `role_code`     VARCHAR(50)     NOT NULL                 COMMENT '角色编码（如 ROLE_ADMIN, ROLE_TEACHER）',
    `description`   VARCHAR(200)    DEFAULT NULL             COMMENT '角色描述',
    `sort_order`    INT             DEFAULT 0                COMMENT '排序号',
    `status`        TINYINT(1)      NOT NULL DEFAULT 1       COMMENT '状态：0-禁用，1-正常',
    `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- ============================================================
-- 3. sys_user_role 用户角色关联表（多对多：user ↔ role）
-- ============================================================
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    `user_id`       BIGINT          NOT NULL                 COMMENT '用户ID',
    `role_id`       BIGINT          NOT NULL                 COMMENT '角色ID',
    `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
    KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- ============================================================
-- 4. sys_permission 权限表
-- ============================================================
DROP TABLE IF EXISTS `sys_permission`;
CREATE TABLE `sys_permission` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '权限ID',
    `parent_id`     BIGINT          DEFAULT 0                COMMENT '父权限ID（0-顶级）',
    `perm_name`     VARCHAR(50)     NOT NULL                 COMMENT '权限名称',
    `perm_code`     VARCHAR(100)    NOT NULL                 COMMENT '权限编码（如 user:list, course:add）',
    `perm_type`     TINYINT(1)      NOT NULL DEFAULT 1       COMMENT '权限类型：1-菜单，2-按钮，3-接口',
    `path`          VARCHAR(200)    DEFAULT NULL             COMMENT '路由路径',
    `icon`          VARCHAR(50)     DEFAULT NULL             COMMENT '图标',
    `sort_order`    INT             DEFAULT 0                COMMENT '排序号',
    `status`        TINYINT(1)      NOT NULL DEFAULT 1       COMMENT '状态：0-禁用，1-正常',
    `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_perm_code` (`perm_code`),
    KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

-- ============================================================
-- 5. sys_role_permission 角色权限关联表（多对多：role ↔ permission）
-- ============================================================
DROP TABLE IF EXISTS `sys_role_permission`;
CREATE TABLE `sys_role_permission` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    `role_id`       BIGINT          NOT NULL                 COMMENT '角色ID',
    `perm_id`       BIGINT          NOT NULL                 COMMENT '权限ID',
    `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_perm` (`role_id`, `perm_id`),
    KEY `idx_perm_id` (`perm_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

-- ============================================================
-- 6. sys_log 操作日志表（日志表）
-- ============================================================
DROP TABLE IF EXISTS `sys_log`;
CREATE TABLE `sys_log` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '日志ID',
    `user_id`       BIGINT          DEFAULT NULL             COMMENT '操作用户ID',
    `username`      VARCHAR(50)     DEFAULT NULL             COMMENT '操作用户名',
    `module`        VARCHAR(50)     DEFAULT NULL             COMMENT '操作模块',
    `operation`     VARCHAR(100)    DEFAULT NULL             COMMENT '操作类型（如 ADD, UPDATE, DELETE, LOGIN）',
    `description`   VARCHAR(500)    DEFAULT NULL             COMMENT '操作描述',
    `method`        VARCHAR(200)    DEFAULT NULL             COMMENT '请求方法（类名.方法名）',
    `request_url`   VARCHAR(500)    DEFAULT NULL             COMMENT '请求URL',
    `request_method` VARCHAR(10)    DEFAULT NULL             COMMENT '请求方式（GET, POST等）',
    `request_params` TEXT           DEFAULT NULL             COMMENT '请求参数（JSON）',
    `request_ip`    VARCHAR(50)     DEFAULT NULL             COMMENT '请求IP',
    `cost_time`     BIGINT          DEFAULT NULL             COMMENT '耗时（毫秒）',
    `result`        TINYINT(1)      DEFAULT 1                COMMENT '操作结果：0-失败，1-成功',
    `error_msg`     TEXT            DEFAULT NULL             COMMENT '错误信息',
    `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_module` (`module`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- ============================================================
-- 7. course 课程表（核心业务表）
-- ============================================================
DROP TABLE IF EXISTS `course`;
CREATE TABLE `course` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '课程ID',
    `course_name`   VARCHAR(200)    NOT NULL                 COMMENT '课程名称',
    `teacher_id`    BIGINT          NOT NULL                 COMMENT '授课教师ID（一对一：一个教师对应一个课程主体）',
    `category`      VARCHAR(50)     NOT NULL                 COMMENT '课程分类（如 编程、数学、英语）',
    `cover_url`     VARCHAR(500)    DEFAULT NULL             COMMENT '封面图片URL',
    `description`   TEXT            DEFAULT NULL             COMMENT '课程简介',
    `price`         DECIMAL(10,2)   NOT NULL DEFAULT 0.00    COMMENT '课程价格',
    `total_hours`   INT             DEFAULT 0                COMMENT '总课时',
    `student_count` INT             DEFAULT 0                COMMENT '已报名学生数',
    `rating`        DECIMAL(2,1)    DEFAULT 0.0              COMMENT '评分（0-5分）',
    `tags`          VARCHAR(500)    DEFAULT NULL             COMMENT '课程标签（JSON数组）',
    `difficulty`    TINYINT(1)      DEFAULT 1                COMMENT '难度等级：1-入门，2-初级，3-中级，4-高级',
    `status`        TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '状态：0-草稿，1-已发布，2-已下架',
    `publish_time`  DATETIME        DEFAULT NULL             COMMENT '发布时间',
    `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       TINYINT(1)      NOT NULL DEFAULT 0        COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_teacher_id` (`teacher_id`),
    KEY `idx_category` (`category`),
    KEY `idx_status` (`status`),
    FULLTEXT KEY `ft_course_name` (`course_name`, `description`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程表';

-- ============================================================
-- 8. chapter 章节表（一对多：course → chapters）
-- ============================================================
DROP TABLE IF EXISTS `chapter`;
CREATE TABLE `chapter` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '章节ID',
    `course_id`     BIGINT          NOT NULL                 COMMENT '所属课程ID',
    `parent_id`     BIGINT          DEFAULT 0                COMMENT '父章节ID（0-一级章节，支持二级结构）',
    `chapter_name`  VARCHAR(200)    NOT NULL                 COMMENT '章节名称',
    `chapter_type`  TINYINT(1)      NOT NULL DEFAULT 1       COMMENT '章节类型：1-视频，2-文档，3-测验',
    `video_url`     VARCHAR(500)    DEFAULT NULL             COMMENT '视频URL',
    `content`       LONGTEXT        DEFAULT NULL             COMMENT '章节内容（文档/富文本）',
    `duration`      INT             DEFAULT 0                COMMENT '时长/学时（分钟）',
    `sort_order`    INT             NOT NULL DEFAULT 0       COMMENT '排序号',
    `is_free`       TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '是否免费试看：0-否，1-是',
    `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_course_id` (`course_id`),
    KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='章节表';

-- ============================================================
-- 9. student_course 学生选课表（多对多：user ↔ course）
-- ============================================================
DROP TABLE IF EXISTS `student_course`;
CREATE TABLE `student_course` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    `student_id`    BIGINT          NOT NULL                 COMMENT '学生ID（用户表）',
    `course_id`     BIGINT          NOT NULL                 COMMENT '课程ID',
    `progress`      DECIMAL(5,2)    DEFAULT 0.00             COMMENT '学习进度（百分比）',
    `completed_chapters` INT        DEFAULT 0                COMMENT '已完成章节数',
    `enroll_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '报名时间',
    `finish_time`   DATETIME        DEFAULT NULL             COMMENT '完成时间',
    `status`        TINYINT(1)      NOT NULL DEFAULT 1       COMMENT '状态：0-已退课，1-学习中，2-已完成',
    `score`         DECIMAL(5,2)    DEFAULT NULL             COMMENT '课程成绩',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_student_course` (`student_id`, `course_id`),
    KEY `idx_course_id` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生选课表';

-- ============================================================
-- 10. exam 考试/测验表
-- ============================================================
DROP TABLE IF EXISTS `exam`;
CREATE TABLE `exam` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '考试ID',
    `course_id`     BIGINT          DEFAULT NULL             COMMENT '所属课程ID（可为空，表示独立考试）',
    `chapter_id`    BIGINT          DEFAULT NULL             COMMENT '所属章节ID（可为空）',
    `exam_name`     VARCHAR(200)    NOT NULL                 COMMENT '考试名称',
    `exam_type`     TINYINT(1)      NOT NULL DEFAULT 1       COMMENT '考试类型：1-随堂测验，2-章节测试，3-期末考试',
    `questions_json` LONGTEXT       DEFAULT NULL             COMMENT '试题内容（JSON格式，包含题目、选项、答案）',
    `total_score`   INT             NOT NULL DEFAULT 100     COMMENT '试卷总分',
    `pass_score`    INT             NOT NULL DEFAULT 60      COMMENT '及格分数线',
    `duration`      INT             NOT NULL DEFAULT 60      COMMENT '考试时长（分钟）',
    `difficulty`    TINYINT(1)      DEFAULT 1                COMMENT '难度等级：1-简单，2-中等，3-困难',
    `status`        TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '状态：0-未发布，1-进行中，2-已结束',
    `start_time`    DATETIME        DEFAULT NULL             COMMENT '开始时间',
    `end_time`      DATETIME        DEFAULT NULL             COMMENT '结束时间',
    `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_course_id` (`course_id`),
    KEY `idx_chapter_id` (`chapter_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='考试表';

-- ============================================================
-- 11. exam_record 考试记录表（一对多：exam → records，一对一：student → record）
-- ============================================================
DROP TABLE IF EXISTS `exam_record`;
CREATE TABLE `exam_record` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '记录ID',
    `exam_id`       BIGINT          NOT NULL                 COMMENT '考试ID',
    `student_id`    BIGINT          NOT NULL                 COMMENT '学生ID',
    `answers_json`  TEXT            DEFAULT NULL             COMMENT '学生答案（JSON）',
    `score`         DECIMAL(5,2)    DEFAULT NULL             COMMENT '得分',
    `is_passed`     TINYINT(1)      DEFAULT NULL             COMMENT '是否通过：0-未通过，1-通过',
    `start_time`    DATETIME        DEFAULT NULL             COMMENT '开始答题时间',
    `submit_time`   DATETIME        DEFAULT NULL             COMMENT '提交时间',
    `status`        TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '状态：0-未开始，1-进行中，2-已提交',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_exam_student` (`exam_id`, `student_id`),
    KEY `idx_student_id` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='考试记录表';

-- ============================================================
-- 初始化数据
-- ============================================================

-- 角色数据
INSERT INTO `sys_role` (`id`, `role_name`, `role_code`, `description`, `sort_order`) VALUES
(1, '超级管理员', 'ROLE_ADMIN', '系统超级管理员，拥有所有权限', 1),
(2, '教师', 'ROLE_TEACHER', '教师角色，可管理课程和考试', 2),
(3, '学生', 'ROLE_STUDENT', '学生角色，可学习课程和参加考试', 3);

-- 权限数据
INSERT INTO `sys_permission` (`id`, `parent_id`, `perm_name`, `perm_code`, `perm_type`, `path`, `icon`, `sort_order`) VALUES
(1,  0, '系统管理',   'system:manage',     1, '/system',     'Setting',     1),
(2,  1, '用户管理',   'user:list',          1, '/user',       'User',        1),
(3,  2, '添加用户',   'user:add',           2, NULL,          NULL,          1),
(4,  2, '编辑用户',   'user:edit',          2, NULL,          NULL,          2),
(5,  2, '删除用户',   'user:delete',        2, NULL,          NULL,          3),
(6,  1, '角色管理',   'role:list',          1, '/role',       'UserFilled',  2),
(7,  6, '添加角色',   'role:add',           2, NULL,          NULL,          1),
(8,  6, '编辑角色',   'role:edit',          2, NULL,          NULL,          2),
(9,  6, '删除角色',   'role:delete',        2, NULL,          NULL,          3),
(10, 1, '权限管理',   'perm:list',          1, '/permission', 'Lock',        3),
(11, 0, '课程管理',   'course:manage',      1, '/course',     'Reading',     2),
(12,11, '课程列表',   'course:list',        1, '/course/list','List',        1),
(13,11, '添加课程',   'course:add',         2, NULL,          NULL,          2),
(14,11, '编辑课程',   'course:edit',        2, NULL,          NULL,          3),
(15,11, '删除课程',   'course:delete',      2, NULL,          NULL,          4),
(16, 0, '考试管理',   'exam:manage',        1, '/exam',       'EditPen',     3),
(17,16, '考试列表',   'exam:list',          1, '/exam/list',  'List',        1),
(18,16, '创建考试',   'exam:add',           2, NULL,          NULL,          2),
(19, 0, '数据分析',   'data:manage',        1, '/dashboard',  'DataAnalysis',4),
(20,19, '数据看板',   'data:dashboard',     1, '/dashboard',  'PieChart',    1),
(21, 0, 'AI助手',     'ai:assistant',       1, '/ai',         'MagicStick',  5),
(22, 0, '系统日志',   'log:list',           1, '/log',        'Document',    6);

-- 角色权限关联（管理员拥有所有权限）
INSERT INTO `sys_role_permission` (`role_id`, `perm_id`) VALUES
(1,1),(1,2),(1,3),(1,4),(1,5),(1,6),(1,7),(1,8),(1,9),(1,10),
(1,11),(1,12),(1,13),(1,14),(1,15),(1,16),(1,17),(1,18),
(1,19),(1,20),(1,21),(1,22);

-- 教师权限
INSERT INTO `sys_role_permission` (`role_id`, `perm_id`) VALUES
(2,11),(2,12),(2,13),(2,14),(2,16),(2,17),(2,18),(2,19),(2,20),(2,21);

-- 学生权限
INSERT INTO `sys_role_permission` (`role_id`, `perm_id`) VALUES
(3,12),(3,17),(3,19),(3,20),(3,21);

-- 管理员用户（密码：admin123，MD5+盐加密）
INSERT INTO `sys_user` (`id`, `username`, `password`, `real_name`, `email`, `phone`, `user_type`, `status`) VALUES
(1, 'admin',  'd967be9a91e54919027a02b8c5a8021c', '系统管理员', 'admin@example.com', '13800000001', 1, 1);

-- 教师用户
INSERT INTO `sys_user` (`id`, `username`, `password`, `real_name`, `email`, `phone`, `user_type`, `status`) VALUES
(2, 'teacher1', 'd967be9a91e54919027a02b8c5a8021c', '张老师', 'teacher1@example.com', '13800000002', 2, 1),
(3, 'teacher2', 'd967be9a91e54919027a02b8c5a8021c', '李老师', 'teacher2@example.com', '13800000003', 2, 1);

-- 学生用户
INSERT INTO `sys_user` (`id`, `username`, `password`, `real_name`, `email`, `phone`, `user_type`, `status`) VALUES
(4, 'student1', 'd967be9a91e54919027a02b8c5a8021c', '王同学', 'student1@example.com', '13800000004', 3, 1),
(5, 'student2', 'd967be9a91e54919027a02b8c5a8021c', '赵同学', 'student2@example.com', '13800000005', 3, 1),
(6, 'student3', 'd967be9a91e54919027a02b8c5a8021c', '陈同学', 'student3@example.com', '13800000006', 3, 1);

-- 用户角色关联
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES
(1, 1),
(2, 2),
(3, 2),
(4, 3),
(5, 3),
(6, 3);

-- 课程数据
INSERT INTO `course` (`id`, `course_name`, `teacher_id`, `category`, `description`, `price`, `total_hours`, `student_count`, `rating`, `tags`, `difficulty`, `status`, `publish_time`) VALUES
(1, 'Java从入门到精通', 2, '编程开发', '全面系统的Java编程课程，涵盖Java基础、面向对象、集合框架、多线程、网络编程等核心内容。', 299.00, 80, 156, 4.8, '["Java","编程","后端开发"]', 2, 1, '2025-01-15 00:00:00'),
(2, 'Spring Boot 3.x 实战', 2, '编程开发', '深入学习Spring Boot 3.x框架，掌握企业级应用开发的核心技能，包括自动配置、数据访问、安全、微服务等。', 399.00, 60, 98, 4.6, '["SpringBoot","微服务","企业级"]', 3, 1, '2025-02-20 00:00:00'),
(3, 'Vue3 + Element Plus 前端开发', 3, '前端开发', '从零开始学习Vue3框架，结合Element Plus组件库，打造现代化前端应用。', 259.00, 50, 132, 4.7, '["Vue3","前端","ElementPlus"]', 2, 1, '2025-03-10 00:00:00'),
(4, '数据结构与算法精讲', 3, '计算机基础', '系统学习常用数据结构和经典算法，提升编程思维和面试竞争力。', 359.00, 70, 85, 4.9, '["算法","数据结构","面试"]', 4, 1, '2025-01-01 00:00:00'),
(5, 'Python数据分析实战', 2, '数据科学', '使用Python进行数据分析、可视化和机器学习入门，掌握Pandas、NumPy、Matplotlib等工具。', 329.00, 55, 110, 4.5, '["Python","数据分析","机器学习"]', 2, 1, '2025-04-01 00:00:00'),
(6, 'MySQL数据库深度解析', 3, '数据库', '深入理解MySQL数据库原理，掌握SQL优化、索引设计、事务管理、主从复制等高级特性。', 279.00, 45, 72, 4.4, '["MySQL","数据库","SQL优化"]', 3, 1, '2025-05-01 00:00:00');

-- 章节数据（课程1）
INSERT INTO `chapter` (`id`, `course_id`, `parent_id`, `chapter_name`, `chapter_type`, `duration`, `sort_order`, `is_free`) VALUES
(1,  1, 0, '第一章 Java语言基础', 1, 120, 1, 1),
(2,  1, 1, '1.1 Java发展史与开发环境搭建', 2, 30, 1, 1),
(3,  1, 1, '1.2 基本数据类型与运算符', 2, 45, 2, 0),
(4,  1, 1, '1.3 流程控制语句', 2, 45, 3, 0),
(5,  1, 0, '第二章 面向对象编程', 1, 180, 2, 0),
(6,  1, 5, '2.1 类与对象', 2, 60, 1, 0),
(7,  1, 5, '2.2 继承与多态', 2, 60, 2, 0),
(8,  1, 5, '2.3 接口与抽象类', 2, 60, 3, 0),
(9,  1, 0, '第三章 集合框架', 1, 150, 3, 0),
(10, 1, 9, '3.1 List集合详解', 2, 50, 1, 0),
(11, 1, 9, '3.2 Map与Set', 2, 50, 2, 0),
(12, 1, 9, '3.3 Stream流操作', 2, 50, 3, 0);

-- 学生选课数据
INSERT INTO `student_course` (`student_id`, `course_id`, `progress`, `completed_chapters`, `enroll_time`, `status`) VALUES
(4, 1, 75.00, 9,  '2025-01-20 00:00:00', 1),
(4, 2, 30.00, 4,  '2025-03-01 00:00:00', 1),
(4, 5, 10.00, 1,  '2025-05-15 00:00:00', 1),
(5, 1, 50.00, 6,  '2025-02-01 00:00:00', 1),
(5, 3, 90.00, 12, '2025-03-15 00:00:00', 1),
(5, 4, 100.00, 15,'2025-01-15 00:00:00', 2),
(6, 2, 60.00, 8,  '2025-04-01 00:00:00', 1),
(6, 3, 20.00, 3,  '2025-05-01 00:00:00', 1),
(6, 6, 45.00, 6,  '2025-06-01 00:00:00', 1);

-- 考试数据
INSERT INTO `exam` (`id`, `course_id`, `exam_name`, `exam_type`, `total_score`, `pass_score`, `duration`, `difficulty`, `status`, `start_time`, `end_time`) VALUES
(1, 1, 'Java基础阶段测试', 2, 100, 60, 90, 1, 1, '2025-06-01 00:00:00', '2025-12-31 23:59:59'),
(2, 1, 'Java期末综合考试', 3, 100, 60, 120, 2, 1, '2025-06-15 00:00:00', '2025-12-31 23:59:59'),
(3, 2, 'Spring Boot 阶段测试', 2, 100, 60, 90, 2, 1, '2025-06-01 00:00:00', '2025-12-31 23:59:59');

-- 考试记录
INSERT INTO `exam_record` (`exam_id`, `student_id`, `score`, `is_passed`, `start_time`, `submit_time`, `status`) VALUES
(1, 4, 85.00, 1, '2025-06-10 09:00:00', '2025-06-10 10:30:00', 2),
(1, 5, 72.00, 1, '2025-06-11 14:00:00', '2025-06-11 15:30:00', 2),
(2, 5, 58.00, 0, '2025-06-20 10:00:00', '2025-06-20 12:00:00', 2),
(3, 6, 91.00, 1, '2025-06-12 08:00:00', '2025-06-12 09:30:00', 2);
