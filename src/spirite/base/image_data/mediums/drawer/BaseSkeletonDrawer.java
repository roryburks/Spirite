package spirite.base.image_data.mediums.drawer;

import java.util.List;

import spirite.base.image_data.layers.puppet.BasePuppet;
import spirite.base.image_data.layers.puppet.BasePuppet.BaseBone;
import spirite.base.image_data.layers.puppet.BasePuppet.BasePart;
import spirite.base.image_data.layers.puppet.PuppetLayer;
import spirite.base.image_data.mediums.drawer.IImageDrawer.IPuppetBoneDrawer;
import spirite.base.util.MUtil;
import spirite.base.util.glmath.Vec2;

public class BaseSkeletonDrawer 
	implements 
		IImageDrawer,
		IPuppetBoneDrawer 
{
	private final PuppetLayer puppet;
	
	public BaseSkeletonDrawer( PuppetLayer base) {
		this.puppet = base;
	}

	@Override
	public BaseBone grabBone(int x, int y, float width) {
		BasePuppet base = puppet.getBase();
		List<BasePart> parts = base.getParts();
		
		for( int i=0; i < parts.size(); ++ i) {
			BasePart part = parts.get(i);
			BaseBone bone = part.getBone();
			if(bone != null) {
				Vec2 projection = MUtil.projectOnto(bone.x1, bone.y1, bone.x2, bone.y2, new Vec2(x, y));
				if( Math.abs(projection.y) < width) {
					
					float mag = (float) MUtil.distance(bone.x1, bone.y1, bone.x2, bone.y2);
					float grab = mag * projection.x;
					
					if( grab >= -width && grab <= mag+width) {
						puppet.setSelectedIndex(i);
						return bone;
					}
				}
			}
		}
		
		return null;
	}

	@Override
	public void makeBone(float x1, float y1, float x2, float y2) {
		BasePart part = puppet.getBase().getParts().get(puppet.getSelectedIndex());
		part.setBone( new BaseBone(x1, y1, x2, y2, 1));
	}
}
