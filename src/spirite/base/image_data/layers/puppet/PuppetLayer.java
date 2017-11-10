package spirite.base.image_data.layers.puppet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import spirite.base.graphics.renderer.RenderEngine.TransformedHandle;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.ImageWorkspace.BuildingMediumData;
import spirite.base.image_data.ImageWorkspace.ImageCropHelper;
import spirite.base.image_data.layers.Layer;
import spirite.base.image_data.MediumHandle;
import spirite.base.util.glmath.Rect;

public class PuppetLayer extends Layer {
	public final Puppet puppet;
	public final ImageWorkspace context;
	
	MediumHandle __D__medium;
	
	private Puppet.Part selectedPart;
	
	public PuppetLayer( ImageWorkspace context, MediumHandle firstMedium) {
		this.context = context;
		this.puppet = new Puppet();
		
		__D__medium = firstMedium;
	}

	@Override
	public BuildingMediumData getActiveData() {
		return new BuildingMediumData(__D__medium);
	}

	@Override
	public List<MediumHandle> getImageDependencies() {
		List<MediumHandle> ret = new ArrayList<>();
		
		ret.add(__D__medium);
		
		ret.addAll(puppet.getDependencies());
		
		return ret;
	}

	@Override
	public List<BuildingMediumData> getDataToBuild() {
		return new ArrayList<BuildingMediumData>(0);
	}


	// TODO
	@Override public int getWidth() {return 0;}
	@Override public int getHeight() {return 0;}

	// Probably will never be implemented
	@Override public boolean canMerge(Node node) {return false;}
	@Override public LayerActionHelper merge(Node node, int x, int y) {return null;}

	@Override
	public List<TransformedHandle> getDrawList() {
		TransformedHandle renderable = new TransformedHandle();
		renderable.handle = __D__medium;
		renderable.depth = 0;
			
		return Arrays.asList( new TransformedHandle[]{renderable});
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
