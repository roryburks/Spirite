package rbJvm.glow.util

import com.jogamp.opengl.GL2
import rb.glow.gl.GLImage
import rb.glow.img.IImage
import rb.vectrix.shapes.RectI
import rbJvm.glow.awt.ImageBI
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte
import java.awt.image.DataBufferInt
import java.util.concurrent.atomic.AtomicReference

object ContentBoundsFinder {
    class UnsupportedImageTypeException(message: String) : Exception(message)

    /**
     * Returns a rectangle that represents the sub-section of the original image
     * such that as large full rectangular regions on the outside of a single
     * jcolor are removed without touching non-background data.
     *
     * @param image
     * The image to crop.
     * @param buffer
     * The amount of pixels outside of the region with non-background
     * content to preserve on each side of the image.
     * @param transparentOnly
     * If true, it only crops if the background is transparent, if
     * false it will crop any background jcolor.
     * @return
     * The RectShape of the image as it should be cropped.
     * @throws
     * UnsupportedDataTypeException if the ColorModel does not conform
     * to the a supported format
     */
    @Throws(UnsupportedImageTypeException::class)
    fun findContentBounds(raw: IImage, buffer: Int, transparentOnly: Boolean): RectI {
        val data: _ImageCropHelper

        if (raw is ImageBI) {
            val image = raw.bi


            val type = image.getType()
            when (type) {
                BufferedImage.TYPE_4BYTE_ABGR, BufferedImage.TYPE_4BYTE_ABGR_PRE -> data = _ImageCropHelperByte(image)
                BufferedImage.TYPE_INT_ARGB, BufferedImage.TYPE_INT_ARGB_PRE -> data = _ImageCropHelperInt(image)
                else -> throw UnsupportedImageTypeException("Only programmed to deal with 4-byte jcolor data.")
            }
        } else if (raw is GLImage) {
            val engine = raw.engine
            val gl = engine.gl
            val w = raw.width
            val h = raw.height

            engine.setTarget(raw)

            val intBuffer = IntArray(w*h)
            gl.readnPixels(0, 0, w, h,
                    GL2.GL_BGRA, GL2.GL_UNSIGNED_INT_8_8_8_8_REV, 4 * w * h,
                    gl.makeInt32Source(intBuffer))

            data = _ImageCropHelperInt(intBuffer, w, h)
        } else
            throw UnsupportedImageTypeException("Unsupported RawImage type")


        var x1: Int
        var y1: Int
        var x2: Int
        var y2: Int

        // Don't feel like going through and special-casing 1-size things.
        if (data.w < 2 || data.h < 2) return RectI(0, 0, data.w, data.h)

        data.setBG(0, 0)

        // Usually the background jcolor will be the top-left pixel, but
        //	sometimes it'll be the bottom-right pixel.
        // (Note all pixels in the edges share either a row or a column
        //	with one of these two pixels, so it'll be one of the two).
        // Note: this also pulls double-duty of checking the special case
        //	of the 0th row and column, which simplifies the Binary Search
        if (!data.rowIsEmpty(0) || !data.colIsEmpty(0))
            data.setBG(data.w - 1, data.h - 1)

        if (transparentOnly && !data.isBGTransparent)
            return RectI(0, 0, data.w, data.h)

        val leftRet = AtomicReference<Int>(null)
        val left = Thread { leftRet.set(data.findFirstEmptyLine((0 until data.w).toList(), false)) }
        val rightRet = AtomicReference<Int>(null)
        val right = Thread { rightRet.set(data.findFirstEmptyLine( (data.w-1 downTo 0).toList(), false)) }
        val topRet = AtomicReference<Int>(null)
        val top = Thread { topRet.set(data.findFirstEmptyLine((0 until data.h).toList(), true)) }
        val bottomRet = AtomicReference<Int>(null)
        val bottom = Thread { bottomRet.set(data.findFirstEmptyLine((data.h - 1 downTo 0).toList(), true)) }

        left.start()
        right.start()
        top.start()
        bottom.start()
        try {
            left.join()
            right.join()
            top.join()
            bottom.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        if (leftRet.get() == -1)
            return RectI(0, 0, 0, 0)
        x1 = leftRet.get()
        x2 = if (rightRet.get() == -1) data.w - 1 else data.w - 1 - rightRet.get()

        if (topRet.get() == -1)
            return RectI(0, 0, 0, 0)
        y1 = topRet.get()
        y2 = if (bottomRet.get() == -1) data.h - 1 else data.h - 1 - bottomRet.get()

        x1 = Math.max(0, x1 - buffer)
        y1 = Math.max(0, y1 - buffer)
        x2 = Math.min(data.w - 1, x2 + buffer)
        y2 = Math.min(data.h - 1, y2 + buffer)

        return RectI(x1, y1 , x2 - x1 + 1, y2 - y1 + 1)
    }

    private abstract class _ImageCropHelper {
        internal val w: Int
        internal val h: Int
        internal abstract val isBGTransparent: Boolean

        internal constructor(image: BufferedImage) {
            this.w = image.width
            this.h = image.height
        }

        internal constructor(w: Int, h: Int) {
            this.w = w
            this.h = h
        }

        // Sanity checks would be inefficient, let's just assume everything
        //	works correctly because it's already been tested.
        internal abstract fun verify(x: Int, y: Int): Boolean

        internal abstract fun setBG(x: Int, y: Int)

        // Kind of Ugly code in here, but it's a bit much to copy all that
        // code just to swap AnimationCommand and Y and wf for h

        /**
         * @return true if the line contains only BG data
         */
        private fun lineIsEmpty(num: Int, row: Boolean): Boolean {
            if (row) {
                for (x in 0 until w) {
                    if (!verify(x, num)) return false
                }
            } else {
                for (y in 0 until h) {
                    if (!verify(num, y)) return false
                }
            }
            return true
        }

        internal fun rowIsEmpty(rownum: Int): Boolean {
            return lineIsEmpty(rownum, true)
        }

        internal fun colIsEmpty(colnum: Int): Boolean {
            return lineIsEmpty(colnum, false)
        }

        internal fun findFirstEmptyLine(data: List<Int>, row: Boolean): Int {
            val size = data.size
            if (size == 0) return -1

            for (i in 0 until size) {
                if (!lineIsEmpty(data.get(i), row))
                    return i
            }
            return -1
        }
    }

    private class _ImageCropHelperByte internal constructor(image: BufferedImage) : _ImageCropHelper(image) {
        internal val pixels: ByteArray
        internal val bgcolor = ByteArray(4)

        override val isBGTransparent: Boolean
            get() = bgcolor[0].toInt() == 0

        init {
            this.pixels = (image.raster.dataBuffer as DataBufferByte).data
        }

        override fun verify(x: Int, y: Int): Boolean {
            val i = (x + y * w) * 4
            return pixels[i] == bgcolor[0] &&
                    pixels[i + 1] == bgcolor[1] &&
                    pixels[i + 2] == bgcolor[2] &&
                    pixels[i + 3] == bgcolor[3]
        }

        override fun setBG(x: Int, y: Int) {
            val i = (x + y * w) * 4
            bgcolor[0] = pixels[i]
            bgcolor[1] = pixels[i + 1]
            bgcolor[2] = pixels[i + 2]
            bgcolor[3] = pixels[i + 3]
        }
    }

    private class _ImageCropHelperInt : _ImageCropHelper {
        internal val pixels: IntArray
        internal var bgcolor: Int = 0

        override val isBGTransparent: Boolean
            get() = bgcolor.ushr(24) and 0xFF == 0

        internal constructor(image: BufferedImage) : super(image) {
            this.pixels = (image.raster.dataBuffer as DataBufferInt).data
        }

        internal constructor(data: IntArray, w: Int, h: Int) : super(w, h) {
            this.pixels = data
        }

        override fun verify(x: Int, y: Int): Boolean {
            val i = x + y * w
            return pixels[i] == bgcolor
        }

        override fun setBG(x: Int, y: Int) {
            val i = x + y * w
            bgcolor = pixels[i]
        }
    }
}