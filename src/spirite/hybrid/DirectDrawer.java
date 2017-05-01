package spirite.hybrid;

import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.Queue;

import spirite.base.image_data.RawImage;
import spirite.base.util.DataCompaction.IntQueue;
import spirite.base.util.MUtil;
import spirite.hybrid.MDebug.WarningType;
import spirite.pc.graphics.ImageBI;

public class DirectDrawer {
	public static void fill( RawImage img, int x, int y, int color) {
		if( img instanceof ImageBI) {
			fillBI( ((ImageBI) img).img, x, y, color);
		}
		else {
			MDebug.handleWarning( WarningType.UNSUPPORTED, null, "Unsupported Fill Image Type.");
		}
	}
	
	private static void fillBI( BufferedImage bi, int x, int y, int c) {
		int w = bi.getWidth();
		int h = bi.getHeight();
		int bg = bi.getRGB(x, y);

		IntQueue queue = new IntQueue();
		
		queue.add( MUtil.packInt(x, y));
		if( bg == c) return;
		
		while( !queue.isEmpty()) {
			int p = queue.poll();
			int ix = MUtil.high16(p);
			int iy = MUtil.low16(p);
			
			if( bi.getRGB(ix, iy) != bg)
				continue;
				
			bi.setRGB(ix, iy, c);

			if( ix + 1 < w) {
				queue.add( MUtil.packInt(ix+1, iy));
			}
			if( ix - 1 >= 0) {
				queue.add( MUtil.packInt(ix-1, iy));
			}
			if( iy + 1 < h) {
				queue.add( MUtil.packInt(ix, iy+1));
			}
			if( iy - 1 >= 0) {
				queue.add( MUtil.packInt(ix, iy-1));
			}
		}
	}
}
