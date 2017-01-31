package spirite.image_data.layers;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import spirite.MUtil;
import spirite.brains.CacheManager.CachedImage;
import spirite.image_data.GroupTree.Node;
import spirite.image_data.ImageHandle;
import spirite.image_data.ImageWorkspace;
import spirite.image_data.ImageWorkspace.BuildingImageData;
import spirite.image_data.RenderEngine.RenderSettings;
import spirite.image_data.RenderEngine.Renderable;
import spirite.image_data.UndoEngine.DrawImageAction;
import spirite.image_data.UndoEngine.UndoableAction;

public class SimpleLayer extends Layer {
	private final ImageHandle data;
	
	public SimpleLayer( ImageHandle data) {
		this.data = data;
	}
	
	public ImageHandle getData() {
		return data;
	}

	@Override
	public List<ImageHandle> getUsedImages() {
		return Arrays.asList(data);
	}

	@Override
	public void draw(Graphics g) {
		data.drawLayer(g);
	}

	@Override
	public BuildingImageData getActiveData() {
		return new BuildingImageData(data, 0, 0);
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
	public List<Rectangle> interpretCrop(Rectangle rect) {
		Rectangle bounds = new Rectangle(data.getWidth(), data.getHeight());
		
		List<Rectangle> list = new ArrayList<>(1);
		list.add( bounds.intersection(rect));
		
		return list;
	}

	@Override
	public boolean canMerge(Node node) {
		return (data.getContext() != null);
	}

	@Override
	public MergeHelper merge(Node node, int x, int y) {
		MergeHelper helper = new MergeHelper();
		if( !canMerge(node)) return helper;

		ImageWorkspace workspace = data.getContext();	// Non-null as per canMerge
		RenderSettings settings = new RenderSettings();
		settings.workspace = data.getContext();
		settings.node = node;
		BufferedImage image = workspace.getRenderEngine().renderImage(settings);
		
		Rectangle myBounds = new Rectangle( 0, 0, data.getWidth(), data.getHeight());
		Rectangle imgBounds = new Rectangle( x, y, image.getWidth(), image.getHeight());
		
		if( !myBounds.contains( imgBounds)) {
			Rectangle newBounds = myBounds.union(imgBounds);
			helper.offsetChange.x = newBounds.x;
			helper.offsetChange.y = newBounds.y;
			
			// Draw both images in their respective spots
			BufferedImage combination = new BufferedImage( newBounds.width, newBounds.height, BufferedImage.TYPE_INT_ARGB);
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
	public List<Renderable> getDrawList() {
		Renderable renderable = new Renderable() {
			@Override
			public void draw(Graphics g) {
				SimpleLayer.this.draw(g);
			}
		};
		renderable.depth = 0;
		
		return Arrays.asList( new Renderable[]{renderable});
	}
	
	

}
