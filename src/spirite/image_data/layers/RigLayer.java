package spirite.image_data.layers;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import spirite.image_data.GroupTree.LayerNode;
import spirite.image_data.GroupTree.Node;
import spirite.image_data.ImageHandle;
import spirite.image_data.ImageWorkspace;
import spirite.image_data.UndoEngine.NullAction;
import spirite.image_data.UndoEngine.UndoableAction;


/**
 * A Rig is composed of multiple Images arranged in parts, each part having an 
 * offset and possibly more.
 * 
 * 
 * @author Rory Burks
 *
 */
public class RigLayer extends Layer
{
	private final List<Part> parts = new ArrayList<>();
	private Part active = null;
	
	public RigLayer( ImageHandle handle) {
		active = new Part(handle);
		parts.add( active);
	}
	
	public class Part {
		private final ImageHandle handle;
		private int ox, oy;
		// TODO: Make this global, not just local, i.e. having a depth of -1 will draw
		//	before everything with a greater depth, even outside of this rig 
		//	(but probably limited to the same group?)
		private int depth;	
		private String partName;
		
		private Part(ImageHandle handle) {
			this.handle = handle;
		}
		
		public int getDepth() {
			return depth;
		}
		public void setDepth(int newDepth) {
			if ( depth != newDepth) {
				depth = newDepth;
				
				parts.sort( new Comparator<Part>() {
					@Override
					public int compare(Part o1, Part o2) {
						return o1.depth - o2.depth;
					}
				});
			}
		}
	}
	
	
	// :::: Unique Methods
	public void setActiveData(Part part) {
		if( parts.contains(part))
			active = part;
	}
	
	public List<Part> getParts() {
		return new ArrayList<>(parts);
	}
	
	public UndoableAction createAddPartAction(BufferedImage image, int ox, int oy, int depth) {
		ImageWorkspace context = getContext();
		if( context == null)
			return null;
		
		Part part = new Part(context.importData(image));
		part.ox = ox;
		part.oy = oy;
		part.depth = depth;
		
		
		return new AddPartAction( part);
	}
	
	private ImageWorkspace getContext() {
		if( parts.isEmpty())
			return null;
		
		return parts.get(0).handle.getContext();
	}
	
	public class AddPartAction extends NullAction {
		private final Part added;
		
		AddPartAction( Part part) {
			added = part;
		}
		
		public Part getPart() {
			return added;
		}
		
		@Override
		protected void performAction() {
			parts.add( added);
			active = added;
			parts.sort( new Comparator<Part>() {
				@Override
				public int compare(Part o1, Part o2) {
					return o1.depth - o2.depth;
				}
			});
		}

		@Override
		protected void undoAction() {
			parts.remove(added);
		}
	}
	
	
	// :::: Layer
	@Override
	public ImageHandle getActiveData() {
		if( active == null)
			return null;
		
		return active.handle;
	}

	@Override
	public List<ImageHandle> getUsedImages() {
		List<ImageHandle> handles = new ArrayList<>( parts.size());
		for( Part part : parts) {
			handles.add(part.handle);
		}
		return handles;
	}

	@Override
	public void draw(Graphics g) {
		// Note: Parts are already pre-sorted by depth when they are added and when
		//	depth is changed.
		Graphics2D g2 = (Graphics2D)g;
		
		AffineTransform trans = g2.getTransform();
		
		for( Part part : parts) {
			g2.translate(part.ox, part.oy);
			g2.setTransform(trans);
			part.handle.drawLayer(g);
		}
	}

	@Override
	public int getWidth() {
		int x1 = 0;
		int x2 = 0;
		
		for( Part part : parts) {
			if( part.ox < x1) x1 = part.ox;
			
			int px2 = part.ox + part.handle.getWidth();
			if( px2 > x2) x2 = px2;
		}
		
		return x2 - x1;
	}

	@Override
	public int getHeight() {
		int y1 = 0;
		int y2 = 0;
		
		for( Part part : parts) {
			if( part.oy < y1) y1 = part.oy;
			
			int py2 = part.oy + part.handle.getHeight();
			if( py2 > y2) y2 = py2;
		}
		return y2 - y1;
	}

	@Override
	public boolean canMerge(Node node) {
		return node instanceof LayerNode;
	}

	@Override
	public MergeHelper merge(Node node, int x, int y) {
		Layer layer = ((LayerNode)node).getLayer();
		if( layer instanceof RigLayer) {
			RigLayer other = (RigLayer)layer;
	
			// TODO:
//			Merge
//			for( )
		}
		
		return null;
	}

	@Override
	public List<Rectangle> interpretCrop(Rectangle rect) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Layer logicalDuplicate() {
		// TODO
		return new RigLayer( parts.get(0).handle);
	}

}
