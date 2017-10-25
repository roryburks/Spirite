package spirite.base.image_data.layers;

import java.util.List;

import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.renderer.RenderEngine.TransformedHandle;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.ImageHandle;
import spirite.base.image_data.ImageWorkspace.BuildingImageData;
import spirite.base.image_data.ImageWorkspace.ImageCropHelper;
import spirite.base.util.glmath.Rect;

public class ReferenceLayer extends Layer {
	public final Layer underlying;
	
	public ReferenceLayer( Layer underlying) {
		this.underlying = underlying;
	}

	@Override public BuildingImageData getActiveData() 
		{return underlying.getActiveData();}
	@Override public List<ImageHandle> getImageDependencies() 
		{return underlying.getImageDependencies();}
	@Override public List<BuildingImageData> getDataToBuild() 
	
		{return underlying.getDataToBuild();}
	@Override public void draw(GraphicsContext gc) 
		{underlying.draw(gc);}

	@Override public int getWidth() 
		{return underlying.getWidth();}
	@Override public int getHeight() 
		{return underlying.getHeight();}

	@Override public boolean canMerge(Node node) 
		{return underlying.canMerge(node);}

	@Override public List<TransformedHandle> getDrawList() 
		{return underlying.getDrawList();}

	@Override public LayerActionHelper merge(Node node, int x, int y) 
		{return underlying.merge(node, x, y);}

	@Override public List<Rect> getBoundList() 
		{return underlying.getBoundList();}

	@Override public LayerActionHelper interpretCrop(List<ImageCropHelper> helpers) 
		{return underlying.interpretCrop(helpers);}

	@Override public Layer logicalDuplicate() 
		{return new ReferenceLayer(underlying);}

	@Override public int getDynamicOffsetX() 
		{return underlying.getDynamicOffsetX();}
	@Override public int getDynamicOffsetY() 
		{return underlying.getDynamicOffsetY();}
}
