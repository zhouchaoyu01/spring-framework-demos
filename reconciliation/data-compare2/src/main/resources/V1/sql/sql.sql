CREATE TABLE `rdc_data_source` (
                                   `id` bigint NOT NULL AUTO_INCREMENT COMMENT '数据源ID',
                                   `name` varchar(100) NOT NULL COMMENT '数据源名称',
                                   `type` varchar(50) NOT NULL COMMENT '数据源类型（MYSQL/HIVE/KAFKA等）',
                                   `connection_params` json NOT NULL COMMENT '连接参数（JSON格式）',
                                   `status` tinyint NOT NULL COMMENT '状态：1-可用；0-不可用',
                                   `remark` text COMMENT '备注',
                                   `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                   `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                   PRIMARY KEY (`id`),
                                   KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据源配置表';


CREATE TABLE `rdc_reconciliation_task` (
                                           `id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务ID',
                                           `name` varchar(100) NOT NULL COMMENT '任务名称',
                                           `type` tinyint NOT NULL COMMENT '任务类型：1-实时；2-批量',
                                           `source_data_source_id` bigint NOT NULL COMMENT '源数据源ID',
                                           `target_data_source_id` bigint NOT NULL COMMENT '目标数据源ID',
                                           `source_table` varchar(100) NOT NULL COMMENT '源表名',
                                           `target_table` varchar(100) NOT NULL COMMENT '目标表名',
                                           `status` tinyint NOT NULL COMMENT '状态：1-运行中；2-停用；3-异常',
                                           `description` text COMMENT '任务描述',
                                           `create_user_id` bigint NOT NULL COMMENT '创建人ID',
                                           `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                           `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                           PRIMARY KEY (`id`),
                                           KEY `idx_status` (`status`),
                                           KEY `idx_type` (`type`),
                                           KEY `idx_source_ds` (`source_data_source_id`),
                                           KEY `idx_target_ds` (`target_data_source_id`),
                                           CONSTRAINT `fk_task_source_ds` FOREIGN KEY (`source_data_source_id`) REFERENCES `rdc_data_source` (`id`),
                                           CONSTRAINT `fk_task_target_ds` FOREIGN KEY (`target_data_source_id`) REFERENCES `rdc_data_source` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对账任务表';

CREATE TABLE `rdc_reconciliation_rule` (
                                           `id` bigint NOT NULL AUTO_INCREMENT COMMENT '规则ID',
                                           `task_id` bigint NOT NULL COMMENT '关联任务ID',
                                           `join_fields` json NOT NULL COMMENT '连接主键（JSON数组）',
                                           `compare_fields` json NOT NULL COMMENT '比对字段（JSON数组）',
                                           `filter_conditions` text COMMENT '过滤条件',
                                           `allow_error` decimal(18,4) DEFAULT '0.0000' COMMENT '允许误差',
                                           `ignore_fields` varchar(512) DEFAULT NULL COMMENT '忽略字段（逗号分隔）',
                                           `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                           `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                           PRIMARY KEY (`id`),
                                           KEY `idx_task_id` (`task_id`),
                                           CONSTRAINT `fk_rule_task` FOREIGN KEY (`task_id`) REFERENCES `rdc_reconciliation_task` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对账规则表';



CREATE TABLE `rdc_schedule` (
                                `id` bigint NOT NULL AUTO_INCREMENT COMMENT '调度ID',
                                `task_id` bigint NOT NULL COMMENT '关联任务ID',
                                `type` tinyint NOT NULL COMMENT '调度类型：1-CRON；2-每日；3-每周',
                                `cron_expression` varchar(100) DEFAULT NULL COMMENT 'Cron表达式（类型1时必填）',
                                `start_time` datetime NOT NULL COMMENT '开始时间',
                                `end_time` datetime DEFAULT NULL COMMENT '结束时间（NULL表示永久）',
                                `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                PRIMARY KEY (`id`),
                                KEY `idx_task_id` (`task_id`),
                                CONSTRAINT `fk_schedule_task` FOREIGN KEY (`task_id`) REFERENCES `rdc_reconciliation_task` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='调度计划表';


CREATE TABLE `rdc_difference_record` (
                                         `id` bigint NOT NULL AUTO_INCREMENT COMMENT '差异ID',
                                         `difference_no` varchar(50) NOT NULL COMMENT '差异编号',
                                         `task_id` bigint NOT NULL COMMENT '关联任务ID',
                                         `type` varchar(50) NOT NULL COMMENT '差异类型',
                                         `source_data_id` varchar(100) DEFAULT NULL COMMENT '源表数据ID',
                                         `target_data_id` varchar(100) DEFAULT NULL COMMENT '目标表数据ID',
                                         `mismatch_field` varchar(100) DEFAULT NULL COMMENT '不匹配字段',
                                         `source_value` text DEFAULT NULL COMMENT '源表字段值',
                                         `target_value` text DEFAULT NULL COMMENT '目标表字段值',
                                         `status` tinyint NOT NULL COMMENT '状态：1-待处理；2-已处理；3-已忽略',
                                         `discovery_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发现时间',
                                         `handle_user_id` bigint DEFAULT NULL COMMENT '处理人ID',
                                         `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
                                         `handle_remark` text DEFAULT NULL COMMENT '处理备注',
                                         PRIMARY KEY (`id`),
                                         UNIQUE KEY `uk_difference_no` (`difference_no`),
                                         KEY `idx_task_id` (`task_id`),
                                         KEY `idx_status` (`status`),
                                         KEY `idx_discovery_time` (`discovery_time`),
                                         CONSTRAINT `fk_diff_task` FOREIGN KEY (`task_id`) REFERENCES `rdc_reconciliation_task` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='差异记录表';


CREATE TABLE `rdc_execution_record` (
                                        `id` bigint NOT NULL AUTO_INCREMENT COMMENT '执行记录ID',
                                        `execution_no` varchar(50) NOT NULL COMMENT '执行编号',
                                        `task_id` bigint NOT NULL COMMENT '关联任务ID',
                                        `type` tinyint NOT NULL COMMENT '执行类型：1-手动；2-定时；3-事件',
                                        `status` tinyint NOT NULL COMMENT '状态：1-处理中；2-成功；3-失败',
                                        `total_count` bigint DEFAULT '0' COMMENT '总比对记录数',
                                        `match_count` bigint DEFAULT '0' COMMENT '匹配成功数',
                                        `difference_count` bigint DEFAULT '0' COMMENT '差异数',
                                        `start_time` datetime NOT NULL COMMENT '开始时间',
                                        `end_time` datetime DEFAULT NULL COMMENT '结束时间',
                                        `cost_ms` bigint DEFAULT '0' COMMENT '耗时（毫秒）',
                                        `error_msg` text DEFAULT NULL COMMENT '错误信息',
                                        `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                        PRIMARY KEY (`id`),
                                        UNIQUE KEY `uk_execution_no` (`execution_no`),
                                        KEY `idx_task_id` (`task_id`),
                                        KEY `idx_status` (`status`),
                                        KEY `idx_start_time` (`start_time`),
                                        CONSTRAINT `fk_exec_task` FOREIGN KEY (`task_id`) REFERENCES `rdc_reconciliation_task` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对账执行记录表';


