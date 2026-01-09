package `is`.xyz.mpv

/**
 * Quality preset for thumbnail generation.
 * 
 * Each quality level is optimized for different use cases:
 * - FAST: Optimized for super-fast extraction, lower quality but usable
 * - NORMAL: Balanced quality and speed (default behavior)
 * - HQ: High quality thumbnails, optimized for best visual fidelity
 */
enum class ThumbnailQuality(val value: Int) {
    /**
     * Fast thumbnail extraction with optimized settings for speed.
     * Uses:
     * - SWS_FAST_BILINEAR scaling (fastest)
     * - Lower quality settings
     * - Minimal frame analysis
     * 
     * Best for: Quick previews, thumbnail strips, large batch operations
     */
    FAST(0),
    
    /**
     * Normal quality with balanced speed and quality.
     * Uses:
     * - SWS_POINT scaling (good balance)
     * - Standard quality settings
     * 
     * Best for: General purpose thumbnails, default usage
     */
    NORMAL(1),
    
    /**
     * High quality thumbnails optimized for visual fidelity.
     * Uses:
     * - SWS_LANCZOS scaling (highest quality)
     * - Best quality settings
     * - More precise frame selection
     * 
     * Best for: Snapshots, high-res previews, poster images
     */
    HQ(2);
    
    companion object {
        /**
         * Get quality level from integer value.
         */
        @JvmStatic
        fun fromValue(value: Int): ThumbnailQuality {
            return values().find { it.value == value } ?: NORMAL
        }
    }
}
