package spirite.base.image_data.layers;

import java.util.ArrayList;
import java.util.List;

import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.renderer.RenderEngine.TransformedHandle;
import spirite.base.image_data.GroupTree;
import spirite.base.image_data.ImageWorkspace.BuildingMediumData;
import spirite.base.image_data.ImageWorkspace.ImageCropHelper;
import spirite.base.image_data.MediumHandle;
import spirite.base.image_data.UndoEngine.UndoableAction;
import spirite.base.image_data.mediums.IMedium;
import spirite.base.image_data.mediums.drawer.IImageDrawer;
import spirite.base.util.linear.Rect;
import spirite.base.util.linear.Vec2i;

public abstract class Layer {
	public abstract BuildingMediumData getActiveData();
	public IImageDrawer getDrawer(BuildingMediumData building, IMedium medium) {
		return medium.getImageDrawer(building);
	}
	public abstract List<MediumHandle> getImageDependencies();
	public abstract List<BuildingMediumData> getDataToBuild();
	public final void draw( GraphicsContext gc) {
		List<TransformedHandle> drawList = getDrawList();
		
		for( TransformedHandle th : drawList) {
			th.handle.drawLayer(gc, th.trans, gc.getComposite(), th.alpha);
		}
	}
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
	public abstract List<Rect> getBoundList();
	
	
	
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

	public abstract int getDynamicOffsetX();
	public abstract int getDynamicOffsetY();
	
	/** 
	 * Contains all the logical information for the workspace to create
	 * actions in addition to any external changes needed.
	 */
	public static class LayerActionHelper {
		public List<UndoableAction> actions = new ArrayList<>(0);
		public Vec2i offsetChange = new Vec2i(0,0);
	}
}
