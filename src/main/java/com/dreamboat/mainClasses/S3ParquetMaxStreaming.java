package com.dreamboat.mainClasses;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.s3a.S3AFileSystem;
import org.apache.parquet.column.ColumnDescriptor;
import org.apache.parquet.example.data.Group;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.hadoop.ParquetReader;
import org.apache.parquet.hadoop.example.GroupReadSupport;
import org.apache.parquet.hadoop.metadata.ParquetMetadata;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.Type;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

public class S3ParquetMaxStreaming {
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

    public static Comparable<?> findMaxValueInColumn(String s3FolderPath, String columnName) throws IOException {
        Configuration conf = new Configuration();
        conf.set("fs.s3a.aws.credentials.provider", DefaultAWSCredentialsProviderChain.class.getName());
        conf.set("fs.s3a.impl", S3AFileSystem.class.getName());
        conf.set("fs.s3a.path.style.access", "true");
        conf.set("fs.s3a.connection.ssl.enabled", "true");
        conf.set("fs.s3a.fast.upload", "true");

        Path path = new Path(s3FolderPath);
        AtomicReference<Comparable<?>> maxValue = new AtomicReference<>(null);

        try (ParquetFileReader fileReader = ParquetFileReader.open(conf, path)) {
            ParquetMetadata metadata = fileReader.getFooter();
            MessageType schema = metadata.getFileMetaData().getSchema();

            if (!schema.containsField(columnName)) {
                throw new IllegalArgumentException("Column '" + columnName + "' not found in Parquet schema");
            }

            int fieldIndex = schema.getFieldIndex(columnName);
            Type fieldType = schema.getType(fieldIndex);

            GroupReadSupport readSupport = new GroupReadSupport();
            readSupport.init(conf, metadata.getFileMetaData().getKeyValueMetaData(), schema);

            try (ParquetReader<Group> reader = ParquetReader.builder(readSupport, path)
                    .withConf(conf)
                    .build()) {
                Group group;
                while ((group = reader.read()) != null) {
                    Comparable<?> currentValue = getValueFromGroup(group, fieldIndex, fieldType);
                    if (currentValue != null) {
                        maxValue.updateAndGet(currentMax ->
                                (currentMax == null || compareValues(currentValue, currentMax) > 0) ? currentValue : currentMax);
                    }
                }
            }
        }

        if (maxValue.get() == null) {
            throw new RuntimeException("No values found in column '" + columnName + "'");
        }
        return maxValue.get();
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
            default:
                throw new UnsupportedOperationException("Unsupported column type: " + fieldType);
        }
    }

    @SuppressWarnings("unchecked")
    private static int compareValues(Comparable<?> a, Comparable<?> b) {
        return ((Comparable<Object>) a).compareTo(b);
    }
}
