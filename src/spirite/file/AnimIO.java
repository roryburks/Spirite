package spirite.file;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import mutil.RectanglePacker;
import mutil.RectanglePacker.PackedRectangle;
import spirite.MUtil;
import spirite.brains.RenderEngine.TransformedHandle;
import spirite.image_data.ImageHandle;
import spirite.image_data.animation_data.FixedFrameAnimation;

public class AnimIO {
	public static void exportFFAnim( FixedFrameAnimation animation, File file) 
			throws IOException 
	{
		List<ImageHandle> handles = new ArrayList<>();
		List<AAFFrame> frames = new ArrayList<>();
		
		// Step 0: Convert the given filename to a uniform format
		String name = file.getName();
		if( name.endsWith(".png")) name = name.substring(0, name.length()-".png".length());
		else if( name.endsWith(".aaf")) name = name.substring(0, name.length()-".aaf".length());
		name = file.getParent() +"/"+ name;
		
		// Step 1: Interpret animation data as AAF Data
		for( int i=0; i < animation.getEnd() ; ++i) {
			List<TransformedHandle> drawList = animation.getDrawListForFrame(i);
			AAFFrame aframe = new AAFFrame();
			
			for( TransformedHandle th : drawList) {
				AAFFrame.AAFSubFrame asf = new AAFFrame.AAFSubFrame();
				int index = handles.indexOf(th.handle);
				if( index == -1) {
					asf.id = handles.size();
					handles.add(th.handle);
				}
				else
					asf.id = index;
				
				// Probably not the best way to do it
				asf.ox = (int) th.trans.getTranslateX();
				asf.oy = (int) th.trans.getTranslateY();
				
				aframe.frames.add(asf);
			}
			frames.add(aframe);
		}

		// Step 2: Crop the image to only their used bounds
		List<CroppedImage> images = new ArrayList<>(handles.size());
		List<Dimension> toPack = new ArrayList<>(images.size());
		for( ImageHandle handle : handles) {
			Rectangle bounds = MUtil.findContentBounds(handle.deepAccess(), 0, true);

			
			if( bounds == null || bounds.isEmpty()) {
				images.add(null);
				toPack.add(null);
			}
			else {
				int ox = 0;
				int oy = 0;
				if( handle.isDynamic()) {
					ox = handle.getDynamicX();
					oy = handle.getDynamicY();
				}
				
				BufferedImage bi = new BufferedImage( bounds.width, bounds.height, BufferedImage.TYPE_INT_ARGB);
				Graphics g = bi.getGraphics();
				g.drawImage(handle.deepAccess(), -bounds.x, -bounds.y, null);
				g.dispose();
				
				CroppedImage ci = new CroppedImage();
				ci.bi = bi;
				ci.ox = bounds.x + ox;
				ci.oy = bounds.y + oy;
				
				images.add(ci);
				toPack.add(new Dimension(bounds.width,bounds.height));
			}
		}
		
		// Step 3: Pack the Rectangles in a given area and save to a png
		PackedRectangle pr = RectanglePacker.modifiedSleatorAlgorithm(toPack);
		for( Rectangle r : pr.packedRects) {
			for( CroppedImage ci : images) {
				if( ci != null && ci.rectInImage == null &&
					ci.bi.getWidth() == r.width && ci.bi.getHeight() == r.height) 
				{
					ci.rectInImage = r;
					break;
				}
			}
		}
		
		BufferedImage output_bi = new BufferedImage(
				pr.width, pr.height, BufferedImage.TYPE_INT_ARGB);
		Graphics g = output_bi.getGraphics();
		for( CroppedImage ci : images) {
			if( ci != null && ci.rectInImage != null) {
				g.drawImage(ci.bi, ci.rectInImage.x, ci.rectInImage.y, null);
			}
		}
		g.dispose();
		ImageIO.write( output_bi, "png", new File(name+".png"));

		// Step 4: Write the Assosciated AAF file
		RandomAccessFile ra = new RandomAccessFile(new File(name+".aaf"),"rw");
		
		// a: Header
		// [4] : Version
		ra.writeInt(SaveLoadUtil.AAF.version);
		
		// b: Animation Info
		// [2] : Number of Animations
		ra.writeShort(1);
		{
			// Per Animation (once):
			// [n] : Animation Name
			ra.write(SaveLoadUtil.strToByteArrayUTF8(animation.getName()));
			// [2] : Number of Frames
			ra.writeShort(frames.size());

			for( AAFFrame af : frames) {
				// Per Frame:
				// [1] : number of SubFrames (SubFrames past 255 are ignored)
				ra.writeByte(Math.min(0xff, af.frames.size()));
				
				for( int i=0; i<af.frames.size() && i < 0xff; ++i) {
					AAFFrame.AAFSubFrame sub = af.frames.get(i);
					CroppedImage ci = images.get(sub.id);
					if( ci == null) ci = new CroppedImage();
					
					// Per Subframe:
					// [2] SpriteID
					ra.writeShort(sub.id);
					// [2] : X Offset
					ra.writeShort(sub.ox + ci.ox);
					// [2] : Y Offset
					ra.writeShort(sub.oy + ci.oy);
				}
			}
		}
		
		// c: Packed Sprite Info
		// [2] : Number of packed Sprites
		ra.writeShort( images.size());
		for( CroppedImage ci : images) {
			// Per Sprite:
			// 8 : (x,y,w,h) : Rectangle describing the frame's location
			if( ci == null || ci.rectInImage == null) {
				ra.writeLong(0x0);
			}
			else {
				ra.writeShort(ci.rectInImage.x);
				ra.writeShort(ci.rectInImage.y);
				ra.writeShort(ci.rectInImage.width);
				ra.writeShort(ci.rectInImage.height);
			}
		}
		ra.close();
	}
	private static class CroppedImage {
		int ox, oy;
		BufferedImage bi;
		Rectangle rectInImage;
	}
	private static class AAFFrame {
		ArrayList<AAFSubFrame> frames = new ArrayList<>();
		static class AAFSubFrame {
			int id;
			int ox, oy;
		}
	}
}
