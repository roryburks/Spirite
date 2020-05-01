package rb.glow

import rb.glow.color.Color


interface IImage {
    /** Gets the Width of the underlying image.  */
    val width: Int

    /** Gets the Height of the underlying image.  */
    val height: Int

    /** If true, the image is oriented with the Y-axis going upward, with
     * 0,0 at the bottom-left.  If false, the image is oriented with the
     * Y-axis goind downward, with 0,0 at the top-left.
     */
    // Why would an image care?  It's just data.
    //val isGLOriented: Boolean

    /** Gets the amount of Bytes that the RawImage is using (generally
     * width*height*bytesPerPixel).  This should only be used to try and
     * approximate memory usage for memory management/UI feedback. */
    val byteSize: Int

    /** Creates a duplicate of this image that can be modified without altering
     * the original.
     */
    fun deepCopy(): RawImage

    /**
     * Gets the Color data at the given point in nonGL, top-to-bottom format
     * (point 0,0 would be the top left).
     *
     * Note: Though you could use getJcolor().argb, this can often be more efficient
     *
     * @param x AnimationCommand coordinate
     * @param y Y coordinate (top-to-bottom format)
     * @return an integer packed in ARGB form
     *  * bits 24-31: Alpha
     *  * bits 16-23: Red
     *  * bits 8-15: Green
     *  * bits 0-7: Blue
     */
    fun getARGB(x: Int, y: Int): Int = getColor(x,y).argb32
    fun getColor(x: Int, y: Int): Color



    val byteStream: Sequence<Byte> get() = ByteWalker(this)
}
private class ByteWalker( val image: IImage) : Sequence<Byte> {
    override fun iterator(): Iterator<Byte>  = Iter()

    private inner class Iter : Iterator<Byte> {
        var x = 0
        var y = 0
        var rgba = 0
        var color : Int = 0

        override fun hasNext() = y >= image.height

        override fun next(): Byte {
            if( rgba == 0) {
                color = image.getARGB(x,y)
            }

            val toReturn = when(rgba) {
                0 -> color
                1 -> color shr 8
                2 -> color shr 16
                3 -> color shr 24
                else -> 0
            }.toByte()

            if(++rgba == 4) {
                rgba = 0
                if( ++x == image.width) {
                    x = 0
                    ++y
                }
            }
            return toReturn
        }

    }

}
