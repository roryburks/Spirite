package jspirite.rasters;

import sun.awt.image.ByteInterleavedRaster;
import sun.awt.image.IntegerInterleavedRaster;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

public class RasterHelper {
    public static Object GetDataStorageFromBi(BufferedImage bi) {
        WritableRaster raster = bi.getRaster();

        if( raster instanceof ByteInterleavedRaster)
        {
            return ((ByteInterleavedRaster) raster).getDataStorage();
        }
        if( raster instanceof IntegerInterleavedRaster)
        {
            return  ((IntegerInterleavedRaster) raster).getDataStorage();
        }

        return null;
    }
}
