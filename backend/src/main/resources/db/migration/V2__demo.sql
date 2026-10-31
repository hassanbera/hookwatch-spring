INSERT INTO endpoints(id,created_at,name,url) VALUES
 ('10000000-0000-0000-0000-000000000001',now(),'Yerel demo alıcısı','http://127.0.0.1:4205/api/demo/receiver');
INSERT INTO deliveries(id,created_at,endpoint_id,event_type,payload,status,attempt_count,cycle_attempts,next_attempt_at) VALUES
 ('10000000-0000-0000-0000-000000000002',now(),'10000000-0000-0000-0000-000000000001','order.created','{"orderId":"ORD-1042","total":2490}','QUEUED',0,0,now());
