package rbJvm.glow.awt

import rb.glow.gl.GLC
import rb.glow.gl.GLImage
import rb.glow.gle.IGLEngine
import rb.vectrix.linear.ITransformF
import rb.vectrix.linear.MutableTransformF
import rbJvm.glow.jogl.JOGL.JOGLInt32Source
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.nio.IntBuffer

fun BufferedImage.deepCopy() = BufferedImage(
    this.colorModel,
    this.copyData(null),
    this.isAlphaPremultiplied,
    null)

fun GLImage.toBufferedImage() : BufferedImage {
    val gle = this.engine
    gle.setTarget( this)

    val format = if( this.premultiplied) BufferedImage.TYPE_INT_ARGB_PRE else BufferedImage.TYPE_INT_ARGB

    val bi = gle.surfaceToBufferedImage( format, this.width, this.height)
    //gle.setTarget(null)   // Shouldn't be necessary
    return bi
}

fun IGLEngine.surfaceToBufferedImage(type: Int, width: Int, height: Int) : BufferedImage{
    val bi = when( type) {
        BufferedImage.TYPE_INT_ARGB,
        BufferedImage.TYPE_INT_ARGB_PRE -> {
            val bi = BufferedImage(width, height, type)

            val internalStorage = RasterHelper.getDataStorageFromBi(bi) as IntArray
            val ib = IntBuffer.wrap( internalStorage)

            gl.readPixels( 0, 0, width, height,
                GLC.BGRA,
                GLC.UNSIGNED_INT_8_8_8_8_REV,
                JOGLInt32Source(ib))

            bi
        }
        else -> BufferedImage(1, 1, type)
    }

    return bi
}

/** Converts a MatTrans to an AffineTransform  */
fun ITransformF.toAT(): AffineTransform {
    return AffineTransform(
        this.m00f, this.m10f, this.m01f,
        this.m11f, this.m02f, this.m12f)
}

/** Converts an AffineTransform to a MatTrans  */
fun AffineTransform.toMT( ): MutableTransformF {
    return MutableTransformF(
        this.scaleX.toFloat(), this.shearX.toFloat(), this.translateX.toFloat(),
        this.shearY.toFloat(), this.scaleY.toFloat(), this.translateY.toFloat())
}