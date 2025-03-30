package com.dreamboat.LeetCode;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

public class CSVSchemaReader {
    public static void main(String[] args) {
        SparkSession spark = SparkSession.builder()
                .appName("CSVSchemaReader")
                .master("local[*]")
                .getOrCreate();

        Dataset<Row> df = spark.read()
                .format("csv")
                .option("header", true)
                .option("inferSchema", true)
                .load("D:\\CodeDump\\JavaSparkPractice\\data\\Reader.csv");

        df.printSchema();
        spark.stop();
    }
}
