package `is`.xyz.mpv

import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Fast thumbnail generation using direct FFmpeg API.
 * 
 * High-performance thumbnail generation (30-80ms per thumbnail) that bypasses MPV 
 * and uses optimized FFmpeg software decoding for maximum speed.
 * 
 * Features:
 * - Quality-based scaling: 1-10 scale where 10 = original dimensions, 1 = 10% size
 * - Optimized software decoding (HW acceleration disabled for better single-frame performance)
 * - Multi-threaded frame + slice decoding
 * - Aggressive codec optimizations (skip loop filter, non-ref frames)
 * - Limited stream probing for faster initialization
 * - Bilinear scaling for good quality when downscaling
 * - Smart keyframe seeking
 * - Minimal overhead (no MPV initialization)
 * 
 * Usage:
 * ```kotlin
 * // Initialize once (in Application.onCreate)
 * FastThumbnails.initialize(applicationContext)
 * 
 * // Generate thumbnail at original dimensions (quality 10)
 * val bitmap = FastThumbnails.generate("/path/video.mp4", 10.0)
 * 
 * // Generate at 50% size (quality 5)
 * val bitmap = FastThumbnails.generate("/path/video.mp4", 10.0, quality = 5)
 * 
 * // Or async with coroutines
 * val bitmap = FastThumbnails.generateAsync("/path/video.mp4", 10.0, quality = 8)
 * ```
 */
object FastThumbnails {
    private val initialized = AtomicBoolean(false)
    
    /**
     * Initialize the fast thumbnail system.
     * Call this once before generating thumbnails (typically in Application.onCreate).
     * 
     * @param context Application context
     */
    @JvmStatic
    fun initialize(context: Context) {
        if (initialized.compareAndSet(false, true)) {
            MPVLib.setThumbnailJavaVM(context.applicationContext)
        }
    }
    
    /**
     * Check if initialized.
     */
    @JvmStatic
    fun isInitialized(): Boolean = initialized.get()
    
    /**
     * Generate thumbnail using fast FFmpeg direct API (30-80ms).
     * Uses optimized software decoding for maximum speed.
     * 
     * @param path File path or URL to the video
     * @param position Time position in seconds (default: 0.0)
     * @param quality Quality/size scale 1-10 where 10 = original dimensions, 5 = 50%, 1 = 10% (default: 10)
     * @return Bitmap thumbnail, or null if generation fails
     * @throws IllegalStateException if not initialized
     */
    @JvmStatic
    @JvmOverloads
    fun generate(
        path: String,
        position: Double = 0.0,
        quality: Int = 10
    ): Bitmap? {
        check(initialized.get()) {
            "FastThumbnails not initialized. Call initialize(context) first."
        }
        
        require(quality in 1..10) {
            "Quality must be between 1 and 10 (got $quality)"
        }
        
        return try {
            MPVLib.grabThumbnailFast(path, position, quality)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Generate thumbnail asynchronously (IO dispatcher).
     * 
     * @param path File path or URL
     * @param position Time position in seconds (default: 0.0)
     * @param quality Quality/size scale 1-10 where 10 = original dimensions (default: 10)
     * @return Bitmap thumbnail, or null
     */
    suspend fun generateAsync(
        path: String,
        position: Double = 0.0,
        quality: Int = 10
    ): Bitmap? = withContext(Dispatchers.IO) {
        generate(path, position, quality)
    }
    
    /**
     * Generate multiple thumbnails at different positions.
     * 
     * @param path File path
     * @param positions List of time positions
     * @param quality Quality/size scale 1-10 where 10 = original dimensions (default: 10)
     * @return List of bitmaps (may contain nulls)
     */
    @JvmStatic
    @JvmOverloads
    fun generateMultiple(
        path: String,
        positions: List<Double>,
        quality: Int = 10
    ): List<Bitmap?> {
        return positions.map { position ->
            generate(path, position, quality)
        }
    }
    
    /**
     * Generate multiple thumbnails asynchronously.
     * 
     * @param path File path
     * @param positions List of positions
     * @param quality Quality/size scale 1-10 where 10 = original dimensions (default: 10)
     * @return List of bitmaps
     */
    suspend fun generateMultipleAsync(
        path: String,
        positions: List<Double>,
        quality: Int = 10
    ): List<Bitmap?> = withContext(Dispatchers.IO) {
        positions.map { position ->
            generate(path, position, quality)
        }
    }
    
    /**
     * Performance benchmark helper.
     * Generates a thumbnail and measures time taken.
     * 
     * @param path File path
     * @param position Time position in seconds (default: 0.0)
     * @param quality Quality/size scale 1-10 where 10 = original dimensions (default: 10)
     * @return Pair of (bitmap, time in milliseconds)
     */
    @JvmStatic
    @JvmOverloads
    fun benchmark(path: String, position: Double = 0.0, quality: Int = 10): Pair<Bitmap?, Long> {
        val start = System.currentTimeMillis()
        val bitmap = generate(path, position, quality)
        val elapsed = System.currentTimeMillis() - start
        return Pair(bitmap, elapsed)
    }
}
