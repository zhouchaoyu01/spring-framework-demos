-- 创建订单主表
CREATE TABLE `order_main` (
  `order_id` varchar(64) NOT NULL COMMENT '订单ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `coupon_id` varchar(64) DEFAULT NULL COMMENT '优惠券ID',
  `status` varchar(20) NOT NULL COMMENT '订单状态：CREATED-创建成功, FAILED-创建失败, CLOSED-已关闭',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`order_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_coupon_id` (`coupon_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单主表';

-- 创建订单-优惠券核销记录表
CREATE TABLE `order_coupon_verify` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_id` varchar(64) NOT NULL COMMENT '订单ID',
  `coupon_id` varchar(64) NOT NULL COMMENT '优惠券ID',
  `verify_status` varchar(20) NOT NULL COMMENT '核销状态：INIT-待核销, SUCCESS-核销成功, FAILED-核销失败, ROLLBACKED-已冲正',
  `verify_time` datetime DEFAULT NULL COMMENT '核销时间',
  `rollback_time` datetime DEFAULT NULL COMMENT '冲正时间',
  `ext_msg` varchar(500) DEFAULT NULL COMMENT '外部接口返回信息',
  `unique_key` varchar(128) NOT NULL COMMENT '唯一键：coupon_id+order_id',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_unique_key` (`unique_key`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_coupon_id` (`coupon_id`),
  KEY `idx_verify_status` (`verify_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单-优惠券核销记录表';

-- 创建防重表
CREATE TABLE `request_id_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `request_id` varchar(64) NOT NULL COMMENT '请求ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `biz_type` varchar(32) NOT NULL COMMENT '业务类型',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_request_user_biz` (`request_id`,`user_id`,`biz_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='请求防重记录表';