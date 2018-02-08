package spirite.pc

import spirite.base.graphics.gl.GLEngine
import spirite.base.graphics.gl.GLImage
import spirite.base.util.glu.GLC
import spirite.base.util.linear.MutableTransform
import spirite.base.util.linear.Transform
import spirite.hybrid.HybridConfig
import spirite.pc.JOGL.JOGL.JOGLInt32Source
import sun.awt.image.IntegerInterleavedRaster
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

    val bi = gle.surfaceToBufferedImage( HybridConfig.BI_FORMAT, this.width, this.height)
    engine.setTarget(null)
    return bi
}

fun GLEngine.surfaceToBufferedImage( type: Int, width: Int, height: Int) : BufferedImage{
    val bi = when( type) {
        BufferedImage.TYPE_INT_ARGB,
        BufferedImage.TYPE_INT_ARGB_PRE -> {
            val bi = BufferedImage(width, height, type)

            val iir = bi.raster as IntegerInterleavedRaster
            val ib = IntBuffer.wrap( iir.dataStorage)

            gl.readPixels( 0, 0, width, height,
                    GLC.BGRA,
                    GLC.UNSIGNED_INT_8_8_8_8_REV,
                    JOGLInt32Source(ib))

            bi
        }
        else -> BufferedImage(1, 1, type)
    }

    // Flip Vertically
//    if( flip) {
//        val raster = bi.raster
//        var scanline1 : Any? = null
//        var scanline2 : Any? = null
//        for( i in 0 until bi.height/2) {
//            scanline1 = raster.getDataElements( 0 , i, bi.width, 1, scanline1)
//            scanline2 = raster.getDataElements( 0 , bi.height-i-1, bi.width, 1, scanline2)
//            raster.setDataElements( 0, i, bi.width, 1, scanline2)
//            raster.setDataElements( 0, bi.height-i-1, bi.width, 1, scanline1)
//        }
//    }

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