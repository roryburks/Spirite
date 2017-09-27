package spirite.base.image_data.layers;

import java.util.List;

import spirite.base.brains.RenderEngine.TransformedHandle;
import spirite.base.graphics.GraphicsContext;
import spirite.base.image_data.Animation;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.ImageHandle;
import spirite.base.image_data.ImageWorkspace.BuildingImageData;
import spirite.base.image_data.ImageWorkspace.ImageCropHelper;
import spirite.base.util.glmath.Rect;

/**
 * AnimationLayer is not a 
 */
public class AnimationLayer extends Layer {
	private Animation animation;
	
	public AnimationLayer( Animation animation) {
		this.animation = animation;
	}

	@Override
	public BuildingImageData getActiveData() {
		throw new RuntimeException("Don't Implement This");
	}

	@Override
	public List<ImageHandle> getImageDependencies() {
		throw new RuntimeException("Don't Implement This");
	}

	@Override
	public List<BuildingImageData> getDataToBuild() {
		throw new RuntimeException("Don't Implement This");
	}

	@Override
	public void draw(GraphicsContext gc) {
		animation.drawFrame(gc, 0);
	}

	@Override
	public int getWidth() {
		//animation.getWidth();
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getHeight() {
		//animation.getHeight();
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean canMerge(Node node) {
		return false;
	}

	@Override
	public List<TransformedHandle> getDrawList() {
		List<TransformedHandle> list = animation.getDrawList(0);
		return list;
	}

	@Override
	public LayerActionHelper merge(Node node, int x, int y) {
		return null;
	}

	@Override
	public List<Rect> getBoundList() {
		return null;
	}

	@Override
	public LayerActionHelper interpretCrop(List<ImageCropHelper> helpers) {
		return null;
	}

	@Override
	public Layer logicalDuplicate() {
		return null;
	}

	@Override
	public int getDynamicOffsetX() {
		return 0;
	}

	@Override
	public int getDynamicOffsetY() {
		return 0;
	}

}
