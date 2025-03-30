package com.dreamboat.LeetCode;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

public class ColumnSelection {
    public static void main(String[] args) {
        SparkSession spark = SparkSession.builder()
                .appName("ColumnSelection")
                .master("local[*]") // Adjust based on your cluster configuration
                .getOrCreate();

        // Read your CSV data (replace "your_csv_file.csv" with your actual file path)
        Dataset<Row> df = spark.read()
                .format("csv")
                .option("header", true)
                .load("D:\\CodeDump\\JavaSparkPractice\\data\\Reader.csv");

        // Select the desired column (replace "column_name" with the actual column name)
        Dataset<Row> selectedColumn = df.select("Units");
        Dataset<Row> dropColumn = df.drop("Units");

        // Show the selected column
        selectedColumn.show();
        dropColumn.show();

        spark.stop();
    }
}
