-- 创建操作日志表
DROP TABLE IF EXISTS `sys_operation_log`;
CREATE TABLE `sys_operation_log` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `operator` varchar(64) NOT NULL COMMENT '操作人',
    `module` varchar(64) NOT NULL COMMENT '所属模块',
    `action` varchar(255) NOT NULL COMMENT '操作内容',
    `ip_address` varchar(64) DEFAULT NULL COMMENT '操作IP',
    `status` varchar(20) DEFAULT '成功' COMMENT '状态: 成功/失败',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_operator_time` (`operator`, `create_time`),
    KEY `idx_module_time` (`module`, `create_time`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统操作日志表';
