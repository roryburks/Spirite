package spirite.image_data;

import java.util.ArrayList;
import java.util.List;

import spirite.image_data.animation_data.AbstractAnimation;

/***
 * AnimatonManager manages the animation functionality of an ImageWorkspace.
 * 	They are tied 1:1, but separated for maintainability.
 * 
 * @author RoryBurks
 *
 */
public class AnimationManager {
	private final ImageWorkspace context;
	
	private ArrayList<AbstractAnimation> animations = new ArrayList<>();
	

	AnimationManager( ImageWorkspace context) {
		this.context = context;
	}
	
	public void addAnimation( AbstractAnimation animation) {
		animations.add(animation);
		
		triggerStructureChange( new AnimationStructureEvent());
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
}
