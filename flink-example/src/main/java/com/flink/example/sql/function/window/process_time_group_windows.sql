--  示例2 基于处理时间的滚动窗口
CREATE TABLE user_behavior (
  uid BIGINT COMMENT '用户Id',
  pid BIGINT COMMENT '商品Id',
  cid BIGINT COMMENT '商品类目Id',
  type STRING COMMENT '行为类型',
  ts BIGINT COMMENT '行为时间',
  `time` STRING COMMENT '行为时间',
  process_time AS PROCTIME() -- 处理时间
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

CREATE TABLE user_behavior_cnt (
  window_start STRING COMMENT '窗口开始时间',
  window_end STRING COMMENT '窗口结束时间',
  window_start_timestamp TIMESTAMP(3) COMMENT '窗口开始时间',
  window_end_timestamp TIMESTAMP(3) COMMENT '窗口结束时间',
  cnt BIGINT COMMENT '次数',
  min_time STRING COMMENT '最小行为时间',
  max_time STRING COMMENT '最大行为时间'
) WITH (
  'connector' = 'print',
  'print-identifier' = 'PT'
)

INSERT INTO user_behavior_cnt
SELECT
  DATE_FORMAT(TUMBLE_START(process_time, INTERVAL '1' HOUR), 'yyyy-MM-dd HH:mm:ss') AS window_start,
  DATE_FORMAT(TUMBLE_END(process_time, INTERVAL '1' HOUR), 'yyyy-MM-dd HH:mm:ss') AS window_end,
  TUMBLE_START(process_time, INTERVAL '1' HOUR) AS window_start_timestamp,
  TUMBLE_END(process_time, INTERVAL '1' HOUR) AS window_end_timestamp,
  COUNT(*) AS cnt,
  MIN(`time`) AS min_time,
  MAX(`time`) AS max_time
FROM user_behavior
GROUP BY TUMBLE(process_time, INTERVAL '1' HOUR)