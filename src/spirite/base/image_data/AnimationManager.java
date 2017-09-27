package spirite.base.image_data;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import spirite.base.brains.RenderEngine.RenderMethod;
import spirite.base.graphics.RenderProperties;
import spirite.base.image_data.GroupTree.GroupNode;
import spirite.base.image_data.GroupTree.LayerNode;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.ImageWorkspace.ImageChangeEvent;
import spirite.base.image_data.ImageWorkspace.MImageObserver;
import spirite.base.image_data.ImageWorkspace.MSelectionObserver;
import spirite.base.image_data.ImageWorkspace.StructureChangeEvent;
import spirite.base.image_data.animation_data.FixedFrameAnimation;
import spirite.base.image_data.animation_data.FixedFrameAnimation.AnimationLayer;
import spirite.hybrid.MDebug;
import spirite.hybrid.MDebug.ErrorType;

/***
 * AnimatonManager manages the animation functionality of an ImageWorkspace.
 * 	They are tied 1:1, but separated for maintainability.
 * 
 * @author RoryBurks
 *
 */
public class AnimationManager implements MImageObserver, MSelectionObserver {
	private final ImageWorkspace context;
	
	private ArrayList<Animation> animations = new ArrayList<>();
	private Animation selectedAnimation = null;
	private final AnimationView view;
	

	AnimationManager( ImageWorkspace context) {
		this.context = context;
		this.view = new AnimationView(context);
		context.addSelectionObserver(this);
		context.addImageObserver(this);
	}
	
	// Get/Set
	public Animation getSelectedAnimation() {
		return selectedAnimation;
	}
	public void setSelectedAnimation( Animation anim) {
		if( selectedAnimation != anim) {
			Animation previous = selectedAnimation;
			selectedAnimation = anim;
			triggerAnimationSelectionChange(previous);
		}
	}
	public List<Animation> getAnimations() {
		return new ArrayList<> (animations);
	}
	public AnimationView getView() {return view;}

	// :: Animation States
	private final Map<Animation,AnimationState> stateMap = new HashMap<>();
	public AnimationState getAnimationState( Animation animation) {
		return stateMap.get(animation);
	}
	public class AnimationState implements RenderProperties.Trigger{
		
		private float met;
		private boolean expanded = true;
		private float selectMet;
		private Map<AnimationLayer,RenderProperties> substates = new HashMap<>();
		private Map<Integer,RenderProperties> byTick = new HashMap<>();	
		private RenderProperties defaultProperties = new RenderProperties();
		
		private AnimationState(){
			defaultProperties.visible = false;
			resetSubstatesForTicks();
		}
		
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
		
		public float getSelectedMetronome() {return selectMet;}
		public void setSelectedMetronome(float t) {
			selectMet = t;
			context.triggerFlash();
		}
		
		// Vert/Hor Visibility Settings
		public RenderProperties getSubstateForLayer( AnimationLayer al) {
			RenderProperties ss = substates.get(al);
			return (ss == null) ? defaultProperties : ss;
		}
		public void putSubstateForLayer( AnimationLayer al, RenderProperties properties) {
			substates.remove(al);
			substates.put( al, new RenderProperties(properties,this));
		}
		public void resetSubstatesForLayers() {
			substates.clear();
		}
		
		public RenderProperties getSubstateForRelativeTick( int tick) {
			RenderProperties ss = byTick.get(tick);
			return (ss == null) ? defaultProperties : ss;
		}
		public void putSubstateForRelativeTick( int tick, RenderProperties properties) {
			byTick.remove(tick);
			byTick.put( tick, new RenderProperties(properties,this));
		}
		public void resetSubstatesForTicks() {
			byTick.clear();
			byTick.put(0, new RenderProperties(this));
		}
		
		// :::: Specific Get
		public RenderProperties getPropertiesForFrame(AnimationLayer layer, int tick) {
			int _met = (int)Math.floor(selectMet);
			int offset = tick - _met;
			
			RenderProperties properties = byTick.get(offset);
			if( properties == null) properties = defaultProperties;
			
			return properties;
		}

