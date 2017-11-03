package spirite.base.image_data.layers;

import java.util.ArrayList;
import java.util.List;

import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.renderer.RenderEngine.TransformedHandle;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.ImageWorkspace.BuildingMediumData;
import spirite.base.image_data.ImageWorkspace.ImageCropHelper;
import spirite.base.image_data.MediumHandle;
import spirite.base.util.glmath.Rect;

public class PuppetLayer extends Layer {
	public final Puppet puppet;
	public final ImageWorkspace context;
	
	private Puppet.Part selectedPart;
	
	public PuppetLayer( ImageWorkspace context) {
		this.context = context;
		this.puppet = new Puppet();
	}

	@Override
	public BuildingMediumData getActiveData() {
		if( selectedPart == null) return null;
		
		//BuildingImageData data = new BuildingImageData(handle, ox, oy)
		return null;
	}

	@Override
	public List<MediumHandle> getImageDependencies() {
		List<MediumHandle> ret = new ArrayList<>(puppet.parts.size());
		
		for( Puppet.Part part : puppet.parts)
			ret.add(part.handle);
		
		return ret;
	}

	@Override
	public List<BuildingMediumData> getDataToBuild() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void draw(GraphicsContext gc) {
		// TODO Auto-generated method stub

	}

	@Override
	public int getWidth() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getHeight() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean canMerge(Node node) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<TransformedHandle> getDrawList() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LayerActionHelper merge(Node node, int x, int y) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Rect> getBoundList() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LayerActionHelper interpretCrop(List<ImageCropHelper> helpers) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Layer logicalDuplicate() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getDynamicOffsetX() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getDynamicOffsetY() {
		// TODO Auto-generated method stub
		return 0;
	}
	
}
