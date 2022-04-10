package com.flink.example.table.table;/**
 * Created by wy on 2022/4/10.
 */

import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.types.Row;

/**
 * 功能：SQL 方式输出表示例
 * 作者：SmartSi
 * 博客：http://smartsi.club/
 * 公众号：大数据生态
 * 日期：2022/4/10 下午7:59
 */
public class TableOutputExample {
    public static void main(String[] args) {
        // 创建流和表执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);

        // 创建 DataStream
        DataStream<Row> dataStream = env.fromElements(
                Row.of("Alice", 12),
                Row.of("Bob", 10),
                Row.of("Alice", 100),
                Row.of("Lucy", 50)
        );
        // 将 DataStream 转换为 Table
        Table inputTable = tableEnv.fromDataStream(dataStream).as("name", "score");
        // 注册输入虚拟表
        tableEnv.createTemporaryView("input_table", inputTable);
        // 创建输出 Print Connector 表
        String sinkSql = "CREATE TEMPORARY TABLE print_sink_table (\n" +
                "  name STRING,\n" +
                "  score BIGINT\n" +
                ") WITH (\n" +
                "  'connector' = 'print'\n" +
                ")";
        tableEnv.executeSql(sinkSql);

        // 1. 通过 SQL INSERT INTO
        String sql = "INSERT INTO print_sink_table\n" +
                "SELECT name, SUM(score) AS score_sum\n" +
                "FROM input_table\n" +
                "GROUP BY name";
        tableEnv.executeSql(sql);

        // 2. 通过 Table API executeInsert
        String selectSQL = "SELECT name, SUM(score) AS score_sum\n" +
                "FROM input_table\n" +
                "GROUP BY name";
        Table outputTable = tableEnv.sqlQuery(selectSQL);
        outputTable.executeInsert("print_sink_table");
    }
}
