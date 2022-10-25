package com.flink.example.table.function.top;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.state.hashmap.HashMapStateBackend;
import org.apache.flink.runtime.state.storage.FileSystemCheckpointStorage;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

/**
 * 功能：TopN 无排名优化 示例
 * 作者：SmartSi
 * CSDN博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2022/10/18 上午8:20
 */
public class TopWithoutRankExample {
    public static void main(String[] args) {
        // 执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(2);
        env.disableOperatorChaining();
        // 状态后端
        env.setStateBackend(new HashMapStateBackend());
        // 开启 Checkpoint
        env.enableCheckpointing(10000);
        String checkpointPath = "hdfs://localhost:9000/flink/checkpoint";
        env.getCheckpointConfig().setCheckpointStorage(new FileSystemCheckpointStorage(checkpointPath));

        // Table 执行环境
        EnvironmentSettings settings = EnvironmentSettings
                .newInstance()
                .inStreamingMode()
                .build();
        StreamTableEnvironment tEnv = StreamTableEnvironment.create(env, settings);
        Configuration config = tEnv.getConfig().getConfiguration();
        // 设置作业名称
        config.setString("pipeline.name", TopWithoutRankExample.class.getSimpleName());

        // 创建输入表
        tEnv.executeSql("CREATE TABLE shop_sales (\n" +
                "  product_id BIGINT COMMENT '商品Id',\n" +
                "  category STRING COMMENT '商品类目',\n" +
                "  price BIGINT COMMENT '行为类型',\n" +
                "  `timestamp` BIGINT COMMENT '行为时间',\n" +
                "  ts_ltz AS TO_TIMESTAMP_LTZ(`timestamp`, 3), -- 事件时间\n" +
                "  WATERMARK FOR ts_ltz AS ts_ltz - INTERVAL '5' SECOND -- 在 ts_ltz 上定义watermark，ts_ltz 成为事件时间列\n" +
                ") WITH (\n" +
                "  'connector' = 'kafka',\n" +
                "  'topic' = 'shop_sales',\n" +
                "  'properties.bootstrap.servers' = 'localhost:9092',\n" +
                "  'properties.group.id' = 'shop_sales',\n" +
                "  'scan.startup.mode' = 'latest-offset',\n" +
                "  'format' = 'json',\n" +
                "  'json.ignore-parse-errors' = 'false',\n" +
                "  'json.fail-on-missing-field' = 'true'\n" +
                ")");

        // 创建输出表 不保存排名字段
        tEnv.executeSql("CREATE TABLE shop_category_order_top (\n" +
                "  category STRING COMMENT '商品类目',\n" +
                "  product_id BIGINT COMMENT '商品Id',\n" +
                "  price BIGINT COMMENT '下单量',\n" +
                "  `time` TIMESTAMP_LTZ(3) COMMENT '下单时间'\n" +
                ") WITH (\n" +
                "  'connector' = 'print'\n" +
                ")");

        // 执行计算并输出
        tEnv.executeSql("INSERT INTO shop_category_order_top\n" +
                "SELECT\n" +
                "  category, product_id, price, ts_ltz AS `time`\n" +
                "FROM (\n" +
                "  SELECT\n" +
                "    category, product_id, price, ts_ltz,\n" +
                "    ROW_NUMBER() OVER (PARTITION BY category ORDER BY price DESC) AS row_num\n" +
                "  FROM shop_sales\n" +
                ")\n" +
                "WHERE row_num <= 3");
    }
}