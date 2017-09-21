package spirite.base.image_data;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import spirite.base.image_data.GroupTree.GroupNode;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.ImageWorkspace.ImageChangeEvent;
import spirite.base.image_data.ImageWorkspace.MImageObserver;
import spirite.base.image_data.ImageWorkspace.StructureChangeEvent;
import spirite.hybrid.MDebug;
import spirite.hybrid.MDebug.ErrorType;

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
	private Animation selectedAnimation = null;
	

	AnimationManager( ImageWorkspace context) {
		this.context = context;
		context.addImageObserver(this);
	}
	
	// Get/Set
	public Animation getSelectedAnimation() {
		return selectedAnimation;
	}
	public void setSelectedAnimation( Animation anim) {
		Animation previous = selectedAnimation;
		selectedAnimation = anim;
		triggerAnimationSelectionChange(previous);
	}
	public List<Animation> getAnimations() {
		return new ArrayList<> (animations);
	}
	
	public AnimationState getAnimationState( Animation animation) {
		return stateMap.get(animation);
	}
	
	private final Map<Animation,AnimationState> stateMap = new HashMap<>();
	
	public class AnimationState {
		private float met;
		private boolean expanded = true;
		
		private AnimationState(){}
		
		public boolean getExpanded() {return expanded;}
		public void setExpanded( boolean expanded) {
			if(this.expanded != expanded) {
				this.expanded = expanded;
				triggerStateChanged();
			}
		}
		public float getMetronom() {return met;}
		public void setMetronome(float metronome) {
			if( this.met != metronome) {
				this.met = metronome;
				triggerStateChanged();
			}
		}
	}
	
	
	// Add/Remove
	public Animation addAnimation( Animation animation) {
		animations.add(animation);
		animation.context = this;
		setSelectedAnimation(animation);
		triggerNewAnimation(animation);
		return animation;
	}
	
	public void removeAnimation( Animation animation) {
		int index = animations.indexOf(animation);
		
		if( index == -1) {
			MDebug.handleError(ErrorType.STRUCTURAL_MINOR, null, "Attempted to remove Animation that isn't tracked.");			
			return;
		}
		
		
		if( selectedAnimation == animation) {
			selectedAnimation = null;
			triggerAnimationSelectionChange(animation);
		}
		animations.remove(index);
		triggerRemoveAnimation(animation);
	}
	
	
	// :::: Animation Links
