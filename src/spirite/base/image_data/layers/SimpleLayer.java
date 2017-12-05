package spirite.base.image_data.layers;

import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.RawImage;
import spirite.base.graphics.renderer.RenderEngine.RenderSettings;
import spirite.base.graphics.renderer.RenderEngine.TransformedHandle;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.ImageWorkspace.BuildingMediumData;
import spirite.base.image_data.ImageWorkspace.ImageCropHelper;
import spirite.base.image_data.MediumHandle;
import spirite.base.image_data.UndoEngine.DrawImageAction;
import spirite.base.image_data.UndoEngine.UndoableAction;
import spirite.base.util.linear.Rect;
import spirite.base.util.linear.Vec2i;
import spirite.hybrid.HybridHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SimpleLayer extends Layer {
	private final MediumHandle data;
	
	public SimpleLayer( MediumHandle data) {
		this.data = data;
	}
	
	public MediumHandle getData() {
		return data;
	}

	@Override
	public List<MediumHandle> getImageDependencies() {
		return Arrays.asList(data);
	}


	@Override
	public BuildingMediumData getActiveData() {
		return new BuildingMediumData(data, 0, 0);
	}
	@Override
	public List<BuildingMediumData> getDataToBuild(){
		return Arrays.asList( new BuildingMediumData[] {new BuildingMediumData(data, 0, 0)});
	}
	@Override
	public int getWidth() {
		return data.getWidth();
	}
	@Override
	public int getHeight() {
		return data.getHeight();
	}
	@Override
	public Layer logicalDuplicate() {
		return new SimpleLayer(data.dupe());
	}
	@Override
	public List<Rect> getBoundList() {
		
		List<Rect> list = new ArrayList<>(1);
		list.add(new Rect( 0, 0, data.getWidth(), data.getHeight()));
//		list.add( bounds.intersection(rect));
		
		return list;
	}
	@Override
	public boolean canMerge(Node node) {
		return (data.getContext() != null);
	}
	@Override
	public LayerActionHelper merge(Node node, int x, int y) {
		LayerActionHelper helper = new LayerActionHelper();
		if( !canMerge(node)) return helper;

		ImageWorkspace workspace = data.getContext();	// Non-null as per canMerge
		RenderSettings settings = new RenderSettings(
				workspace.getRenderEngine().getNodeRenderTarget(node));
		RawImage image = workspace.getRenderEngine().renderImage(settings);
		
		Rect myBounds = new Rect( 0, 0, data.getWidth(), data.getHeight());
		Rect imgBounds = new Rect( x, y, image.getWidth(), image.getHeight());
		
		if( !myBounds.contains( imgBounds)) {
			Rect newBounds = myBounds.union(imgBounds);
			helper.offsetChange = new Vec2i(newBounds.x, newBounds.y);
			
			// Draw both images in their respective spots
			RawImage combination = HybridHelper.createImage( newBounds.width, newBounds.height);
			GraphicsContext gc = combination.getGraphics();
			gc.clear();
			gc.drawHandle( data, 0-newBounds.x, 0-newBounds.y);
			gc.drawImage( image, x-newBounds.x, y-newBounds.y);
//			g.dispose();
			
			helper.actions.add(workspace.getUndoEngine().createReplaceAction(data, combination));
		}
		else {
			UndoableAction action = new DrawImageAction(  new BuildingMediumData(data,x, y), image);
			helper.actions.add(action);
		}
		
		
		return helper;
	}

	@Override
	public List<TransformedHandle> getDrawList() {
		TransformedHandle renderable = new TransformedHandle();
		renderable.handle = data;
		renderable.depth = 0;
		
		return Arrays.asList( new TransformedHandle[]{renderable});
	}

	@Override
	public LayerActionHelper interpretCrop( List<ImageCropHelper> crops) {
		
		for( ImageCropHelper crop : crops) {
			if( crop.handle.equals(data)){
				LayerActionHelper helper = new  LayerActionHelper();
				helper.offsetChange = new Vec2i(crop.dx, crop.dy);
				return helper;
			}
		}
		return null;
	}

	@Override public int getDynamicOffsetX() {
		return data.getDynamicX();
	}
	@Override public int getDynamicOffsetY() {
		return data.getDynamicY();
	}
}
