package spirite.image_data;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import spirite.MDebug;
import spirite.MDebug.ErrorType;
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
	private Animation selectedAnimation = null;
	private float met = 0.0f;
	

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
	public float getFrame() {return met;}
	public void setFrame( float frame) {
		if( met != frame) {
			met = frame;
			triggerFrameChanged();
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
//		trigg
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
    	
    	Iterator<WeakReference<MAnimationStructureObserver>> it = animationStructureObservers.iterator();
    	while( it.hasNext()) {
    		MAnimationStructureObserver obs = it.next().get();
    		if( obs == null) it.remove();
    		else  
    			obs.animationAdded( evt);
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
    			obs.animationAdded( evt);
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
		private float frame;
		
		private MAnimationStateEvent() {
			selected = AnimationManager.this.getSelectedAnimation();
			frame = AnimationManager.this.getFrame();
		}
		
		public Animation getSelected() {return selected;}
		public Animation getPreviousSelection() {return previous;}
		public float getFrame() { return frame;}
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
	private void triggerFrameChanged( ) {
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
	public void structureChanged(StructureChange evt) {
		List<Node> changed = evt.getChangedNodes();
		
		for( Link link : links) {
			if( changed.contains(link.group)) {
				link.animation.interpretLink(link.group);
				
				triggerChangeAnimation(link.animation);
			}
		}
	}
}
