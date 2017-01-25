package spirite.image_data;

import java.util.ArrayList;
import java.util.List;

import spirite.image_data.GroupTree.GroupNode;
import spirite.image_data.GroupTree.Node;
import spirite.image_data.ImageWorkspace.ImageChangeEvent;
import spirite.image_data.ImageWorkspace.MImageObserver;
import spirite.image_data.ImageWorkspace.StructureChange;
import spirite.image_data.animation_data.AbstractAnimation;

/***
 * AnimatonManager manages the animation functionality of an ImageWorkspace.
 * 	They are tied 1:1, but separated for maintainability.
 * 
 * @author RoryBurks
 *
 */
public class AnimationManager implements MImageObserver {
	private final ImageWorkspace context;
	
	private ArrayList<AbstractAnimation> animations = new ArrayList<>();
	

	AnimationManager( ImageWorkspace context) {
		this.context = context;
		context.addImageObserver(this);
	}
	
	public AbstractAnimation addAnimation( AbstractAnimation animation) {
		animations.add(animation);
		
		triggerStructureChange( new AnimationStructureEvent());
		return animation;
	}
	
	private final List<Link> links = new ArrayList<>();
	
	public void linkAnimation( AbstractAnimation animation, GroupNode group ) {
		Link link = new Link();
		link.animation = animation;
		link.group = group;
		
		links.add(link);
	}
	private class Link {
		 AbstractAnimation animation;
		 GroupNode group;
	}
	
	@SuppressWarnings("unchecked")
	public List<AbstractAnimation> getAnimations() {
		return (List<AbstractAnimation>)animations.clone();
	}
	
	// :::: Observers
	public static interface MAnimationStructureObserver {
		public void animationStructureChanged(AnimationStructureEvent evt);
	}
	
	public static class AnimationStructureEvent {
	}
	

	List<MAnimationStructureObserver> structureObservers = new ArrayList<>();
    public void addStructureObserver( MAnimationStructureObserver obs) { structureObservers.add(obs);}
    public void removeStructureObserver( MAnimationStructureObserver obs) { structureObservers.remove(obs); }

	private void triggerStructureChange( AnimationStructureEvent evt) {
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
