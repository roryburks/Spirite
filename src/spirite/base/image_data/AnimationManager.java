package spirite.base.image_data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import spirite.base.graphics.RenderProperties;
import spirite.base.graphics.renderer.RenderEngine.RenderMethod;
import spirite.base.image_data.GroupTree.LayerNode;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.ImageWorkspace.ImageChangeEvent;
import spirite.base.image_data.ImageWorkspace.MImageObserver;
import spirite.base.image_data.ImageWorkspace.MNodeSelectionObserver;
import spirite.base.image_data.ImageWorkspace.StructureChangeEvent;
import spirite.base.image_data.animations.AnimationState;
import spirite.base.image_data.animations.NodeLinkedAnimation;
import spirite.base.util.ObserverHandler;
import spirite.hybrid.MDebug;
import spirite.hybrid.MDebug.ErrorType;

/***
 * The AnimationManager stores all of the animations within a workspace and handles messages 
 * related to changes in the animation, both in their view and in their structure.
 */
public class AnimationManager implements MImageObserver, MNodeSelectionObserver {
	private final ImageWorkspace context;

	private ArrayList<Animation> animations = new ArrayList<>();
	private Animation selectedAnimation = null;
	private final AnimationView view;

	AnimationManager(ImageWorkspace context) {
		this.context = context;
		this.view = new AnimationView(context, this);
		context.addSelectionObserver(this);
		context.addImageObserver(this);
	}

	// Get/Set
	public Animation getSelectedAnimation() {
		return selectedAnimation;
	}

	public void setSelectedAnimation(Animation anim) {
		if (selectedAnimation != anim) {
			Animation previous = selectedAnimation;
			selectedAnimation = anim;
			triggerAnimationSelectionChange(previous);
		}
	}

	public List<Animation> getAnimations() {
		return new ArrayList<>(animations);
	}

	public AnimationView getView() {
		return view;
	}

	// :: Animation States
	private final Map<Animation, AnimationState> stateMap = new HashMap<>();

	public AnimationState getAnimationState(Animation animation) {
		return stateMap.get(animation);
	}



	// :: Internal Add/Remove
	private void _addAnimation(Animation anim, AnimationState as) {
		_addAnimation(anim, as, animations.size());
	}

	private void _addAnimation(Animation anim, AnimationState as, int index) {
		animations.add(index, anim);
		stateMap.put(anim, new AnimationState(anim));
		triggerNewAnimation(anim);
	}

	private void _removeAnimation(Animation anim) {
		animations.remove(anim);
		stateMap.remove(anim);
		triggerRemoveAnimation(anim);
	}

	// :: AddAnimation
	public void addAnimation(Animation animation) {
		animation.setContext(this.context);
		context.getUndoEngine().performAndStore(new AddAnimationAction(animation));
	}

	public class AddAnimationAction extends UndoEngine.NullAction {
		private final Animation previousSelected;
		private final Animation animation;
		private final AnimationState as;

		private AddAnimationAction(Animation animation) {
			this.animation = animation;
			previousSelected = getSelectedAnimation();
			as = new AnimationState(animation);
		}

