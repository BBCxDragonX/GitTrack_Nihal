package com.dreamboat.utilClasses;

import org.apache.spark.sql.*;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;

import java.util.Properties;

public class ScalableMySQLToParquet {
    public static void main(String[] args) {
        SparkSession spark = SparkSession.builder()
                .appName("ScalableMySQLToParquet")
                .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
                .getOrCreate();

        // Step 1: Fetch min/max ID
        String url = "jdbc:mysql://host:port/db?useCursorFetch=true";
        Properties props = new Properties();
        props.setProperty("user", "user");
        props.setProperty("password", "pass");
        props.setProperty("fetchSize", "100");

        long minId = spark.read().jdbc(url, "(SELECT MIN(id) AS min_id FROM your_table)", props).first().getLong(0);
        long maxId = spark.read().jdbc(url, "(SELECT MAX(id) AS max_id FROM your_table)", props).first().getLong(0);
        int numPartitions = 100; // Adjust based on cluster cores

        // Step 2: Read partitioned data
        StructType schema = new StructType()
                .add("id", DataTypes.LongType)
                .add("large_text", DataTypes.StringType);

        Dataset<Row> df = spark.read()
                .schema(schema)
                .jdbc(url, "your_table", "id", minId, maxId, numPartitions, props);

        // Step 3: Write to Parquet
        df.repartition(numPartitions)
                .write()
                .option("parquet.block.size", 256 * 1024 * 1024)
                .option("compression", "snappy")
                .parquet("output_path");

        spark.stop();
    }
}
