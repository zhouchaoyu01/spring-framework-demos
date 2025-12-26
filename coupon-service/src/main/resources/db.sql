-- 创建优惠券主表
CREATE TABLE `coupon` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `coupon_id` varchar(64) NOT NULL COMMENT '优惠券ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `coupon_code` varchar(32) NOT NULL COMMENT '优惠券码',
  `coupon_type` varchar(20) NOT NULL COMMENT '优惠券类型：CASH-现金券, DISCOUNT-折扣券',
  `amount` decimal(10,2) NOT NULL COMMENT '金额/折扣',
  `min_amount` decimal(10,2) DEFAULT NULL COMMENT '最低消费金额',
  `status` varchar(20) NOT NULL COMMENT '状态：UNUSED-未使用, USED-已使用, EXPIRED-已过期, INVALID-已失效',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime NOT NULL COMMENT '结束时间',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_coupon_id` (`coupon_id`),
  UNIQUE KEY `uk_coupon_code` (`coupon_code`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_end_time` (`end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券主表';

-- 创建优惠券使用记录表
CREATE TABLE `coupon_usage_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `record_id` varchar(64) NOT NULL COMMENT '记录ID',
  `coupon_id` varchar(64) NOT NULL COMMENT '优惠券ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `order_id` varchar(64) NOT NULL COMMENT '订单ID',
  `usage_type` varchar(20) NOT NULL COMMENT '使用类型：VERIFY-核销, ROLLBACK-冲正',
  `usage_time` datetime NOT NULL COMMENT '使用时间',
  `remark` varchar(200) DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_record_id` (`record_id`),
  UNIQUE KEY `uk_coupon_order` (`coupon_id`,`order_id`),
  KEY `idx_coupon_id` (`coupon_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_usage_type` (`usage_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券使用记录表';

-- 创建优惠券防重表
CREATE TABLE `coupon_request_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `request_id` varchar(64) NOT NULL COMMENT '请求ID',
  `coupon_id` varchar(64) NOT NULL COMMENT '优惠券ID',
  `order_id` varchar(64) NOT NULL COMMENT '订单ID',
  `operation_type` varchar(20) NOT NULL COMMENT '操作类型：VERIFY-核销, ROLLBACK-冲正',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_request_coupon_order` (`request_id`,`coupon_id`,`order_id`),
  KEY `idx_coupon_id` (`coupon_id`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券请求防重记录表';