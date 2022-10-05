-- 1. 基于事件时间的滑动窗口
CREATE TABLE user_behavior (
  uid BIGINT COMMENT '用户Id',
  pid BIGINT COMMENT '商品Id',
  cid BIGINT COMMENT '商品类目Id',
  type STRING COMMENT '行为类型',
  `timestamp` BIGINT COMMENT '行为时间',
  `time` STRING COMMENT '行为时间',
  ts_ltz AS TO_TIMESTAMP_LTZ(`timestamp`, 3), -- 事件时间
  WATERMARK FOR ts_ltz AS ts_ltz - INTERVAL '5' SECOND -- 在 ts_ltz 上定义watermark，ts_ltz 成为事件时间列
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
  window_start TIMESTAMP(3) COMMENT '窗口开始时间',
  window_end TIMESTAMP(3) COMMENT '窗口结束时间',
  cnt BIGINT COMMENT '次数',
  min_time STRING COMMENT '最小行为时间',
  max_time STRING COMMENT '最大行为时间',
  pid_set MULTISET<BIGINT> COMMENT '商品集合'
) WITH (
  'connector' = 'print'
)

INSERT INTO user_behavior_cnt
SELECT
  HOP_START(ts_ltz, INTERVAL '30' SECOND, INTERVAL '1' MINUTE) AS window_start,
  HOP_END(ts_ltz, INTERVAL '30' SECOND, INTERVAL '1' MINUTE) AS window_end,
  COUNT(*) AS cnt,
  MIN(`time`) AS min_time,
  MAX(`time`) AS max_time,
  COLLECT(DISTINCT pid) AS pid_set
FROM user_behavior
GROUP BY HOP(ts_ltz, INTERVAL '30' SECOND, INTERVAL '1' MINUTE)