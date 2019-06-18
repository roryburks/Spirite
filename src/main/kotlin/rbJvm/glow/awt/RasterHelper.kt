package rbJvm.glow.awt

import sun.awt.image.ByteInterleavedRaster
import sun.awt.image.IntegerInterleavedRaster
import java.awt.image.BufferedImage

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

        val raster = bi.raster
        return when( raster) {
            is ByteInterleavedRaster -> {raster.dataStorage}
            is IntegerInterleavedRaster -> {raster.dataStorage}
            else -> null
        }
    }
}