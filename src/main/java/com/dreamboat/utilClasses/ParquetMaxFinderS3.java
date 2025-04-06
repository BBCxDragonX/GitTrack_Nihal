package com.dreamboat.utilClasses;

import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.List;

import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.IntVector;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.ipc.ArrowStreamReader;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

public class ParquetMaxFinderS3 {
    public static void main(String[] args) {
        String bucketName = "your-bucket-name"; // Change to your S3 bucket name
        String folderPath = "your-folder-path/"; // Change to your S3 folder prefix (must end with '/')
        String columnName = "target_column"; // Change to your required column

        S3Client s3Client = S3Client.builder().credentialsProvider(DefaultCredentialsProvider.create()).build();
        RootAllocator allocator = new RootAllocator(Long.MAX_VALUE);
        int globalMax = Integer.MIN_VALUE;

        try {
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(folderPath)
                    .build();

            ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);
            List<S3Object> objects = listResponse.contents();

            for (S3Object s3Object : objects) {
                if (!s3Object.key().endsWith(".parquet")) continue;

                GetObjectRequest getRequest = GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(s3Object.key())
                        .build();

                try (ResponseInputStream<?> s3Stream = s3Client.getObject(getRequest);
                     ReadableByteChannel channel = Channels.newChannel(s3Stream);
                     ArrowStreamReader reader = new ArrowStreamReader(s3Stream, allocator)) {

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
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            allocator.close();
            s3Client.close();
        }

        System.out.println("Max value in column '" + columnName + "': " + globalMax);
    }
}