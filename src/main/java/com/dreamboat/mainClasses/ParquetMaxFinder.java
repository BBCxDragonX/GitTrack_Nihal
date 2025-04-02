package com.dreamboat.mainClasses;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.IntVector;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.ipc.ArrowFileReader;


public class ParquetMaxFinder {
    public static void main(String[] args) throws IOException {
        String folderPath = "/path/to/parquet/folder"; // Change this to your actual Parquet directory
        String columnName = "target_column"; // Change this to your required column

        File folder = new File(folderPath);
        if (!folder.isDirectory()) {
            System.err.println("Invalid directory path");
            return;
        }

        File[] parquetFiles = folder.listFiles((dir, name) -> name.endsWith(".parquet"));
        if (parquetFiles == null || parquetFiles.length == 0) {
            System.err.println("No Parquet files found in the directory.");
            return;
        }

        int globalMax = Integer.MIN_VALUE;
        RootAllocator allocator = new RootAllocator(Long.MAX_VALUE);

        for (File parquetFile : parquetFiles) {
            try (FileChannel fileChannel = FileChannel.open(Paths.get(parquetFile.getAbsolutePath()));
                 ArrowFileReader reader = new ArrowFileReader(fileChannel, allocator)) {

                while (reader.loadNextBatch()) {
                    VectorSchemaRoot root = reader.getVectorSchemaRoot();
                    IntVector columnVector = (IntVector) root.getVector(columnName);
                    if (columnVector == null) {
                        System.err.println("Column not found: " + columnName);
                        continue;
                    }

                    int rowCount = root.getRowCount();
                    for (int i = 0; i < rowCount; i++) {
                        if (!columnVector.isNull(i)) {
                            globalMax = Math.max(globalMax, columnVector.get(i));
                        }
                    }
                }
            }
        }
        allocator.close();

        System.out.println("Max value in column '" + columnName + "': " + globalMax);
    }
}