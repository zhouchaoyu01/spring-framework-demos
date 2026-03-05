INSERT INTO recon.rdc_reconciliation_difference_record (execution_record_id,diff_type,join_key_value,mismatch_field,source_value,target_value,task_id,description,create_time) VALUES
	 (1,'MATCH','LOCAL_1','','','',0,'记录匹配','2025-11-25 15:19:48'),
	 (1,'MATCH','LOCAL_0','','','',0,'记录匹配','2025-11-25 15:19:48'),
	 (1,'FIELD_MISMATCH','LOCAL_2','amount<->amount','amount=868.85;','amount=55.00;',0,'字段不匹配','2025-11-25 15:19:48'),
	 (1,'TARGET_ONLY','TTLL','','','',0,'目标表有记录，源表无记录','2025-11-25 15:19:48'),
	 (1,'SOURCE_ONLY','TTL1','','','',0,'源表有记录，目标表无记录','2025-11-25 15:19:48');
