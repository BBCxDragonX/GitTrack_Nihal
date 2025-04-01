package com.dreamboat.mainClasses;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.fs.s3a.S3AFileSystem;
import org.apache.parquet.example.data.Group;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.hadoop.ParquetReader;
import org.apache.parquet.hadoop.example.GroupReadSupport;
import org.apache.parquet.hadoop.metadata.ParquetMetadata;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.Type;
import org.apache.parquet.io.api.Binary;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

public class S3ParquetMaxValue {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java S3ParquetMaxValue <s3FolderPath> <columnName>");
            System.exit(1);
        }

        String s3FolderPath = args[0];
        String columnName = args[1];

        if (!s3FolderPath.startsWith("s3a://")) {
            throw new IllegalArgumentException("Invalid S3 path. Must start with s3a://");
        }

        try {
            Comparable<?> maxValue = findMaxValueInColumn(s3FolderPath, columnName);
            System.out.println("Max value in column '" + columnName + "': " + maxValue);
        } catch (Exception e) {
            System.err.println("Error processing Parquet files: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static Comparable<?> findMaxValueInColumn(String s3FolderPath, String columnName) throws Exception {
        Configuration conf = new Configuration();
        conf.set("fs.s3a.aws.credentials.provider", DefaultAWSCredentialsProviderChain.class.getName());
        conf.set("fs.s3a.impl", S3AFileSystem.class.getName());
        conf.set("fs.s3a.path.style.access", "true");
        conf.set("fs.s3a.connection.ssl.enabled", "true");
        conf.set("fs.s3a.fast.upload", "true");

        FileSystem fs = FileSystem.get(new URI(s3FolderPath), conf);
        RemoteIterator<LocatedFileStatus> fileIterator = fs.listFiles(new Path(s3FolderPath), true);
        AtomicReference<Comparable<?>> maxValue = new AtomicReference<>(null);

        while (fileIterator.hasNext()) {
            LocatedFileStatus fileStatus = fileIterator.next();
            if (fileStatus.getPath().getName().endsWith(".parquet")) {
                System.out.println("Processing file: " + fileStatus.getPath());
                Comparable<?> fileMaxValue = processParquetFile(conf, fileStatus.getPath(), columnName);
                if (fileMaxValue != null) {
                    maxValue.updateAndGet(currentMax ->
                            (currentMax == null || compareValues(fileMaxValue, currentMax) > 0) ? fileMaxValue : currentMax);
                }
            }
        }

        if (maxValue.get() == null) {
            throw new RuntimeException("No values found in column '" + columnName + "'");
        }
        return maxValue.get();
    }

    private static Comparable<?> processParquetFile(Configuration conf, Path filePath, String columnName) throws IOException {
        AtomicReference<Comparable<?>> fileMaxValue = new AtomicReference<>(null);

        try (ParquetFileReader fileReader = ParquetFileReader.open(conf, filePath)) {
            ParquetMetadata metadata = fileReader.getFooter();
            MessageType schema = metadata.getFileMetaData().getSchema();

            if (!schema.containsField(columnName)) {
                throw new IllegalArgumentException("Column '" + columnName + "' not found in Parquet schema");
            }

            int fieldIndex = schema.getFieldIndex(columnName);
            Type fieldType = schema.getType(fieldIndex);

            GroupReadSupport readSupport = new GroupReadSupport();
            readSupport.init(conf, metadata.getFileMetaData().getKeyValueMetaData(), schema);

            try (ParquetReader<Group> reader = ParquetReader.builder(readSupport, filePath)
                    .withConf(conf)
                    .build()) {
                Group group;
                while ((group = reader.read()) != null) {
                    Comparable<?> currentValue = getValueFromGroup(group, fieldIndex, fieldType);
                    if (currentValue != null) {
                        fileMaxValue.updateAndGet(currentMax ->
                                (currentMax == null || compareValues(currentValue, currentMax) > 0) ? currentValue : currentMax);
                    }
                }
            }
        }
        return fileMaxValue.get();
    }

    private static Comparable<?> getValueFromGroup(Group group, int fieldIndex, Type fieldType) {
        if (group.getFieldRepetitionCount(fieldIndex) == 0) {
            return null;
        }
        switch (fieldType.asPrimitiveType().getPrimitiveTypeName()) {
            case INT32:
                return group.getInteger(fieldIndex, 0);
            case INT64:
                return group.getLong(fieldIndex, 0);
            case FLOAT:
                return group.getFloat(fieldIndex, 0);
            case DOUBLE:
                return group.getDouble(fieldIndex, 0);
            case BINARY:
                return group.getString(fieldIndex, 0);
            case BOOLEAN:
                return group.getBoolean(fieldIndex, 0);
            case INT96:
                return convertInt96ToTimestamp(group.getBinary(fieldIndex, 0));
            default:
                throw new UnsupportedOperationException("Unsupported column type: " + fieldType);
        }
    }

    private static String convertInt96ToTimestamp(Binary binary) {
        ByteBuffer buffer = binary.toByteBuffer();
        buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);

        long nanos = buffer.getLong();
        int julianDay = buffer.getInt();

        long epochSeconds = (julianDay - 2440588L) * 86400L;
        Instant instant = Instant.ofEpochSecond(epochSeconds, nanos);

        return instant.toString();
    }

    @SuppressWarnings("unchecked")
    private static int compareValues(Comparable<?> a, Comparable<?> b) {
        return ((Comparable<Object>) a).compareTo(b);
    }
}
