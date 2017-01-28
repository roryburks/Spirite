package spirite.image_data;

import java.util.ArrayList;
import java.util.List;

import spirite.image_data.GroupTree.GroupNode;
import spirite.image_data.GroupTree.Node;
import spirite.image_data.ImageWorkspace.ImageChangeEvent;
import spirite.image_data.ImageWorkspace.MImageObserver;
import spirite.image_data.ImageWorkspace.StructureChange;

/***
 * AnimatonManager manages the animation functionality of an ImageWorkspace.
 * 	They are tied 1:1, but separated for maintainability.
 * 
 * @author RoryBurks
 *
 */
public class AnimationManager implements MImageObserver {
	private final ImageWorkspace context;
	
	private ArrayList<Animation> animations = new ArrayList<>();
	Animation selectedAnimation = null;
	

	AnimationManager( ImageWorkspace context) {
		this.context = context;
		context.addImageObserver(this);
	}
	
	// Get/Set
	public Animation getSelectedAnimation() {
		return selectedAnimation;
	}
	public void setSelectedAnimation( Animation anim) {
		selectedAnimation = anim;
	}
	public List<Animation> getAnimations() {
		return new ArrayList<> (animations);
	}
	
	
	
	public Animation addAnimation( Animation animation) {
		animations.add(animation);
		animation.context = this;
		setSelectedAnimation(animation);
		triggerStructureChange( new AnimationStructureEvent());
		return animation;
	}
	
	
	// :::: Animation Links
	private class Link {
		 Animation animation;
		 GroupNode group;
	}
	private final List<Link> links = new ArrayList<>();
	
	public void linkAnimation( Animation animation, GroupNode group ) {
		Link link = new Link();
		link.animation = animation;
		link.group = group;
		
		links.add(link);
	}
	public void unlinkAnimation(Animation animation, GroupNode group) {
	}
	
	public boolean isLinked(Animation animation, GroupNode group) {
		return false;
	}
	
	public void destroyLinked( Animation animation, GroupNode group) {
	}
	
	
	// :::: Observers
	public static interface MAnimationStructureObserver {
		public void animationStructureChanged(AnimationStructureEvent evt);
	}
	
	public static class AnimationStructureEvent {
		public List<Animation> getAnimationsAffected() { return new ArrayList<>(0);}
	}
	

	List<MAnimationStructureObserver> structureObservers = new ArrayList<>();
    public void addStructureObserver( MAnimationStructureObserver obs) { structureObservers.add(obs);}
    public void removeStructureObserver( MAnimationStructureObserver obs) { structureObservers.remove(obs); }

	void triggerStructureChange( AnimationStructureEvent evt) {
		for( MAnimationStructureObserver obs : structureObservers)
			obs.animationStructureChanged(evt);
	}

	// :::: MImageObserver
	@Override	public void imageChanged(ImageChangeEvent evt) {}

	@Override
	public void structureChanged(StructureChange evt) {
		List<Node> changed = evt.getChangedNodes();
		
		for( Link link : links) {
			if( changed.contains(link.group)) {
				link.animation.interpretLink(link.group);
				triggerStructureChange( new AnimationStructureEvent());
			}
		}
	}
}
