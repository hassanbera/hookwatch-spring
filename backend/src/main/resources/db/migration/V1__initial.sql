CREATE TABLE endpoints (id uuid PRIMARY KEY, created_at timestamptz NOT NULL,
 name varchar(100) NOT NULL,
 url varchar(500) NOT NULL);
CREATE TABLE deliveries (id uuid PRIMARY KEY, created_at timestamptz NOT NULL,
 endpoint_id uuid NOT NULL,
 event_type varchar(80) NOT NULL,
 payload text NOT NULL,
 status varchar(20) NOT NULL,
 attempt_count integer NOT NULL,
 cycle_attempts integer NOT NULL,
 next_attempt_at timestamptz NOT NULL);
CREATE TABLE attempts (id uuid PRIMARY KEY, created_at timestamptz NOT NULL,
 delivery_id uuid NOT NULL,
 number integer NOT NULL,
 status_code integer,
 duration_ms integer NOT NULL,
 error varchar(500) NOT NULL);
ALTER TABLE deliveries ADD FOREIGN KEY (endpoint_id) REFERENCES endpoints(id);
ALTER TABLE attempts ADD FOREIGN KEY (delivery_id) REFERENCES deliveries(id);
ALTER TABLE attempts ADD UNIQUE(delivery_id,number);
CREATE INDEX ON deliveries(status,next_attempt_at);
ALTER TABLE deliveries ADD CHECK(status IN ('QUEUED','DELIVERED','FAILED'));
