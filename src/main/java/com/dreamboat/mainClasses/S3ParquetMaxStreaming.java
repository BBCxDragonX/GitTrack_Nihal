package com.dreamboat.mainClasses;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import static org.apache.spark.sql.functions.*;

public class S3ParquetMaxStreaming {
    public static void main(String[] args) {
        // S3 Path and Column Name
        String s3Path = "s3a://your-bucket/path/to/parquet";
        String columnName = "your_column_name";

        // Initialize Spark Session
        SparkSession spark = SparkSession.builder()
                .appName("Read Max Value from S3 Parquet")
                .config("spark.hadoop.fs.s3a.access.key", "your-access-key")
                .config("spark.hadoop.fs.s3a.secret.key", "your-secret-key")
                .config("spark.hadoop.fs.s3a.endpoint", "s3.amazonaws.com")
                .config("spark.hadoop.fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem")
                .master("local[*]") // Remove for cluster execution
                .getOrCreate();

        // Read Parquet file from S3
        Dataset<Row> parquetData = spark.read().parquet(s3Path);

        // Compute max value of the specified column
        Row maxRow = parquetData.agg(max(columnName)).first();

        if (maxRow != null) {
            System.out.println("Max value in column " + columnName + ": " + maxRow.get(0));
        } else {
            System.out.println("No data found in the specified column.");
        }

        // Stop Spark session
        spark.stop();
    }
}
