package spirite.base.image_data.layers;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import spirite.base.brains.CacheManager.CachedImage;
import spirite.base.brains.RenderEngine.RenderSettings;
import spirite.base.brains.RenderEngine.TransformedHandle;
import spirite.base.graphics.GraphicsContext;
import spirite.base.image_data.ImageHandle;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.ImageWorkspace.BuildingImageData;
import spirite.base.image_data.ImageWorkspace.ImageCropHelper;
import spirite.base.image_data.UndoEngine.DrawImageAction;
import spirite.base.image_data.UndoEngine.UndoableAction;
import spirite.base.util.MUtil;
import spirite.base.util.glmath.Rect;
import spirite.hybrid.Globals;

public class SimpleLayer extends Layer {
	private final ImageHandle data;
	
	public SimpleLayer( ImageHandle data) {
		this.data = data;
	}
	
	public ImageHandle getData() {
		return data;
	}

	@Override
	public List<ImageHandle> getImageDependencies() {
		return Arrays.asList(data);
	}

	@Override
	public void draw(GraphicsContext gc) {
		data.drawLayer(gc,null);
	}

	@Override
	public BuildingImageData getActiveData() {
		return new BuildingImageData(data, 0, 0);
	}
	@Override
	public List<BuildingImageData> getDataToBuild(){
		return Arrays.asList( new BuildingImageData[] {new BuildingImageData(data, 0, 0)});
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
		BufferedImage image = workspace.getRenderEngine().renderImage(settings);
		
		Rectangle myBounds = new Rectangle( 0, 0, data.getWidth(), data.getHeight());
		Rectangle imgBounds = new Rectangle( x, y, image.getWidth(), image.getHeight());
		
		if( !myBounds.contains( imgBounds)) {
			Rectangle newBounds = myBounds.union(imgBounds);
			helper.offsetChange.x = newBounds.x;
			helper.offsetChange.y = newBounds.y;
			
			// Draw both images in their respective spots
			BufferedImage combination = new BufferedImage( newBounds.width, newBounds.height, Globals.BI_FORMAT);
			MUtil.clearImage(combination);
			Graphics g = combination.getGraphics();
			g.drawImage( data.deepAccess(), 0-newBounds.x, 0-newBounds.y, null);
			g.drawImage( image, x-newBounds.x, y-newBounds.y, null);
			g.dispose();
			
			helper.actions.add(workspace.getUndoEngine().createReplaceAction(data, combination));
		}
		else {
			CachedImage ci = workspace.getCacheManager().cacheImage(image, workspace.getUndoEngine());
			UndoableAction action = new DrawImageAction(workspace.new BuiltImageData(data, x, y), ci);
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
				helper.offsetChange.x = crop.dx;
				helper.offsetChange.y = crop.dy;
				return helper;
			}
		}
		return null;
	}
	
	

}
