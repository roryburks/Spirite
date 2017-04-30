package spirite.base.file;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;

import spirite.base.brains.RenderEngine.TransformedHandle;
import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.awt.AWTContext;
import spirite.base.image_data.ImageHandle;
import spirite.base.image_data.GroupTree.GroupNode;
import spirite.base.image_data.GroupTree.LayerNode;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.animation_data.FixedFrameAnimation;
import spirite.base.image_data.animation_data.FixedFrameAnimation.AnimationLayer;
import spirite.base.image_data.animation_data.FixedFrameAnimation.Marker;
import spirite.base.image_data.animation_data.FixedFrameAnimation.AnimationLayer.Frame;
import spirite.base.image_data.layers.Layer;
import spirite.base.image_data.layers.SimpleLayer;
import spirite.base.util.GifSequenceWriter;
import spirite.base.util.MUtil;
import spirite.base.util.RectanglePacker;
import spirite.base.util.RectanglePacker.PackedRectangle;
import spirite.base.util.glmath.Rect;
import spirite.hybrid.HybridHelper;
import spirite.hybrid.HybridUtil;
import spirite.hybrid.HybridUtil.UnsupportedImageTypeException;
import spirite.pc.graphics.ImageBI;

/**
 * AnimIO is a container for static methods that export Animations of various
 * formats into various file formats.
 * 
 * @author RoryBurks
 */
public class AnimIO {
	/**
	 * Exports the animation into a PNG wile with each frame tiled one after the other.
	 */
	public static void exportAnimationSheet( FixedFrameAnimation animation, File file) 
			throws IOException 
	{
		int width = 0;
		int height = 0;


		for( AnimationLayer layer : animation.getLayers()) {
			for( Frame frame : layer.getFrames()) {
				if( frame.getMarker() == Marker.FRAME) {
					width = Math.max( width, frame.getLayerNode().getLayer().getWidth());
					height = Math.max( height, frame.getLayerNode().getLayer().getHeight());
				}
			}
		}
		
		int c = (int)Math.floor(animation.getEndFrame());
		
		BufferedImage bi = new BufferedImage(width*c, height, HybridHelper.BI_FORMAT);
		
		Graphics2D g = (Graphics2D) bi.getGraphics();
		GraphicsContext gc = new AWTContext(g, bi.getWidth(), bi.getHeight());
		MUtil.clearImage(bi);
		g.translate(-width, 0);
		for( int i=0; i<c; ++i) {
			g.translate(width, 0);
			animation.drawFrame(gc, i);
		}
		g.dispose();
		
		ImageIO.write(bi, "png", file);
	}
	
	/**
	 * Exports a given GroupNode into an animated GIF with the to the given file
	 * animated with the given FPS.
	 * 
	 * NOTE: This method is extremely experimental and only works for SimpleLayers
	 * as of now.
	 */
	public static void exportGroupGif( GroupNode group, File file, float fps) 
			throws FileNotFoundException, IOException 
	{
		List<BufferedImage> biList = new ArrayList<>();
		for( Node node : group.getChildren()) {
			if( node instanceof LayerNode ) {
				Layer l = ((LayerNode) node).getLayer();
				if( l instanceof SimpleLayer) {
					biList.add( ((ImageBI)l.getActiveData().handle.deepAccess()).img);
				}
			}
		}
		
		ImageOutputStream ios = new FileImageOutputStream(file);
		
		GifSequenceWriter gsw = new GifSequenceWriter(
				ios, 
				biList.get(0).getType(), 
				(int)(1000 / fps), 
				true);
		
		for( BufferedImage bi : biList) {
			gsw.writeToSequence(bi);
		}
		
		gsw.close();
		ios.close();
	}
	
	/**
	 * Exports the given FixedFrameAnimation into an .aaf file with a corresponding 
	 * PNG.
	 */
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
			Rect bounds = null;
			try {
				bounds = HybridUtil.findContentBounds(handle.deepAccess(), 0, true);
			} catch (UnsupportedImageTypeException e) {
				e.printStackTrace();
			}
			
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
			
				
				BufferedImage bi = new BufferedImage( bounds.width, bounds.height, HybridHelper.BI_FORMAT);
				GraphicsContext gc = new AWTContext( bi);
				gc.drawImage(handle.deepAccess(), -bounds.x, -bounds.y);
//				g.dispose();
				
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
				pr.width, pr.height, HybridHelper.BI_FORMAT);
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