		@Override public boolean visibilityChanged(boolean newVisible) {
			context.triggerFlash();
			return true;
		}
		@Override public boolean alphaChanged(float newAlpha) {
			context.triggerFlash();
			return true;
		}
		@Override public boolean methodChanged(RenderMethod newMethod, int newValue) {
			context.triggerFlash();
			return true;
		}
	}

	// :: Internal Add/Remove
	private void _addAnimation(Animation anim, AnimationState as) {
		_addAnimation(anim,as,animations.size());
	}
	private void _addAnimation(Animation anim, AnimationState as, int index) {
		animations.add(index, anim);
    	stateMap.put(anim, new AnimationState());
		triggerNewAnimation(anim);
	}
	private void _removeAnimation(Animation anim) {
		animations.remove(anim);
		stateMap.remove(anim);
		triggerRemoveAnimation(anim);
	}
	
	// :: AddAnimation
	public void addAnimation( Animation animation) {
		animation.context = this;
		context.getUndoEngine().performAndStore( new AddAnimationAction(animation));
	}
	public class AddAnimationAction extends UndoEngine.NullAction {
		private final Animation previousSelected;
		private final Animation animation;
		private final AnimationState as;
		
		private AddAnimationAction( Animation animation) {
			this.animation = animation;
			previousSelected = getSelectedAnimation();
			as = new AnimationState();
		}

		@Override
		protected void performAction() {
			_addAnimation( animation, as);
			setSelectedAnimation(animation);
		}

		@Override
		protected void undoAction() {
			_removeAnimation(animation);
			setSelectedAnimation(previousSelected);
		}
		
		@Override
		public String getDescription() {
			return "Add Animation";
		}
	}
	
	
	// :: RemoveAnimation
	public void removeAnimation( Animation animation) {
		if( !animations.contains(animation)) {
			MDebug.handleError(ErrorType.STRUCTURAL_MINOR, null, "Attempted to remove Animation that isn't tracked.");			
			return;
		}

		context.getUndoEngine().performAndStore( new RemoveAnimationAction(animation));
	}
	public class RemoveAnimationAction extends UndoEngine.NullAction {
		private final Animation toRemove;
		private final boolean wasSelected;
		private int oldIndex;
		private final AnimationState as;
		
		private RemoveAnimationAction( Animation toRemove) {
			this.toRemove = toRemove;
			wasSelected = (selectedAnimation == toRemove);
			oldIndex = animations.indexOf(toRemove);
			as = stateMap.get(toRemove);
		}
		
		@Override
		protected void performAction() {
			if( wasSelected)
				setSelectedAnimation(null);
			_removeAnimation(toRemove);
		}

		@Override
		protected void undoAction() {
			if( wasSelected) 
				setSelectedAnimation(toRemove);
			_addAnimation( toRemove, as, oldIndex);
		}
		@Override
		public String getDescription() {
			return "Remove Animation";
		}
	}
	
	// :::: Frame Selection
	
	// Since there is a 1:many relationship between nodes and animations, sometimes
	//	you need a finer selection
	
	public AnimationLayer.Frame getSelectedFrame() {
		if( selectedFrame != null)
			return selectedFrame;
		
		Node node = context.getSelectedNode();
		
		if( node instanceof LayerNode && selectedAnimation instanceof FixedFrameAnimation) 
		{
			FixedFrameAnimation anim = (FixedFrameAnimation)selectedAnimation;
			
			for( AnimationLayer layer : anim.getLayers()) {
				for( AnimationLayer.Frame frame:  layer.getFrames())
				{
					if( frame.getLayerNode() == node)
						return frame;
				}
			}
		}
		
		
		return null;
	}
	public void selectFrame( AnimationLayer.Frame frame) {
		context.setSelectedNode( frame == null ? null : frame.getLayerNode());
		selectedFrame = frame;
	}
	
	private AnimationLayer.Frame selectedFrame;
	
	
	
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

	// :::: MSelectionObserver
	@Override
	public void selectionChanged(Node newSelection) {
		// TODO Auto-generated method stub
		
	}
}
