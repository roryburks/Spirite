package rbJvm.glow.awt

import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte
import java.awt.image.DataBufferInt


object RasterHelper
{
    fun getDataStorageFromBi(bi: BufferedImage) : Any?
    {
        /**
         * Note: because a previous version of Spirite needed several-times-a-second transfers from BI -> GL, getting
         * the data storage straight from the Raster was important so that you didn't duplicate large chunks of data
         * in java before importing it to GL.  Right now nothing uses BI -> GL conversions, so for now I'm commenting it
         * out and in the future I might just use wrapped WritableRaster functionality to get the data.
         */

        //val raster = bi.raster
        val buff = bi.raster.dataBuffer as? DataBufferByte
        if(buff != null)
            return buff.data
        val buffInt = bi.raster.dataBuffer as? DataBufferInt
        if( buffInt != null)
            return buffInt.data
        println("bad ${bi.raster.dataBuffer.javaClass}")
        return bi.getRGB(0, 0, bi.width, bi.height, null, 0, bi.width )
    }
}