package spirite.base.graphics


interface IImage {
    /** Gets the Width of the underlying image.  */
    val width: Int

    /** Gets the Height of the underlying image.  */
    val height: Int

    /** If true, the image is oriented with the Y-axis going upward, with
     * 0,0 at the bottom-left.  If false, the image is oriented with the
     * Y-axis goind downward, with 0,0 at the top-left.
     */
    val isGLOriented: Boolean

    /** Gets the amount of Bytes that the RawImage is using (generally
     * width*height*bytesPerPixel).  This should only be used to try and
     * approximate memory usage for memory management/UI feedback. */
    val byteSize: Int

    /** Flushes the image, marking it as no longer being used, allowing it to
     * free up underlying resources.
     *
     * NOTE: it is not guaranteed that flush will ever be called, so if the
     * image is using native resources that need to be de-allocated, be sure
     * to override finalize.
     */
    fun flush()

    /** Creates a duplicate of this image that can be modified without altering
     * the original.
     */
    fun deepCopy(): RawImage

    /**
     * Gets the Color data at the given point in nonGL, top-to-bottom format
     * (point 0,0 would be the top left).
     *
     * @param x X coordinate
     * @param y Y coordinate (top-to-bottom format)
     * @return an integer packed in ARGB form
     *  * bits 24-31: Alpha
     *  * bits 16-23: Red
     *  * bits 8-15: Green
     *  * bits 0-7: Blue
     */
    fun getRGB(x: Int, y: Int): Int
}