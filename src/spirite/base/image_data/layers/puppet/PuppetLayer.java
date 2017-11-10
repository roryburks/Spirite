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
	public final IPuppet puppet;
	public final ImageWorkspace context;
	
	private IPuppet.IPart selectedPart;
	
	public PuppetLayer( ImageWorkspace context, MediumHandle firstMedium) {
		this.context = context;
		this.puppet = new BasePuppet(firstMedium);
		
		usingBase = true;

		selectedPart = puppet.getParts().get(0);
	}
	public PuppetLayer( ImageWorkspace context, BasePuppet base) {
		this.context = context;
		this.puppet = new Puppet(base);
		
		usingBase = false;
		selectedPart = puppet.getParts().get(0);
	}
	
	
	// =========
	// ==== Toggleable Properties
	boolean usingBase = true;
	boolean skeletonVisible = false;
	
	public boolean isBaseOnly() {return puppet instanceof BasePuppet;}
	public boolean isUsingBase() {return usingBase;}
	public void setUsingBase( boolean using) {
		if( !isBaseOnly()) {
			usingBase = using;
			context.triggerFlash();
		}
	}
	
	public boolean isSkeletonVisible() {return skeletonVisible;}
	public void setSkeletonVisible(boolean visible) {
		skeletonVisible = visible;
		context.triggerFlash();
	}
	public BasePuppet getBase() {
		return puppet.getBase();
	}

	// ==========
	// ==== DERIVED
	@Override
	public BuildingMediumData getActiveData() {
		return selectedPart.buildData();
		//return new BuildingMediumData(__D__medium);
	}

	@Override
	public List<MediumHandle> getImageDependencies() {
		List<MediumHandle> ret = new ArrayList<>();
		
		//ret.add(__D__medium);
		
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
		if( puppet instanceof BasePuppet || !usingBase) 
			return puppet.getDrawList();
		
//		TransformedHandle renderable = new TransformedHandle();
//		renderable.handle = __D__medium;
//		renderable.depth = 0;
			
		return Arrays.asList( new TransformedHandle[0]);
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
