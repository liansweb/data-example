package com.flink.example.stream.function.stateful;

import org.apache.flink.api.common.functions.*;
import org.apache.flink.api.common.state.*;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;


/**
 * 功能：CheckpointedFunction 实现操作 KeyedState 的有状态函数
 *         连续两次的温度变化超过阈值则报警
 * 作者：SmartSi
 * CSDN博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2023/4/17 下午11:03
 */
public class CheckpointedFunctionKSExample {
    private static final Logger LOG = LoggerFactory.getLogger(CheckpointedFunctionKSExample.class);

    public static void main(String[] args) throws Exception {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(2);

        // 每10s一次Checkpoint
        env.enableCheckpointing(30 * 1000);

        // Socket 输入
        DataStream<String> stream = env.socketTextStream("localhost", 9100, "\n");

        // 传感器温度流
        DataStream<Tuple3<String, Double, Double>> alertStream = stream.map(new MapFunction<String, Tuple2<String, Double>>() {
            @Override
            public Tuple2<String, Double> map(String value) throws Exception {
                if(Objects.equals(value, "ERROR")) {
                    throw new RuntimeException("error dirty data");
                }
                String[] params = value.split(",");
                LOG.info("sensor input, id: {}, temperature: {}", params[0], params[1]);
                return new Tuple2<>(params[0], Double.parseDouble(params[1]));
            }
        }).keyBy(new KeySelector<Tuple2<String, Double>, String>() {
            @Override
            public String getKey(Tuple2<String, Double> sensor) throws Exception {
                return sensor.f0;
            }
        }).flatMap(new TemperatureAlertFlatMapFunction(10));// 温度变化超过10度则报警
        alertStream.print();

        env.execute("CheckpointedFunctionKSExample");
    }

    // FlatMap 的好处是在温度变化不超过阈值的时候不进行输出
    public static class TemperatureAlertFlatMapFunction implements CheckpointedFunction, FlatMapFunction<Tuple2<String, Double>, Tuple3<String, Double, Double>> {
        // 温度差报警阈值
        private double threshold;
        // 上一次温度
        private ValueState<Double> lastTemperatureState;
        private Double lastTemperature;
        public TemperatureAlertFlatMapFunction(double threshold) {
            this.threshold = threshold;
        }

        @Override
        public void flatMap(Tuple2<String, Double> sensor, Collector<Tuple3<String, Double, Double>> out) throws Exception {
            String sensorId = sensor.f0;
            // 当前温度
            double temperature = sensor.f1;
            // 是否有保存上一次的温度
            if (Objects.equals(lastTemperature, null)) {
                LOG.info("sensor first temperature, id: {}, temperature: {}", sensorId, temperature);
                return;
            }
            double diff = Math.abs(temperature - lastTemperature);
            if (diff > threshold) {
                // 温度变化超过阈值
                LOG.info("sensor alert, id: {}, temperature: {}, lastTemperature: {}, diff: {}", sensorId, temperature, lastTemperature, diff);
                out.collect(Tuple3.of(sensorId, temperature, diff));
            } else {
                LOG.info("sensor no alert, id: {}, temperature: {}, lastTemperature: {}, diff: {}", sensorId, temperature, lastTemperature, diff);
            }
            lastTemperature = temperature;
        }

        @Override
        public void snapshotState(FunctionSnapshotContext context) throws Exception {
            // 获取最新的温度之后更新保存上一次温度的状态
            //lastTemperatureState.clear();
            lastTemperatureState.update(lastTemperature);
            LOG.info("sensor snapshotState, temperature: {}", lastTemperature);
        }

        @Override
        public void initializeState(FunctionInitializationContext context) throws Exception {
            // 初始化
            ValueStateDescriptor<Double> stateDescriptor = new ValueStateDescriptor<>("lastTemperature", Double.class);
            lastTemperatureState = context.getKeyedStateStore().getState(stateDescriptor);
            if (context.isRestored()) {
                lastTemperature = lastTemperatureState.value();
                LOG.info("sensor initializeState, lastTemperature: {}", lastTemperature);
            }
        }
    }
}
// 1,35.4
// 1,20.8
// 2,23.5
// ERROR
// 1,31.6
// 2,37.2