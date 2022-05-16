-- 1. 示例1 基于事件时间的滚动窗口
CREATE TABLE user_behavior (
  uid BIGINT COMMENT '用户Id',
  pid BIGINT COMMENT '商品Id',
  cid BIGINT COMMENT '商品类目Id',
  type STRING COMMENT '行为类型',
  ts BIGINT COMMENT '行为时间',
  ts_ltz AS TO_TIMESTAMP_LTZ(ts, 3), -- 事件时间
  WATERMARK FOR ts_ltz AS ts_ltz - INTERVAL '1' MINUTE -- 在 ts_ltz 上定义watermark，ts_ltz 成为事件时间列
) WITH (
  'connector' = 'kafka',
  'topic' = 'user_behavior',
  'properties.bootstrap.servers' = 'localhost:9092',
  'properties.group.id' = 'user_behavior',
  'scan.startup.mode' = 'latest-offset',
  'format' = 'json',
  'json.ignore-parse-errors' = 'false',
  'json.fail-on-missing-field' = 'true'
)

-- CREATE TABLE user_behavior (
--   uid BIGINT COMMENT '用户Id',
--   pid BIGINT COMMENT '商品Id',
--   cid BIGINT COMMENT '商品类目Id',
--   `type` STRING COMMENT '行为类型',
--   ts BIGINT COMMENT '行为时间戳',
--   tm STRING COMMENT '行为时间',
--   ts_ltz AS TO_TIMESTAMP_LTZ(ts, 3), -- 事件时间
--   WATERMARK FOR ts_ltz AS ts_ltz - INTERVAL '1' MINUTE
-- )
-- WITH (
--   'connector' = 'filesystem',
--   'path' = 'file:///opt/data/user_behavior.csv',
--   'format' = 'csv'
-- )

CREATE TABLE user_behavior_cnt (
  window_start STRING COMMENT '窗口开始时间',
  window_end STRING COMMENT '窗口结束时间',
  window_start_timestamp TIMESTAMP(3) COMMENT '窗口开始时间',
  window_end_timestamp TIMESTAMP(3) COMMENT '窗口结束时间',
  cnt BIGINT COMMENT '次数'
) WITH (
  'connector' = 'print',
  'print-identifier' = 'ET'
)

INSERT INTO user_behavior_cnt
SELECT
  DATE_FORMAT(TUMBLE_START(ts_ltz, INTERVAL '1' HOUR), 'yyyy-MM-dd HH:mm:ss') AS window_start,
  DATE_FORMAT(TUMBLE_END(ts_ltz, INTERVAL '1' HOUR), 'yyyy-MM-dd HH:mm:ss') AS window_end,
  TUMBLE_START(ts_ltz, INTERVAL '1' HOUR) AS window_start_timestamp,
  TUMBLE_END(ts_ltz, INTERVAL '1' HOUR) AS window_end_timestamp,
  COUNT(*) AS cnt
FROM user_behavior
GROUP BY TUMBLE(ts_ltz, INTERVAL '1' HOUR)


--  示例2 基于处理时间的滚动窗口
CREATE TABLE user_behavior_process_time (
  uid BIGINT COMMENT '用户Id',
  pid BIGINT COMMENT '商品Id',
  cid BIGINT COMMENT '商品类目Id',
  type STRING COMMENT '行为类型',
  process_time AS PROCTIME() -- 处理时间
) WITH (
  'connector' = 'kafka',
  'topic' = 'user_behavior',
  'properties.bootstrap.servers' = 'localhost:9092',
  'properties.group.id' = 'group-window-tumble',
  'scan.startup.mode' = 'earliest-offset',
  'format' = 'json',
  'json.ignore-parse-errors' = 'false',
  'json.fail-on-missing-field' = 'true'
)

CREATE TABLE user_behavior_process_time_uv (
  window_start TIMESTAMP(3) COMMENT '窗口开始时间',
  window_end TIMESTAMP(3) COMMENT '窗口结束时间',
  uv BIGINT COMMENT '用户数'
) WITH (
  'connector' = 'print',
  'print-identifier' = 'PT',
  'sink.parallelism' = '1'
)

INSERT INTO user_behavior_process_time_uv
SELECT
  TUMBLE_START(process_time, INTERVAL '1' HOUR) AS window_start,
  TUMBLE_END(process_time, INTERVAL '1' HOUR) AS window_end,
  COUNT(DISTINCT uid) AS uv
FROM user_behavior_process_time
GROUP BY TUMBLE(process_time, INTERVAL '1' HOUR)