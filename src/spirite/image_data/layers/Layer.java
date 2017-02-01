package spirite.image_data.layers;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import spirite.brains.RenderEngine.Renderable;
import spirite.image_data.GroupTree;
import spirite.image_data.ImageHandle;
import spirite.image_data.ImageWorkspace.BuildingImageData;
import spirite.image_data.UndoEngine.UndoableAction;

public abstract class Layer {
	public abstract BuildingImageData getActiveData();
	public abstract List<ImageHandle> getUsedImages();
	public abstract void draw( Graphics g);
	public abstract int getWidth();
	public abstract int getHeight();
	
	public abstract boolean canMerge( GroupTree.Node node);
	
	/** Constructs a list of drawable objects assosciated with their
	 * drawDepth.
	 */
	public abstract List<Renderable> getDrawList();
	
	/**
	 * @param x	the offset of the node relative to this one
	 * @param y "
	 * @return an object that contains all the data needed to create
	 * 	a composite Merge Action.
	 */
	public abstract MergeHelper merge(GroupTree.Node node, int x, int y);
	
	/**
	 * Given a proposed Cropping region, returns a list corresponding
	 * to which areas of the ImageData it uses should be cropped.
	 */
	public abstract List<Rectangle> interpretCrop(Rectangle rect);
	
	/**
	 * Creates a logical duplicate of the Layer, creating Null-Context
	 * ImageHandles.
	 */
	public abstract Layer logicalDuplicate();
	
	
	/** 
	 * Contains all the logical information for the workspace to create
	 * a MergeAction based on the desired merge behavior of the layer.
	 */
	public static class MergeHelper {
		public List<UndoableAction> actions = new ArrayList<>();
		public Point offsetChange = new Point(0,0);
	}
}
