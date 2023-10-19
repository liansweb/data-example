package com.spark.example.sql.datasource;

import org.apache.spark.sql.AnalysisException;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

/**
 * 功能：数据源 Load 与 Save 示例
 * 作者：SmartSi
 * CSDN博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2023/10/19 07:57
 */
public class BasicDataSourceExample {
    public static void main(String[] args) throws AnalysisException {
        SparkSession spark = SparkSession
                .builder()
                .appName("Java Spark SQL basic example")
                .master("local[*]")
                .getOrCreate();

        test(spark);
        //defaultFormat(spark);
        //jsonFormat(spark);
        //runSQLOnFile(spark);
        spark.stop();
    }

    // 测试
    private static void test(SparkSession spark) {
        // 如果不指定 format 默认读取的是 parquet 文件
        Dataset<Row> usersDF = spark.read().load("spark-example-3.1/src/main/resources/data/users.parquet");
        usersDF.show();
        // 如果不指定 format 默认保存的是 parquet 文件
        usersDF.select("name", "favorite_color", "favorite_numbers").write().format("csv").save("users.parquet");
    }

    // 默认 Format
    private static void defaultFormat(SparkSession spark) {
        // 如果不指定 format 默认读取的是 parquet 文件
        Dataset<Row> usersDF = spark.read().load("spark-example-3.1/src/main/resources/data/users.parquet");
        usersDF.show();
        // 如果不指定 format 默认保存的是 parquet 文件
        //usersDF.select("name", "favorite_color").write().save("namesAndFavColors.parquet");
    }

    private static void jsonFormat(SparkSession spark) {
        // format 指定为 json 读取的是 json 文件
        Dataset<Row> usersDF = spark.read().format("json").load("spark-example-3.1/src/main/resources/data/users.json");
        usersDF.show();
        // format 指定为 json  保存的是 json 文件
        usersDF.select("name", "age").write().format("json").save("namesAndFavColors.json");
    }

    private static void csvFormat(SparkSession spark) {
        // format 指定为 csv 读取的是 csv 文件
        Dataset<Row> usersDF = spark.read().format("csv")
                .option("sep", ";")
                .option("inferSchema", "true")
                .option("header", "true")
                .load("spark-example-3.1/src/main/resources/data/people.csv");
        usersDF.show();
    }


    private static void orcFormat(SparkSession spark) {
        // format 指定为 json 读取的是 json 文件
        Dataset<Row> usersDF = spark.read().format("json").load("spark-example-3.1/src/main/resources/data/users.json");
        usersDF.show();

        // format 指定为 orc 保存的是 orc 文件
        usersDF.write().format("orc")
                .option("orc.bloom.filter.columns", "favorite_color")
                .option("orc.dictionary.key.threshold", "1.0")
                .option("orc.column.encoding.direct", "name")
                .save("users_with_options.orc");
    }

    private static void parquetFormat(SparkSession spark) {
        // format 指定为 json 读取的是 json 文件
        Dataset<Row> usersDF = spark.read().format("json").load("spark-example-3.1/src/main/resources/data/users.json");
        usersDF.show();

        // format 指定为 parquet 保存的是 parquet 文件
        usersDF.write().format("parquet")
                .option("parquet.bloom.filter.enabled#favorite_color", "true")
                .option("parquet.bloom.filter.expected.ndv#favorite_color", "1000000")
                .option("parquet.enable.dictionary", "true")
                .option("parquet.page.write-checksum.enabled", "false")
                .save("users_with_options.parquet");
    }

    // 文件上直接运行 SQL
    private static void runSQLOnFile(SparkSession spark) {
        // 在 parquet 文件上直接运行 SQL
        Dataset<Row> parquetDF = spark.sql("SELECT * FROM parquet.`spark-example-3.1/src/main/resources/data/users.parquet`");
        parquetDF.show();
        // 在 json 文件上直接运行 SQL
        Dataset<Row> jsonDF = spark.sql("SELECT * FROM json.`spark-example-3.1/src/main/resources/data/users.json`");
        jsonDF.show();
    }

    private static void bucketBy(SparkSession spark) {
//                peopleDF.write().bucketBy(42, "name").sortBy("age").saveAsTable("people_bucketed");
//        usersDF
//                .write()
//                .partitionBy("favorite_color")
//                .format("parquet")
//                .save("namesPartByColor.parquet");
//        spark.sql("DROP TABLE IF EXISTS people_bucketed");
    }

    private static void partitionBy(SparkSession spark) {
//                usersDF
//                .write()
//                .partitionBy("favorite_color")
//                .bucketBy(42, "name")
//                .saveAsTable("users_partitioned_bucketed");
//        spark.sql("DROP TABLE IF EXISTS users_partitioned_bucketed");
    }
}
