package com.flink.example.stream.watermark.extractor;

import com.common.example.utils.DateUtil;
import com.flink.example.stream.source.simple.AscendingTimestampSource;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.timestamps.AscendingTimestampExtractor;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 功能：周期性 Watermark 分配器 AscendingTimestampExtractor 示例
 * 作者：SmartSi
 * CSDN博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2022/8/31 下午11:32
 */
public class AscendingWatermarkExample {
    private static final Logger LOG = LoggerFactory.getLogger(AscendingWatermarkExample.class);

    public static void main(String[] args) throws Exception {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        // 设置 Checkpoint
        env.enableCheckpointing(1000L);
        // 设置事件时间特性
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        // 每5s输出一次 Watermark
        env.getConfig().setAutoWatermarkInterval(5000);

        // 输入源 每1s输出一个单词
        DataStream<Tuple2<String, Long>> source = env.addSource(new AscendingTimestampSource(10*1000L));

        // 计算单词出现的次数
        SingleOutputStreamOperator<Tuple4<String, Long, String, String>> stream = source
                .assignTimestampsAndWatermarks(new AscendingTimestampExtractor<Tuple2<String, Long>>() {
                    @Override
                    public long extractAscendingTimestamp(Tuple2<String, Long> tuple) {
                        // 提取时间戳
                        return tuple.f1;
                    }
                })
                .map(new MapFunction<Tuple2<String, Long>, Tuple2<String, Long>>() {
                    @Override
                    public Tuple2<String, Long> map(Tuple2<String, Long> tuple2) throws Exception {
                        return Tuple2.of(tuple2.f0, 1L);
                    }
                })
                // 分组
                .keyBy(new KeySelector<Tuple2<String, Long>, String>() {
                    @Override
                    public String getKey(Tuple2<String, Long> element) throws Exception {
                        return element.f0;
                    }
                })
                // 每1分钟一个窗口
                .timeWindow(Time.minutes(1))
                // 求和
                .process(new ProcessWindowFunction<Tuple2<String, Long>, Tuple4<String, Long, String, String>, String, TimeWindow>() {
                    @Override
                    public void process(String word, Context context, Iterable<Tuple2<String, Long>> elements, Collector<Tuple4<String, Long, String, String>> out) throws Exception {
                        // 计算出现次数
                        long count = 0;
                        for (Tuple2<String, Long> element : elements) {
                            count ++;
                        }
                        // 当前 Watermark
                        long currentWatermark = context.currentWatermark();
                        // 时间窗口元数据
                        TimeWindow window = context.window();
                        long start = window.getStart();
                        long end = window.getEnd();
                        String startTime = DateUtil.timeStamp2Date(start, "yyyy-MM-dd HH:mm:ss");
                        String endTime = DateUtil.timeStamp2Date(end, "yyyy-MM-dd HH:mm:ss");
                        LOG.info("word: {}, count: {}, watermark: {}, windowStart: {}, windowEnd: {}",
                                word, count, currentWatermark,
                                start + "|" + startTime, end + "|" + endTime
                        );
                        // 输出
                        out.collect(Tuple4.of(word, count, startTime, endTime));
                    }
                });

        stream.print();
        env.execute("AscendingWatermarkExample");
    }
}