		@Override
		protected void performAction() {
			_addAnimation(animation, as);
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
	public void removeAnimation(Animation animation) {
		if (!animations.contains(animation)) {
			MDebug.handleError(ErrorType.STRUCTURAL_MINOR, null, "Attempted to remove Animation that isn't tracked.");
			return;
		}

		context.getUndoEngine().performAndStore(new RemoveAnimationAction(animation));
	}

	public class RemoveAnimationAction extends UndoEngine.NullAction {
		private final Animation toRemove;
		private final boolean wasSelected;
		private int oldIndex;
		private final AnimationState as;

		private RemoveAnimationAction(Animation toRemove) {
			this.toRemove = toRemove;
			wasSelected = (selectedAnimation == toRemove);
			oldIndex = animations.indexOf(toRemove);
			as = stateMap.get(toRemove);
		}

		@Override
		protected void performAction() {
			if (wasSelected)
				setSelectedAnimation(null);
			_removeAnimation(toRemove);
		}

		@Override
		protected void undoAction() {
			if (wasSelected)
				setSelectedAnimation(toRemove);
			_addAnimation(toRemove, as, oldIndex);
		}

		@Override
		public String getDescription() {
			return "Remove Animation";
		}
	}
	
	// Goes through all the animations and purges them of orphaned data
	public void purge() {
		
	}

	// :::: Observers
	private final ObserverHandler<MAnimationStructureObserver> animationStructureObs = new ObserverHandler<>();
	public void addAnimationStructureObserver(MAnimationStructureObserver obs) {animationStructureObs.addObserver(obs);}
	public void removeAnimationStructureObserver(MAnimationStructureObserver obs) {animationStructureObs.removeObserver(obs);}
	public static interface MAnimationStructureObserver {
		public void animationAdded(AnimationStructureEvent evt);
		public void animationRemoved(AnimationStructureEvent evt);
		public void animationChanged(AnimationStructureEvent evt);
	}
	enum StructureChangeType {
		ADD, REMOVE, CHANGE
	};
	public static class AnimationStructureEvent {
		private Animation animation;
		private StructureChangeType type;

		public Animation getAnimation() {
			return animation;
		}

		public StructureChangeType getType() {
			return type;
		}

	}

	private void triggerNewAnimation(Animation anim) {
		AnimationStructureEvent evt = new AnimationStructureEvent();
		evt.animation = anim;
		evt.type = StructureChangeType.ADD;
		
		animationStructureObs.trigger((MAnimationStructureObserver obs) -> {obs.animationAdded(evt);}); 
	}

	private void triggerRemoveAnimation(Animation anim) {
		AnimationStructureEvent evt = new AnimationStructureEvent();
		evt.animation = anim;
		evt.type = StructureChangeType.REMOVE;

		animationStructureObs.trigger((MAnimationStructureObserver obs) -> {obs.animationRemoved(evt);}); 
	}

	// Non-private so Animations can trigger it
	void triggerChangeAnimation(Animation anim) {
		AnimationStructureEvent evt = new AnimationStructureEvent();
		evt.animation = anim;
		evt.type = StructureChangeType.CHANGE;

		animationStructureObs.trigger((MAnimationStructureObserver obs) -> {obs.animationChanged(evt);}); 
	}


	/**
	 * AnimationStateObserver triggers any time the "selection" state of the
	 * animation changes, either from selecting a new animation or changing the
	 * frame.
	 */
	private final ObserverHandler<MAnimationStateObserver> animationStateObs = new ObserverHandler<>();
	public void addAnimationStateObserver(MAnimationStateObserver obs) { animationStateObs.addObserver(obs);}
	public void removeAnimationStateObserver(MAnimationStateObserver obs) { animationStateObs.removeObserver(obs);}
	public static interface MAnimationStateObserver {
		public void selectedAnimationChanged(MAnimationStateEvent evt);

		public void animationFrameChanged(MAnimationStateEvent evt);

		public void viewStateChanged(MAnimationStateEvent evt);
	}

	public class MAnimationStateEvent {
		private Animation selected;
		private Animation previous = null;
		private AnimationState state;

		private MAnimationStateEvent() {
			selected = AnimationManager.this.getSelectedAnimation();
			state = stateMap.get(selected);
		}

		public Animation getSelected() {
			return selected;
		}

		public Animation getPreviousSelection() {
			return previous;
		}

		public AnimationState getState() {
			return state;
		}
	}

	private void triggerAnimationSelectionChange(Animation previous) {
		MAnimationStateEvent evt = new MAnimationStateEvent();
		evt.previous = previous;
		
		animationStateObs.trigger((MAnimationStateObserver obs) -> {obs.selectedAnimationChanged(evt);});
		context.triggerFlash();
	}

	private void triggerFrameChanged() {
		MAnimationStateEvent evt = new MAnimationStateEvent();

		animationStateObs.trigger((MAnimationStateObserver obs) -> {obs.animationFrameChanged(evt);});
		context.triggerFlash();
	}

	private void triggerInnerStateChange(Animation animation) {
		MAnimationStateEvent evt = new MAnimationStateEvent();

		animationStateObs.trigger((MAnimationStateObserver obs) -> {obs.viewStateChanged(evt);});
		context.triggerFlash();
	}


	// :::: MImageObserver
	@Override
	public void imageChanged(ImageChangeEvent evt) {
	}

	@Override
	public void structureChanged(StructureChangeEvent evt) {
		List<Node> changed = evt.change.getChangedNodes();

		for (Animation animation : animations) {
			if( animation instanceof NodeLinkedAnimation)
				((NodeLinkedAnimation)animation).nodesChanged(changed);
		}
	}

	// :::: MSelectionObserver
	@Override
	public void selectionChanged(Node newSelection) {
		// TODO Auto-generated method stub

	}
}
