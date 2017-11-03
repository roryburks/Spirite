package spirite.base.image_data.layers;

import java.util.List;

import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.renderer.RenderEngine.TransformedHandle;
import spirite.base.image_data.GroupTree.LayerNode;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.ImageWorkspace.BuildingMediumData;
import spirite.base.image_data.ImageWorkspace.ImageCropHelper;
import spirite.base.image_data.MediumHandle;
import spirite.base.util.glmath.Rect;

public class ReferenceLayer extends Layer {
	private  LayerNode underlying;
	private Layer layer;
	
	public ReferenceLayer( LayerNode underlying) {
		this.underlying = underlying;
		this.layer = (underlying == null) ? null : underlying.getLayer();
	}
	
	/** NON UNDOABLE*/
	public void setUnderlying(LayerNode underlying) {
		this.underlying = underlying;
		this.layer = underlying.getLayer();
	}
	
	public LayerNode getUnderlying() {
		return this.underlying;
	}
	public Layer getUnderlyingLayer() {
		return layer;
	}
	

	@Override public BuildingMediumData getActiveData() 
		{return (layer == null)? null : layer.getActiveData();}
	@Override public List<MediumHandle> getImageDependencies() 
		{return (layer == null)? null : layer.getImageDependencies();}
	@Override public List<BuildingMediumData> getDataToBuild() 
	
		{return (layer == null)? null : layer.getDataToBuild();}
	@Override public void draw(GraphicsContext gc) 
		{if( layer != null)layer.draw(gc);}

	@Override public int getWidth() 
		{return (layer == null)? 0 : layer.getWidth();}
	@Override public int getHeight() 
		{return (layer == null)? 0 : layer.getHeight();}

	@Override public boolean canMerge(Node node) 
		{return (layer == null)? false : layer.canMerge(node);}

	@Override public List<TransformedHandle> getDrawList() 
		{return (layer == null)? null : layer.getDrawList();}

	@Override public LayerActionHelper merge(Node node, int x, int y) 
		{return (layer == null)? null : layer.merge(node, x, y);}

	@Override public List<Rect> getBoundList() 
		{return (layer == null)? null : layer.getBoundList();}

	@Override public LayerActionHelper interpretCrop(List<ImageCropHelper> helpers) 
		{return (layer == null)? null : layer.interpretCrop(helpers);}

	@Override public Layer logicalDuplicate() 
		{return new ReferenceLayer(underlying);}

	@Override public int getDynamicOffsetX() 
		{return (layer == null)? 0 : layer.getDynamicOffsetX();}
	@Override public int getDynamicOffsetY() 
		{return (layer == null)? 0 : layer.getDynamicOffsetY();}
}
