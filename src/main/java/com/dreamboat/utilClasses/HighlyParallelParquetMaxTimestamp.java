package com.dreamboat.utilClasses;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.log4j.Logger;
import org.apache.parquet.column.statistics.Statistics;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.hadoop.metadata.ParquetMetadata;
import org.apache.parquet.io.api.Binary;
// ... other necessary imports from previous examples ...
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.apache.parquet.hadoop.metadata.ColumnChunkMetaData;
import org.apache.parquet.hadoop.metadata.BlockMetaData;
import org.apache.parquet.schema.PrimitiveType;
import org.apache.parquet.schema.Type;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.*;


public class HighlyParallelParquetMaxTimestamp {

    private static final Logger logger = (Logger) LoggerFactory.getLogger(HighlyParallelParquetMaxTimestamp.class);
    private static final long JULIAN_DAY_OF_EPOCH = 2440588L; // Julian day for 1970-01-01
    private static final long MILLIS_PER_DAY = TimeUnit.DAYS.toMillis(1); // Milliseconds in a day
    private static final long NANOS_PER_MILLISECOND = TimeUnit.MILLISECONDS.toNanos(1);
    private static final long NANOS_PER_SECOND = TimeUnit.SECONDS.toNanos(1);

    // *** IMPORTANT: Endianness assumption for INT96 parsing ***
    // Parquet spec doesn't mandate endianness for INT96. Many systems (Impala, Hive)
    // write it as LITTLE_ENDIAN. If your files use BIG_ENDIAN, change this.
    private static final ByteOrder INT96_BYTE_ORDER = ByteOrder.LITTLE_ENDIAN;

    public static void main(String[] args) {
        String bucketName = "your-bucket-name";
        String prefix = "path/to/your/files/"; // S3 prefix where Parquet files reside
        String columnName = "your_int96_timestamp_column";
        int numThreads = Runtime.getRuntime().availableProcessors() * 2; // Example tuning

        // 1. Setup Shared Resources (Thread-Safe)
        Configuration conf = setupHadoopConfig(); // Method to create Hadoop Conf
        AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                //.withRegion("your-region") // Optional: If not default
                .build(); // Use default credential provider chain

        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
        logger.info("Initialized with {} threads.");

        Timestamp overallMaxTimestamp = null;
        long startTime = System.currentTimeMillis();

        try {
            // 2. List S3 Files Efficiently
            List<String> parquetFileKeys = listParquetFileKeys(s3Client, bucketName, prefix);
            logger.info("Found {} potential Parquet files.");

            if (parquetFileKeys.isEmpty()) {
                System.out.println("No Parquet files found at the specified prefix.");
                return;
            }

            // 3. Submit Tasks Asynchronously
            List<CompletableFuture<Timestamp>> futures = new ArrayList<>();
            for (String fileKey : parquetFileKeys) {
                Path filePath = new Path("s3a://" + bucketName + "/" + fileKey);
                Callable<Timestamp> task = () -> processSingleFile(conf, filePath, columnName);
                futures.add(CompletableFuture.supplyAsync(
                        () -> processSingleFile(conf, filePath, columnName), // Direct lambda calling the method
                        executorService
                ));
            }
            logger.info("Submitted {} tasks to executor.");

            // 4. Wait for all tasks and Aggregate Results
            CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            allOf.join(); // Wait for all futures to complete (or throw exception)

            logger.info("All tasks completed. Aggregating results...");

            // Use stream to find the max timestamp efficiently
            Optional<Timestamp> maxOptional = futures.stream()
                    .map(future -> {
                        try {
                            // join() is safe here because we already waited with allOf
                            return future.join();
                        } catch (CompletionException e) {
                            // Log errors from individual tasks
                            logger.error("Task failed for a file: {}");
                            return null; // Treat failed task as having no result
                        }
                    })
                    .filter(Objects::nonNull) // Filter out nulls (errors or no stats)
                    .max(Comparator.naturalOrder()); // Find the maximum timestamp

            if (maxOptional.isPresent()) {
                overallMaxTimestamp = maxOptional.get();
                System.out.println("Overall Maximum Timestamp: " + overallMaxTimestamp);
            } else {
                System.out.println("No valid maximum timestamp found across all files (or no files processed successfully).");
            }

        } catch (Exception e) { // Catch exceptions during listing, submission, or aggregation
            logger.error("An error occurred during the process: {}");
            System.err.println("Processing failed: " + e.getMessage());
        } finally {
            // 5. Shutdown Executor
            logger.info("Shutting down executor service...");
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    logger.warn("Executor did not terminate in 60s. Forcing shutdown...");
                    executorService.shutdownNow();
                }
            } catch (InterruptedException ie) {
                logger.error("Await termination interrupted.", ie);
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
            // Close S3 client if necessary (often managed by SDK lifecycle)
        }

