package spirite.base.image_data.mediums.maglev;

import spirite.base.graphics.IImage;
import spirite.base.image_data.ImageWorkspace.BuildingMediumData;
import spirite.base.image_data.layers.puppet.BasePuppet;
import spirite.base.image_data.layers.puppet.Puppet;
import spirite.base.image_data.mediums.ABuiltMediumData;
import spirite.base.image_data.mediums.IMedium;
import spirite.base.image_data.mediums.drawer.IImageDrawer;
import spirite.base.util.interpolation.Interpolator2D;

public class DerivedMaglevMedium 
	implements IMedium
{
	public DerivedMaglevMedium( MaglevMedium derivedFrom) {
		
	}
	
	public void deriveFromBone( BasePuppet.BaseBone bone, Interpolator2D state) {
		
	}

	// TODO
	@Override public int getWidth() {return 0;}
	@Override public int getHeight() {return 0;}
	@Override public int getDynamicX() {return 0;}
	@Override public int getDynamicY() {return 0;}

	@Override
	public ABuiltMediumData build(BuildingMediumData building) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IMedium dupe() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IMedium copyForSaving() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void flush() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public IImage readOnlyAccess() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InternalImageTypes getType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IImageDrawer getImageDrawer(BuildingMediumData building) {
		// TODO Auto-generated method stub
		return null;
	}

}
