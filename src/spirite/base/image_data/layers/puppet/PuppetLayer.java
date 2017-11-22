package spirite.base.image_data.layers.puppet;

import java.util.ArrayList;
import java.util.List;

import spirite.base.file.LoadEngine.PuppetPartInfo;
import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.renderer.RenderEngine.TransformedHandle;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.ImageWorkspace.BuildingMediumData;
import spirite.base.image_data.ImageWorkspace.ImageCropHelper;
import spirite.base.image_data.MediumHandle;
import spirite.base.image_data.layers.Layer;
import spirite.base.image_data.layers.puppet.IPuppet.IPart;
import spirite.base.image_data.mediums.IMedium;
import spirite.base.image_data.mediums.drawer.BaseSkeletonDrawer;
import spirite.base.image_data.mediums.drawer.IImageDrawer;
import spirite.base.image_data.mediums.drawer.SkeletonStateDrawer;
import spirite.base.util.glmath.Rect;

public class PuppetLayer extends Layer {
	public final IPuppet puppet;
	public final ImageWorkspace context;
	
	private int selected = 0;
//	private IPuppet.IPart selectedPart;

	
	public PuppetLayer( ImageWorkspace context, List<PuppetPartInfo> toImport) {
		this.context = context;
		this.puppet = new BasePuppet(context, toImport);
		selected = 0;
	}
	public PuppetLayer( ImageWorkspace context, MediumHandle firstMedium) {
		this.context = context;
		this.puppet = new BasePuppet(context, firstMedium);
		
		usingBase = true;

		selected = 0;
		//selectedPart = puppet.getParts().get(0);
	}
	public PuppetLayer( ImageWorkspace context, BasePuppet base) {
		this.context = context;
		this.puppet = new Puppet(base, context);
		
		usingBase = false;
		selected = 0;
		//selectedPart = puppet.getParts().get(0);
	}
	public PuppetLayer( PuppetLayer other) {
		this.context = other.context;
		this.puppet = other.puppet.dupe();
		this.selected = other.selected;
	}
	
	//
	public IPuppet getActivePuppet() {
		return (usingBase) ? puppet.getBase() : puppet;
	}
	
	// ==========
	// ==== Part-creation/modification
	public int getSelectedIndex() {
		return selected;
	}
	public void setSelectedIndex( int selected) {
		this.selected = selected;
	}
	
	public void addNewPart() {
		getBase().addNewPart(selected);
		selected++;
	}
	public void movePart(int from, int to) {
		getBase().movePart(from,to);
	}
	public void removePart( int index) {
		
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
	public BasePuppet getBase() {
		return puppet.getBase();
	}

	
	public boolean isSkeletonVisible() {return skeletonVisible;}
	public void setSkeletonVisible(boolean visible) {
		skeletonVisible = visible;
		context.triggerFlash();
	}
	public void drawSkeleton( GraphicsContext gc) {
		
	}

	// ==========
	// ==== DERIVED
	@Override
	public BuildingMediumData getActiveData() {
		List<? extends IPart> parts = getActivePuppet().getParts();
		
		if( parts.size()-1 < selected)
			selected = parts.size()-1;
		
		return getActivePuppet().getParts().get(selected).buildData();
		//return new BuildingMediumData(__D__medium);
	}
	@Override
	public IImageDrawer getDrawer(BuildingMediumData building, IMedium medium) {
		if( skeletonVisible) {
			if( usingBase)
				return new BaseSkeletonDrawer(this);
			else if( puppet instanceof Puppet)
				return new SkeletonStateDrawer((Puppet)puppet);
		}
		
		return super.getDrawer(building, medium);
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
		if( usingBase)
			return puppet.getBase().getDrawList();
		return puppet.getDrawList();

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
		return new PuppetLayer(this);
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