        long duration = System.currentTimeMillis() - startTime;
        logger.info("Processing completed in {} ms.");
    }

    // --- Helper Methods ---

    private static Configuration setupHadoopConfig() {
        Configuration conf = new Configuration();
        conf.set("fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem");
        conf.set("fs.file.impl", "org.apache.hadoop.fs.LocalFileSystem");
        // Add credential config here ONLY IF NOT using default providers
        // conf.set("fs.s3a.access.key", "...");
        // conf.set("fs.s3a.secret.key", "...");
        // Consider adding S3A performance tuning parameters if needed
        // conf.set("fs.s3a.connection.maximum", "100"); // Example
        return conf;
    }

    private static List<String> listParquetFileKeys(AmazonS3 s3Client, String bucket, String prefix) {
        List<String> keys = new ArrayList<>();
        ListObjectsV2Request req = new ListObjectsV2Request().withBucketName(bucket).withPrefix(prefix); //.withDelimiter("/"); // Optional delimiter
        ListObjectsV2Result result;
        logger.info("Listing objects in s3://{}/{}");
        do {
            result = s3Client.listObjectsV2(req);
            for (S3ObjectSummary objectSummary : result.getObjectSummaries()) {
                String key = objectSummary.getKey();
                // Basic filtering - adjust if needed (e.g., exclude _$folder$ files)
                if (key.endsWith(".parquet") && !key.endsWith("/")) {
                    keys.add(key);
                }
            }
            String token = result.getNextContinuationToken();
            req.setContinuationToken(token);
        } while (result.isTruncated());
        logger.info("Finished listing. Found {} keys ending with .parquet");
        return keys;
    }

    /**
     * Processes a single Parquet file to find the max timestamp from stats.
     * Optimized for speed within the parallel task.
     */
    private static Timestamp processSingleFile(Configuration conf, Path filePath, String columnName) {
        // Simplified version of findMaxFromStats - focuses only on getting max Timestamp or null
        // Contains the logic to read footer, find column chunk stats, get max Binary, parse INT96
        // Returns null if column/stats not found, parsing fails, or other error occurs for this file.
        // MUST include try-catch blocks internally to prevent single file failure from killing the thread.

        logger.debug("Processing file: {}");
        ParquetMetadata footer = null;
        try {
            footer = ParquetFileReader.readFooter(conf, filePath); // S3 Read happens here
            PrimitiveType columnType = getColumnPrimitiveType(footer, columnName); // Reuse from previous example

            if (columnType.getPrimitiveTypeName() != PrimitiveType.PrimitiveTypeName.INT96) {
                logger.warn("Column {} in file {} is not INT96. Skipping timestamp processing for this file.");
                return null; // Or handle other types if needed
            }

            Timestamp fileMaxTimestamp = null;
            List<BlockMetaData> blocks = footer.getBlocks();

            for (BlockMetaData block : blocks) {
                ColumnChunkMetaData chunkMeta = findColumnChunkMetadata(block, columnName); // Reuse from previous
                if (chunkMeta != null) {
                    Statistics<?> stats = chunkMeta.getStatistics();
                    if (stats != null && !stats.isEmpty() && stats.hasNonNullValue()) {
                        Object maxObj = stats.genericGetMax();
                        if (maxObj instanceof Binary) {
                            try {
                                Timestamp chunkMaxTs = parseInt96ToTimestamp((Binary) maxObj); // Reuse from previous
                                if (fileMaxTimestamp == null || chunkMaxTs.after(fileMaxTimestamp)) {
                                    fileMaxTimestamp = chunkMaxTs;
                                }
                            } catch (IllegalArgumentException parseEx) {
                                logger.warn("Failed to parse INT96 statistic for column {} in file {}. Skipping chunk stat. Error: {}"
                                );
                            }
                        } else {
                            logger.warn("Expected Binary statistic for INT96 column {} in file {}, but got {}. Skipping chunk stat."
                            );
                        }
                    }
                }
            }
            logger.debug("Max timestamp for file {}: {}");
            return fileMaxTimestamp; // Max timestamp found in this file's stats, or null

        } catch (Exception e) {
            // Log file-specific errors and return null so other tasks continue
            logger.error("Failed to process file {}: {}");
            return null;
        }
    }

    // Include helper methods: getColumnPrimitiveType, findColumnChunkMetadata, parseInt96ToTimestamp
    // from the previous example here...
    /**
     * Parses a 12-byte Parquet INT96 value (expected as Binary) into a java.sql.Timestamp.
     * Assumes Little Endian byte order and the common Impala/Hive format.
     *
     * @param int96Binary The 12-byte Binary object.
     * @return The corresponding Timestamp.
     * @throws IllegalArgumentException if the Binary is not 12 bytes or parsing fails.
     */
    static Timestamp parseInt96ToTimestamp(Binary int96Binary) throws IllegalArgumentException {
        byte[] bytes = int96Binary.getBytes();
        if (bytes.length != 12) {
            throw new IllegalArgumentException("Invalid INT96 value: Expected 12 bytes, but received " + bytes.length);
        }

        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(INT96_BYTE_ORDER); // Use configured endianness

        long nanosOfDay = buffer.getLong(); // First 8 bytes: nanoseconds within the day
        int julianDay = buffer.getInt();    // Last 4 bytes: Julian day

        // Convert Julian day and nanoseconds to milliseconds since epoch
        long millisSinceEpoch = (julianDay - JULIAN_DAY_OF_EPOCH) * MILLIS_PER_DAY;

        // Calculate nanoseconds within the second for Timestamp.setNanos
        // Need millis within the day to correctly calculate the second
        long millisOfDay = nanosOfDay / NANOS_PER_MILLISECOND;
        millisSinceEpoch += millisOfDay; // Add milliseconds part of the day

        int nanosWithinSecond = (int) (nanosOfDay % NANOS_PER_SECOND); // Nanos within the current second

        Timestamp ts = new Timestamp(millisSinceEpoch);
        // Timestamp constructor uses millis. setNanos handles the fractional second part.
        try {
            ts.setNanos(nanosWithinSecond);
        } catch (IllegalArgumentException e) {
            // Handle potential edge cases where nanos might be slightly out of expected range
            // after calculations, though ideally shouldn't happen with % NANOS_PER_SECOND.
            logger.warn("Could not set nanos {} on timestamp for millis {}. Error: {}");
            // Depending on requirement, either return the timestamp truncated to millis, or re-throw
            throw new IllegalArgumentException("Failed to set nanoseconds part of INT96 timestamp", e);
        }

        return ts;
    }
    /** Finds ColumnChunkMetaData for a specific column name within a block */
    static ColumnChunkMetaData findColumnChunkMetadata(BlockMetaData block, String columnName) {
        for (ColumnChunkMetaData columnChunkMetaData : block.getColumns()) {
            // Column path in schema is dot-separated
            if (columnChunkMetaData.getPath().toDotString().equals(columnName)) {
                return columnChunkMetaData;
            }
        }
        return null; // Not found in this block
    }

    /**
     * Finds the PrimitiveType for a given column name from the schema.
     */
    static PrimitiveType getColumnPrimitiveType(ParquetMetadata footer, String columnName) throws IllegalArgumentException {
        // Handle potentially nested columns by splitting the name
        String[] path = columnName.split("\\.");
        Type schemaType = null;
        try {
            schemaType = footer.getFileMetaData().getSchema().getType(path);
        } catch (Exception e) {
            // Catch potential errors if path is invalid or type doesn't exist
            throw new IllegalArgumentException("Error accessing type for column '" + columnName + "' in Parquet schema.", e);
        }

        if (schemaType == null) {
            throw new IllegalArgumentException("Column '" + columnName + "' not found in Parquet schema.");
        }
        if (!schemaType.isPrimitive()) {
            throw new IllegalArgumentException("Column '" + columnName + "' is not a primitive type. Statistics comparison is currently supported only for primitives.");
        }
        return schemaType.asPrimitiveType();
    }
    /**
     * Gets a suitable comparator based on the Parquet primitive type.
     * Includes specific handling for INT96 timestamps.
     */
    @SuppressWarnings({"unchecked", "rawtypes"}) // Suppress warnings for generic comparator casting
    private static Comparator<Object> getComparatorForType(PrimitiveType type) {
        switch (type.getPrimitiveTypeName()) {
            case INT32:
            case INT64:
            case FLOAT:
            case DOUBLE:
                // Standard comparable works for numbers
                return (o1, o2) -> ((Comparable) o1).compareTo(o2);
            case BOOLEAN:
                return (o1, o2) -> ((Boolean) o1).compareTo((Boolean) o2);
            case BINARY: // Often represents Strings or other byte arrays
            case FIXED_LEN_BYTE_ARRAY:
                // Default: Compare byte arrays lexicographically
                return (o1, o2) -> ((Binary) o1).compareTo((Binary) o2);

            case INT96: // Special handling for Timestamps stored as INT96
                logger.info("INT96 type detected. Using Timestamp comparison based on parsed values.");
                // Compare objects assuming they have been parsed into java.sql.Timestamp
                return (o1, o2) -> {
                    if (!(o1 instanceof Timestamp && o2 instanceof Timestamp)) {
                        // This shouldn't happen if parsing in findMaxFromStats works correctly, but defensively handle
                        throw new IllegalArgumentException("Comparator expected Timestamps for INT96 comparison, but received: "
                                + o1.getClass().getName() + " and " + o2.getClass().getName());
                    }
                    return ((Timestamp) o1).compareTo((Timestamp) o2);
                };
            default:
                throw new IllegalArgumentException("Unsupported primitive type for comparison: " + type.getPrimitiveTypeName());
        }
    }

} // End of class

