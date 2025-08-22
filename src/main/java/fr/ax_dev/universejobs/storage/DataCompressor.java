package fr.ax_dev.universejobs.storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * High-performance data compression utility.
 * Provides GZIP compression with caching and metrics.
 */
public class DataCompressor {
    
    private static final int MIN_COMPRESSION_SIZE = 512; // Don't compress small data
    private static final ConcurrentHashMap<String, String> compressionCache = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 1000;
    
    // Performance metrics
    private static final AtomicLong compressionTime = new AtomicLong(0);
    private static final AtomicLong decompressionTime = new AtomicLong(0);
    private static final AtomicLong bytesCompressed = new AtomicLong(0);
    private static final AtomicLong bytesDecompressed = new AtomicLong(0);
    private static final AtomicLong compressionCacheHits = new AtomicLong(0);
    
    /**
     * Compress data using GZIP with Base64 encoding.
     * 
     * @param data The data to compress
     * @return Compressed and encoded data
     */
    public static String compress(String data) {
        if (data == null || data.length() < MIN_COMPRESSION_SIZE) {
            return data; // Don't compress small data
        }
        
        // Check cache first
        String cached = compressionCache.get(data);
        if (cached != null) {
            compressionCacheHits.incrementAndGet();
            return cached;
        }
        
        long startTime = System.nanoTime();
        
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (GZIPOutputStream gzipOut = new GZIPOutputStream(baos)) {
                gzipOut.write(data.getBytes(StandardCharsets.UTF_8));
            }
            
            byte[] compressed = baos.toByteArray();
            String result = Base64.getEncoder().encodeToString(compressed);
            
            // Update metrics
            compressionTime.addAndGet(System.nanoTime() - startTime);
            bytesCompressed.addAndGet(data.length());
            
            // Cache result if beneficial
            if (compressed.length < data.length() * 0.8) { // Only cache if >20% compression
                cacheCompression(data, result);
            }
            
            return result;
            
        } catch (IOException e) {
            // Fall back to original data on compression failure
            return data;
        }
    }
    
    /**
     * Decompress data that was compressed with compress().
     * 
     * @param compressedData The compressed data
     * @return Decompressed data
     */
    public static String decompress(String compressedData) {
        if (compressedData == null) {
            return null;
        }
        
        // Check if data is actually compressed (has Base64 chars)
        if (!isCompressed(compressedData)) {
            return compressedData;
        }
        
        long startTime = System.nanoTime();
        
        try {
            byte[] compressed = Base64.getDecoder().decode(compressedData);
            
            ByteArrayInputStream bais = new ByteArrayInputStream(compressed);
            try (GZIPInputStream gzipIn = new GZIPInputStream(bais)) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len;
                while ((len = gzipIn.read(buffer)) != -1) {
                    baos.write(buffer, 0, len);
                }
                
                String result = baos.toString(StandardCharsets.UTF_8);
                
                // Update metrics
                decompressionTime.addAndGet(System.nanoTime() - startTime);
                bytesDecompressed.addAndGet(result.length());
                
                return result;
            }
            
        } catch (Exception e) {
            // Fall back to original data on decompression failure
            return compressedData;
        }
    }
    
    /**
     * Check if data appears to be compressed.
     * 
     * @param data The data to check
     * @return true if data appears compressed
     */
    private static boolean isCompressed(String data) {
        if (data.length() < 4) return false;
        
        // Check for Base64 pattern and reasonable length
        return data.matches("^[A-Za-z0-9+/]*={0,2}$") && data.length() % 4 == 0;
    }
    
    /**
     * Cache compression result with size limit.
     * 
     * @param original Original data
     * @param compressed Compressed data
     */
    private static void cacheCompression(String original, String compressed) {
        if (compressionCache.size() >= MAX_CACHE_SIZE) {
            // Simple eviction: remove random entry
            String firstKey = compressionCache.keys().nextElement();
            compressionCache.remove(firstKey);
        }
        
        compressionCache.put(original, compressed);
    }
    
    /**
     * Get compression statistics.
     * 
     * @return Map containing compression metrics
     */
    public static java.util.Map<String, Object> getStats() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        
        stats.put("compression_time_ms", compressionTime.get() / 1_000_000.0);
        stats.put("decompression_time_ms", decompressionTime.get() / 1_000_000.0);
        stats.put("bytes_compressed", bytesCompressed.get());
        stats.put("bytes_decompressed", bytesDecompressed.get());
        stats.put("cache_hits", compressionCacheHits.get());
        stats.put("cache_size", compressionCache.size());
        stats.put("cache_max_size", MAX_CACHE_SIZE);
        
        // Calculate compression ratio
        long totalIn = bytesCompressed.get();
        long totalOut = bytesDecompressed.get();
        if (totalIn > 0 && totalOut > 0) {
            stats.put("compression_ratio", (double) totalOut / totalIn);
        } else {
            stats.put("compression_ratio", 1.0);
        }
        
        return stats;
    }
    
    /**
     * Reset compression statistics.
     */
    public static void resetStats() {
        compressionTime.set(0);
        decompressionTime.set(0);
        bytesCompressed.set(0);
        bytesDecompressed.set(0);
        compressionCacheHits.set(0);
    }
    
    /**
     * Clear compression cache.
     */
    public static void clearCache() {
        compressionCache.clear();
    }
    
    /**
     * Test compression efficiency for sample data.
     * 
     * @param sampleData Sample data to test
     * @return Compression efficiency report
     */
    public static java.util.Map<String, Object> testCompression(String sampleData) {
        java.util.Map<String, Object> report = new java.util.HashMap<>();
        
        if (sampleData == null) {
            report.put("error", "Sample data is null");
            return report;
        }
        
        long originalSize = sampleData.length();
        report.put("original_size", originalSize);
        
        if (originalSize < MIN_COMPRESSION_SIZE) {
            report.put("compressed", false);
            report.put("reason", "Below minimum compression size");
            return report;
        }
        
        long startTime = System.nanoTime();
        String compressed = compress(sampleData);
        long compressionTimeNs = System.nanoTime() - startTime;
        
        startTime = System.nanoTime();
        String decompressed = decompress(compressed);
        long decompressionTimeNs = System.nanoTime() - startTime;
        
        report.put("compressed_size", compressed.length());
        report.put("compression_ratio", (double) originalSize / compressed.length());
        report.put("space_saved_percent", ((double) (originalSize - compressed.length()) / originalSize) * 100);
        report.put("compression_time_ms", compressionTimeNs / 1_000_000.0);
        report.put("decompression_time_ms", decompressionTimeNs / 1_000_000.0);
        report.put("data_integrity", sampleData.equals(decompressed));
        
        return report;
    }
}