/*	private class Link {
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
	}*/
	
	
	// :::: Observers
	public static interface MAnimationStructureObserver {
		public void animationAdded( AnimationStructureEvent evt);
		public void animationRemoved( AnimationStructureEvent evt);
		public void animationChanged( AnimationStructureEvent evt);
	}
	enum StructureChangeType { ADD, REMOVE, CHANGE};
	public static class AnimationStructureEvent {
		private Animation animation;
		private StructureChangeType type;
		public Animation getAnimation() {return animation;}
		public StructureChangeType getType() {return type;}
	}

    private void triggerNewAnimation( Animation anim) {
    	AnimationStructureEvent evt = new AnimationStructureEvent();
    	evt.animation = anim;
    	evt.type = StructureChangeType.ADD;
    	
    	// This might not belong here
    	stateMap.put(anim, new AnimationState());
    	
    	Iterator<WeakReference<MAnimationStructureObserver>> it = animationStructureObservers.iterator();
    	while( it.hasNext()) {
    		MAnimationStructureObserver obs = it.next().get();
    		if( obs == null) it.remove();
    		else  
    			obs.animationAdded( evt);
    	}
	}
    private void triggerRemoveAnimation( Animation anim) {
    	AnimationStructureEvent evt = new AnimationStructureEvent();
    	evt.animation = anim;
    	evt.type = StructureChangeType.REMOVE;
    	
    	// This might not belong here
    	stateMap.remove(anim);
    	
    	Iterator<WeakReference<MAnimationStructureObserver>> it = animationStructureObservers.iterator();
    	while( it.hasNext()) {
    		MAnimationStructureObserver obs = it.next().get();
    		if( obs == null) it.remove();
    		else  
    			obs.animationRemoved(evt);
    	}
	}
    
    // Non-private so Animations can trigger it
    void triggerChangeAnimation( Animation anim) {
    	AnimationStructureEvent evt = new AnimationStructureEvent();
    	evt.animation = anim;
    	evt.type = StructureChangeType.CHANGE;
    	
    	Iterator<WeakReference<MAnimationStructureObserver>> it = animationStructureObservers.iterator();
    	while( it.hasNext()) {
    		MAnimationStructureObserver obs = it.next().get();
    		if( obs == null) it.remove();
    		else  
    			obs.animationChanged( evt);
    	}
	}
	
	List< WeakReference<MAnimationStructureObserver>> animationStructureObservers = new ArrayList<>();
	public void addAnimationStructureObserver( MAnimationStructureObserver obs) { animationStructureObservers.add(new WeakReference<>(obs));}
	public void removeAnimationStructureObserver( MAnimationStructureObserver obs) {
    	Iterator<WeakReference<MAnimationStructureObserver>> it = animationStructureObservers.iterator();
    	while( it.hasNext()) {
    		MAnimationStructureObserver other = it.next().get();
    		if( other == obs || other == null)
    			it.remove();
    	}
	}
	

	/** 
	 * AnimationStateObserver triggers any time the "selection" state of the
	 * animation changes, either from selecting a new animation or changing
	 * the frame.
	 */
	public static interface MAnimationStateObserver {
		public void selectedAnimationChanged(  MAnimationStateEvent evt);
		public void animationFrameChanged(MAnimationStateEvent evt);
	}
	public class MAnimationStateEvent {
		private Animation selected;
		private Animation previous = null;
		private AnimationState state;
		
		private MAnimationStateEvent() {
			selected = AnimationManager.this.getSelectedAnimation();
			state = stateMap.get(selected);
		}
		
		public Animation getSelected() {return selected;}
		public Animation getPreviousSelection() {return previous;}
		public AnimationState getState() { return state;}
	}
	
	private void triggerAnimationSelectionChange( Animation previous) {
    	Iterator<WeakReference<MAnimationStateObserver>> it = animationStateObservers.iterator();
    	
    	MAnimationStateEvent evt = new MAnimationStateEvent();
    	evt.previous = previous;
    	
    	while( it.hasNext()) {
    		MAnimationStateObserver obs = it.next().get();
    		if( obs == null) it.remove();
    		else  
    			obs.selectedAnimationChanged( evt);
    	}
	}
	private void triggerStateChanged( ) {
    	Iterator<WeakReference<MAnimationStateObserver>> it = animationStateObservers.iterator();
    	
    	MAnimationStateEvent evt = new MAnimationStateEvent();
    	
    	while( it.hasNext()) {
    		MAnimationStateObserver obs = it.next().get();
    		if( obs == null) it.remove();
    		else  
    			obs.animationFrameChanged( evt);
    	}
	}
	
	List< WeakReference<MAnimationStateObserver>> animationStateObservers = new ArrayList<>();
	public void addAnimationStateObserver( MAnimationStateObserver obs) { animationStateObservers.add(new WeakReference<>(obs));}
	public void removeAnimationStateObserver( MAnimationStateObserver obs) {
    	Iterator<WeakReference<MAnimationStateObserver>> it = animationStateObservers.iterator();
    	while( it.hasNext()) {
    		MAnimationStateObserver other = it.next().get();
    		if( other == obs || other == null)
    			it.remove();
    	}
	}
	
	
	

	// :::: MImageObserver
	@Override	public void imageChanged(ImageChangeEvent evt) {}

	@Override
	public void structureChanged(StructureChangeEvent evt) {
		List<Node> changed = evt.change.getChangedNodes();
		
		for( Animation animation : animations) {
			for( GroupNode node : animation.getGroupLinks()) {
				if( changed.contains(node))
					animation.interpretChange(node, evt);
			}
		}
	}
}
