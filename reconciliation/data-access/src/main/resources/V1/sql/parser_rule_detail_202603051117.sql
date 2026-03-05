INSERT INTO recon.parser_rule_detail (parser_rule_id,source_col_index,source_col_name,target_field,data_type,format_expr,unit,default_value,enum_mapping,is_required) VALUES
	 ('WX_CLEARING_EXCEL',0,'交易时间','trade_time','date','yyyy-MM-dd HH:mm:ss',NULL,NULL,NULL,1),
	 ('WX_CLEARING_EXCEL',6,'商户订单号','order_id','string',NULL,NULL,NULL,NULL,1),
	 ('WX_CLEARING_EXCEL',5,'微信订单号','channel_txn_id','string',NULL,NULL,NULL,NULL,1),
	 ('WX_CLEARING_EXCEL',12,'应结订单金额','amount','decimal',NULL,'元',NULL,NULL,1),
	 ('WX_CLEARING_EXCEL',22,'手续费','fee','decimal',NULL,'元',NULL,NULL,0),
	 ('WX_CLEARING_EXCEL',9,'交易状态','status','enum',NULL,NULL,NULL,'{"退款": "REFUND", "支付成功": "SUCCESS"}',1),
	 ('WX_CLEARING_EXCEL',8,'交易类型','biz_type','string',NULL,NULL,NULL,NULL,0);
