package spirite.pc

import jspirite.rasters.RasterHelper
import spirite.base.graphics.gl.GLEngine
import spirite.base.graphics.gl.GLImage
import spirite.base.util.glu.GLC
import spirite.base.util.linear.MutableTransform
import spirite.base.util.linear.Transform
import spirite.pc.JOGL.JOGL.JOGLInt32Source
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

fun GLEngine.surfaceToBufferedImage( type: Int, width: Int, height: Int) : BufferedImage{
    val bi = when( type) {
        BufferedImage.TYPE_INT_ARGB,
        BufferedImage.TYPE_INT_ARGB_PRE -> {
            val bi = BufferedImage(width, height, type)

            val internalStorage = RasterHelper.GetDataStorageFromBi(bi) as IntArray
            val ib = IntBuffer.wrap( internalStorage)

            getGl().readPixels( 0, 0, width, height,
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
fun Transform.toAT(): AffineTransform {
    return AffineTransform(
            this.m00, this.m10, this.m01,
            this.m11, this.m02, this.m12)
}

/** Converts an AffineTransform to a MatTrans  */
fun AffineTransform.toMT( ): MutableTransform {
    return MutableTransform(
            this.scaleX.toFloat(), this.shearX.toFloat(), this.translateX.toFloat(),
            this.shearY.toFloat(), this.scaleY.toFloat(), this.translateY.toFloat())
}