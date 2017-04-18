package spirite.image_data.layers;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import spirite.brains.RenderEngine.TransformedHandle;
import spirite.graphics.GraphicsContext;
import spirite.image_data.GroupTree;
import spirite.image_data.ImageHandle;
import spirite.image_data.ImageWorkspace.BuildingImageData;
import spirite.image_data.ImageWorkspace.ImageCropHelper;
import spirite.image_data.UndoEngine.UndoableAction;

public abstract class Layer {
	public abstract BuildingImageData getActiveData();
	public abstract List<ImageHandle> getImageDependencies();
	public abstract List<BuildingImageData> getDataToBuild();
	public abstract void draw( GraphicsContext gc);
	public abstract int getWidth();
	public abstract int getHeight();
	
	public abstract boolean canMerge( GroupTree.Node node);
	
	/** Constructs a list of drawable objects assosciated with their
	 * drawDepth.
	 */
	public abstract List<TransformedHandle> getDrawList();
	
	/**
	 * @param x	the offset of the node relative to this one
	 * @param y "
	 * @return an object that contains all the data needed to create
	 * 	a composite Merge Action.
	 */
	public abstract LayerActionHelper merge(GroupTree.Node node, int x, int y);
	
	/**
	 * Returns a list of the relative bounds within the Layer where each ImageData
	 * is drawn.
	 */
	public abstract List<Rectangle> getBoundList();
	
	
	
	/**
	 * Returns a list of UndoableActions corresponding to a Crop action for 
	 * the Layer.
	 */
	public abstract LayerActionHelper interpretCrop( List<ImageCropHelper> helpers);
	
	/**
	 * Creates a logical duplicate of the Layer, creating Null-Context
	 * ImageHandles.
	 */
	public abstract Layer logicalDuplicate();
	
	
	/** 
	 * Contains all the logical information for the workspace to create
	 * actions in addition to any external changes needed.
	 */
	public static class LayerActionHelper {
		public List<UndoableAction> actions = new ArrayList<>(0);
		public Point offsetChange = new Point(0,0);
	}
}
