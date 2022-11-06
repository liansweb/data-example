package com.hive.example.udf;

import com.common.example.utils.BitmapFunction;
import org.apache.hadoop.hive.ql.exec.UDF;
import org.apache.hadoop.io.BytesWritable;
import org.apache.parquet.io.api.Binary;

/**
 * 功能：Bitmap 与
 * 作者：SmartSi
 * CSDN博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2022/11/6 下午6:08
 */
public class BitmapAnd extends UDF {
    public BytesWritable evaluate(Binary b1, Binary b2) {
        BytesWritable result = BitmapFunction.bitmapAnd(b1, b2);
        return result;
    }
}
