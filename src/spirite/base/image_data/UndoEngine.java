package spirite.base.image_data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Stack;

import spirite.base.graphics.RawImage;
import spirite.base.graphics.gl.GLEngine;
import spirite.base.image_data.ImageWorkspace.BuildingMediumData;
import spirite.base.image_data.ImageWorkspace.ImageChangeEvent;
import spirite.base.image_data.mediums.ABuiltMediumData;
import spirite.base.image_data.mediums.FlatMedium;
import spirite.base.image_data.mediums.IMedium;
import spirite.base.util.ObserverHandler;
import spirite.hybrid.MDebug;
import spirite.hybrid.MDebug.ErrorType;
import spirite.hybrid.MDebug.WarningType;

/***
 * The UndoEngine stores all undoable actions and the data needed to recover
 * the image data to its previous states.  It also determines how and when the
 * data should be clipped.
 * 
 * The Basic idea behind the UndoEngine is that only every Nth action 
 * (by default 10) actually stores the entire ImageData with it.  Instead
 * most undo actions are stored as their logical components, be they a 
 * series of Stroke Paths, a Fill Command, or what have you.  This minimizes the
 * memory storage while keeping the processing demand on undo reasonable
 * and without doing anything crazy like on-the-fly image compression or 
 * differential encoding.
 * 
 * While coding drawing actions for UndoActions separately from the actual
 *   draw commands might seem like redundant code for many tasks, there are enough
 *   tasks that require continuous visual feedback to separate performAction code
 *   from realtime execution code (though calling similar methods).
 * All NullActions are performed in a centralized place (ImageWorkspace.executeAction)
 * 
 * 
 * XXXXXXXXXXXX OUTDATED XXXXXXXXXXXXXXX
 * Events which update the undo engine with undo-able commands should come from
 *  a limited number of places to promote code maintainability.  As of now, only:
 * -ImageWorkspace
 * -DrawEngine
 * -SelectionEngine
 * ~~Penner has access to it for ending the Stroke, should probably fix that.
 * XXXXXXXXXXXX OUTDATED XXXXXXXXXXXXXXX
 * Ultimately this philosophy was abandoned as it would lead to monolythic ImageWorkspaces
 * and non-modular code.  Instead perform and store were hard-coupled such that performance
 * IS undoability, though care needs to be made between dealing with hard references 
 * (i.e. Java references) and soft references (e.g. relative positions on lists)
 * XXXXXXXXXXXX OUTDATED XXXXXXXXXXXXXXX
 * 
 * TODO: Get rid of Image Actions with non-image components to avoid weird 
 * needs to call performImageAction
 * 
 * @author Rory Burks
 *
 */
public class UndoEngine {
	private static final int MAX_TICKS_PER_KEY = 10;
	private long absoluteMaxCache = 2000000000L;	// 2 Gig
	private int maxCacheSize = 100000000;	// 1 Gig
	private int maxQueueSize = 100;
	private final List<UndoContext> contexts;
	private final LinkedList<UndoContext> queue = new LinkedList<>();
	private ListIterator<UndoContext> queuePosition = null;
	
	private final ImageWorkspace workspace;
	
	private int met = 0;	// Keeps track of how many changes there have been
							// since the last reset.

	/** Marks the point on the undoQueue where the image is considered "unchanged" */
	private int saveSpot = -1;	
	
	private enum CullBehavior {
		// If cacheSize > maxCacheSize, start clipping the queue's tail
		//	until that is no longer true
		ONLY_CACHE,		
		// If queue.size > maxQueueSize or cacheSize > absoluteMaxCache
		//	 clip until that's not true
		ONLY_UNDO_COUNT,
		// If queue.size > maxQueueSize and cacheSize > maxCacheSize, clip
		//	or if cacheSize > absoluteMaxCache
		CACHE_AND_COUNT,
	}
	private CullBehavior cullBehavior = CullBehavior.CACHE_AND_COUNT;
	
	
	public UndoEngine(ImageWorkspace workspace) {
		this.workspace = workspace;
		
		contexts = new ArrayList<UndoContext>();
		contexts.add( new NullContext());
		contexts.add( new CompositeContext());
		assert( contexts.get(0) instanceof NullContext);
		assert( contexts.get(1) instanceof CompositeContext);
	}
	
	@Override
	public String toString() {
		return "UndoEngine for: " + workspace.getFileName();
	}
	
	
	/***
	 * Usually called after loading an image or creating a new one, this
	 * method solidifies the current image state, removing any hanging
	 * undoable actions.
	 */
	public void reset() {
		for( UndoContext context : contexts)
			context.flush();
		queue.clear();
		queuePosition = null;
		contexts.clear();
		contexts.add( new NullContext());
		contexts.add( new CompositeContext());
		assert( contexts.get(0) instanceof NullContext);
		assert( contexts.get(1) instanceof CompositeContext);
		saveSpot = 0;
		met = 0;
	}
	
	/***
	 * Gets the current position of the Endo Queue
	 * @return 0: Base Image, 1: One Action off the base Image, ....
	 */
	public int getQueuePosition() {
		if( queuePosition == null) 
			return queue.size();
		else
			return queuePosition.nextIndex();
	}
	
	/***
	 * Moves the undo Queue to the requested position, performing all
	 * undo's and redo's along the way.
	 */
	public void setQueuePosition( int pos) {
		if( pos > queue.size() || pos < 0) 
			return;
		
		while( pos < getQueuePosition()) {
			undo();
		}
		while( pos > getQueuePosition()) {
			redo();
		}
	}
	
	/** Returns the number of UndoActions it's been since the last time
	 * reset was called (or since creation if it's never been called).
	 */
	public int getMetronome() {
		return met;
	}
	
	// :::: Called by ImageWorkapce
	/** Gets a list of all Data used in the UndoEngine. */
	List<MediumHandle> getDataUsed() {
		LinkedHashSet<MediumHandle> set = new LinkedHashSet<>();
		
		for( UndoContext context : contexts) {
			if( context instanceof NullContext)
				set.addAll(((NullContext) context).getImageDependency());
			else if( context instanceof CompositeContext) 
				set.addAll(((CompositeContext) context).getImageDependency());
			else if( context.image != null)
				set.add(context.image);
		}
		
		return new ArrayList<>(set);
	}
	
	/** @return true if the undoQueue is currently at the "Saved" position. */
	boolean atSaveSpot() {
		return saveSpot == getQueuePosition();
	}
	/** Sets the current undoQueue position as the "Saved" position. */
	void setSaveSpot() {
		saveSpot = getQueuePosition();
	}
	
	/***
	 * Constructs a list of all the logical actions that are performed in the
	 * Undo History in a straight list that makes very little sense to the Undo 
	 * Engine but is more easily interpreted into user-readable data.
	 */
	public List<UndoIndex> constructUndoHistory() {
		// !!!! NOTE: this leaks data access
		List<UndoIndex> list = new ArrayList<>(queue.size());
		
		// Prepare all the UndoContexts to create a queue from their first data
		for( UndoContext context : contexts ) {
			context.startIterate();
		}
		
		// Get Each UndoAction from 
		Iterator<UndoContext> it = queue.iterator();
		while( it.hasNext()) {
			UndoContext context= it.next();
			
			list.add( new UndoIndex( context.image, context.iterateNext()));
		}
		
		return list;
	}
	public UndoableAction getMostRecentAction() {
		if( queue.size() == 0)
			return null;
		return queue.getLast().getLast();
	}
	public static class UndoIndex {
		public MediumHandle data;
		public UndoableAction action;
		public UndoIndex( MediumHandle data, UndoableAction action) {
			this.data = data;
			this.action = action;
		}
	}
	
	/***
	 * Prepares the given ImageData for storing an upcoming undoable action.
	 * You need to call this before you edit the data or else the first action
	 * you perform will not be undoable.
	 * 
	 * Should only be called by ImageWorkspace.checkoutImage
	 */
	public void prepareContext( MediumHandle data) {
		for( UndoContext context : contexts) {
			if( context.image == null) continue;
			if( context.image.equals(data))
				return;
		}
		
		assert( data != null);
		contexts.add( new ImageContext( data));
	}
	private UndoContext contextOf( MediumHandle data) {
		assert( data != null);

		for( UndoContext context : contexts) {
			if( context.image == null) continue;
			if( context.image.equals(data))
				return context;
		}
		
		UndoContext context = new ImageContext(data);
		contexts.add(context);
		return context;
	}
	
	
	/** 
	 * Stores the action into the undo engine as it gets performed.
	 * 
	 * NOTE: As a principal all actions that can be undoed should go through this.
	 * Should never perform their action (for a first time) outside of this method.
	 */
	public void performAndStore( UndoableAction action) {
		if( action != null) {
			if( action instanceof ImageAction) 
				prepareContext(((ImageAction) action).building.handle);
			if( activeStoreState != null)
				activeStoreState.add(action);
			else {
				action.performAction();
				storeAction(action);
			}
			

			if( action instanceof ImageAction)
				((ImageAction) action).building.handle.refresh();
		}
		else
			MDebug.handleWarning(WarningType.STRUCTURAL, this, "Attempted to store null as an action.");
	}
	
	/***
	 * Stores an action into the undo engine that has already been performed (but
	 * the action still contains data to recreate it).
	 * 
	 * Note: This action will screw up advanced UndoEngine behavior like pausing/
	 * unpausing.  It should only be used for complex actions that need to be 
	 * performed (e.g. for user feedback) as it is being inputted such as with
	 * the stroke engine.  It may be phased out entirely in the future in favor
	 * of undoing the action and re-doing it.
	 */
	public void storePerformedAction( UndoableAction action) {
		if( action != null) {
			action.performAction();
			storeAction(action);
		}
		else
			MDebug.handleWarning(WarningType.STRUCTURAL, this, "Attempted to store null as an action.");
	}
	
	/***
	 * Stores the given UndoAction into the UndoEngine on the given
	 * ImageData.
	 * 
 	 * Note: the Context should have already been created using prepareContext
	 * when the image was checked out.  If not, the first action will not be
	 * undoable because the previous keyframe will not have been stored.
	 */
	public void storeAction( UndoableAction action) {
		
		// Delete all actions stored after the current iterator point
		if( queuePosition != null) {
			queue.subList(queuePosition.nextIndex(), queue.size()).clear();
			
			for( UndoContext context : contexts) {
				context.cauterize();
			}
			
			if( queue.size() < saveSpot) {
				saveSpot = -1;
			}
			queuePosition = null;
		}
		

		// If the UndoAction is a StackableAction and a compatible entry
		//   is on the top of the stack, modify that entry instead of creating
		//   a new one.
		if( queue.size() != 0 && getQueuePosition() == queue.size() && action instanceof StackableAction) {
			UndoableAction lastAction = getMostRecentAction();
			
			if( lastAction instanceof StackableAction) {
				StackableAction stackAction = ((StackableAction)lastAction);
				if(stackAction.canStack(action)) {
					stackAction.stackNewAction(action);
					return;
				}
			}
		}
		
		// Determine if the Context for the given ImageData exists
		UndoContext context = null;
		
		if( action instanceof NullAction) {
			context = contexts.get(0);
		}
		else if( action instanceof CompositeAction) {
			context = contexts.get(1);
		}
		else {
			ImageAction iaction = (ImageAction)action;
			
			for( UndoContext test : contexts) {
				if( test.image == null) continue;
				if( test.image.equals(iaction.building.handle)) {
					context = test;
					break;
				}
			}
		}
		

		
		if( context != null) {
			// Add the action to the queue
			context.addAction(action);
			action.onAdd();
				
			queue.add(context);
			queuePosition = null;
			triggerHistoryChanged();
		}
		else {
			assert( action instanceof ImageAction);
			contexts.add(new ImageContext( ((ImageAction)action).building.handle));
		}
		
		met++;
		
		// Cull 
		cull();
	}
	
	// =================
	// ==== Store States
	//private boolean isPaused = false;
	//private List<UndoableAction> queuedActions;
	private final Stack<List<UndoableAction>> storeStates = new Stack<>();
	private List<UndoableAction> activeStoreState = null;
	
	public void doAsAggregateAction( Runnable doer, String description) {
		List<UndoableAction> storeState = new ArrayList<>(2);
		storeStates.push(storeState);
		activeStoreState = storeState;
		
		doer.run();
		storeStates.pop();
		
		if( storeStates.size() > 0) {
			List<UndoableAction> topList = storeStates.peek();
			topList.addAll(storeState);
			activeStoreState = topList;
		}
		else {
			activeStoreState = null;
			performAndStore( new CompositeAction(storeState, description));
		}
	}
	
	public List<UndoableAction> aggregateActions( Runnable doer) {
		List<UndoableAction> storeState = new ArrayList<>(2);
		storeStates.push(storeState);
		activeStoreState = storeState;
		
		doer.run();
		storeStates.pop();
		
		if( storeStates.size() > 0) {
			List<UndoableAction> topList = storeStates.peek();
			topList.addAll(storeState);
			activeStoreState = topList;
			return topList;
		}
		else {
			activeStoreState = null;
			return storeState;
		}
	}
	
	
	/** Checks to see if there are too many UndoActions (using the behavior determined
	 * by the cullBehavior) and gets rid of the oldest undo actions until there
	 * aren't. */
	private void cull() {
		while( queue.size() > 0 && GLEngine.getInstance().getUsedResources() > absoluteMaxCache) {

			MDebug.log("Cull (Max)");
			clipTail();
		}
		
		// TODO: Make more general
		switch( cullBehavior) {
		case CACHE_AND_COUNT:
			while( GLEngine.getInstance().getUsedResources() > maxCacheSize 
					&& queue.size() > maxQueueSize) {

				MDebug.log("Cull (CaC)");
				clipTail();
				System.gc();
			}
			break;
		case ONLY_CACHE:
			while( GLEngine.getInstance().getUsedResources() > maxCacheSize
					&& queue.size() > 0) {
				MDebug.log("Cull (Cache)");
				clipTail();
				System.gc();
			}
			break;
		case ONLY_UNDO_COUNT:
			while( queue.size() > maxQueueSize){
				MDebug.log("Cull Queue");
				clipTail();
				System.gc();
			}
			break;
		}
		// Note: it SHOULD be impossible for a action to be culled if it is
		//	the current pointer on the UndoQueue, but it might happen.
		
		cleanUnusedContexts();
		workspace.cleanDataCache();
	}
	
	private void clipTail() {
		queue.getFirst().clipTail();
		queue.removeFirst();
	}
	
	private void cleanUnusedContexts() {
		Iterator<UndoContext> it = contexts.iterator();
		
		it.next();	// Skip past the two Special Contexts (Null/Composite)
		it.next();
		while( it.hasNext()) {
			UndoContext uc = it.next();
			if( uc.isEmpty()) {
				uc.flush();
				it.remove();
			}
		}
	}
	
	/***
	 * Attempts to undo an action.
	 * @return true if it was successful, false otherwise (if it has no earlier data)
	 */
	public synchronized boolean undo() {
		if( queuePosition == null) 
			queuePosition = queue.listIterator(queue.size());
		
		if( !queuePosition.hasPrevious())
			return false;
		else {
			--met;
			UndoContext toUndo = queuePosition.previous();
			toUndo.undo();
			triggerUndo();
			return true;
		}
	}
	
	/***
	 * Attempts to redo an action
	 * @return true if it was successful, false otherwise (if it has no further data)
	 */
	public synchronized boolean redo() {
		if( queuePosition == null || !queuePosition.hasNext()) {
			return false;
		}
		else {
			++met;
			queuePosition.next().redo();
			triggerRedo();
			return true;
		}
	}
	
	// :::: Special Creation methods
	
	/**
	 * Creates a special action that replaces the image at the given handle with the
	 *	new BufferedImage.  
	 *
	 * After creating the image, you should performAction on it as well as store
	 * it into the UndoEngine or else Undoing might get a little weird.
	 */
	public UndoableAction createReplaceAction( MediumHandle handle, RawImage newImage) {

		prepareContext(handle);
		for( UndoContext context : contexts) {
			if( context instanceof ImageContext ) {
					ImageContext icontext = (ImageContext)context;
					
				if( icontext.image.equals(handle)) {
					ImageContext.ReplaceImageAction action = icontext.new ReplaceImageAction(handle, new FlatMedium(newImage, workspace));
					return action;
				}
			}
		}
		
		MDebug.handleError(ErrorType.STRUCTURAL, "Failed to create ReplaceAction (problem with prepareContext?)");
		return null;
	}
	

	/***
	 * An UndoContext represents a "space" in which actions are sequentially relevant
	 * to each other.  e.g. If you change an ImageData's image data, it will have no
	 * effect on any other ImageData's data and such is stored in a separate context.
	 * But if you move a Node from one tree to another tree and then delete its old
	 * parent the order in which that happens is very important, and thus they must
	 * exist in the same context.
	 * 
	 * As of now there are two types of contexts: 
	 * -ImageContext which exist one per image.
	 * -NullContext of which only one exists, for changes outside any image data.
	 */
	public abstract static class UndoContext {
		MediumHandle image;

		protected UndoContext( MediumHandle data) {
			this.image = data;
		}

		protected abstract void addAction( UndoableAction action);
		protected abstract void undo();
		protected abstract void redo();
		protected abstract void cauterize();
		protected abstract void clipTail();

		protected abstract void startIterate();
		protected abstract UndoableAction iterateNext();
		protected abstract UndoableAction getLast();
		
		protected abstract boolean isEmpty();
		
		protected void flush() {}
	}
	
	/***
	 * ImageContext are tied to a particular ImageData.  Instead of storing
	 * each state as a separate BufferedImage, it only stores every N Keyframes
	 * (although the distance between Keyframes can be dynamic).  It stores 
	 * the changes in between Keyframes as their logical components and when
	 * undoing it reverts to the most recent earlier Keyframe and then re-constructs
	 * each action to get to the expected state.
	 * 
	 * This is an attempt to balance between memory-use and processor-use in a 
	 * setting where "reversible actions" are virtually impossible since you're 
	 * writing over old data.
	 */
	private class ImageContext extends UndoContext {
//		List<BufferedImage> keyframes = new ArrayList<>();
		private final List<ImageAction> actions = new ArrayList<>();
		private int pointer = 0;	// The position on the actionsList
		private int met = 0;	// Amount of actions it's been since a Keyframe
		private int vstart = 0;	// The first "valid" action.  As the tail is clipped,
								// this increments until it hits a Keyframe, then it removes
								// the old base keyframe and adjusts
		
		protected ImageContext( MediumHandle image) {
			super(image);
			
			//RawImage img = image.deepAccess().deepCopy();
			
			actions.add(new KeyframeAction(null));
			
			met = 0;
			pointer = 0;
		}

		/** A KeyframeAction is a special kind of ImageAction which instead of
		 * storing the way the image was changed, but instead stores the
		 * entire image as it should appear after the action.  It stores the
		 * action that it's "supposed" to be in hiddenAction and performs
		 * its logical components (if it has any).*/
		private class KeyframeAction extends ImageAction {
			protected IMedium _ii;
			ImageAction hiddenAction;
			boolean freed = false;
			KeyframeAction( ImageAction action) {
				super( new BuildingMediumData(image, 0, 0));
				this.hiddenAction = action;

				_ii = workspace.getData(image.id).dupe();
			}
			@Override 
			public void performNonimageAction() {
				if( hiddenAction != null)
					hiddenAction.performAction();
			}
			@Override
			public void undoAction() {
				if( hiddenAction != null)
					hiddenAction.undoAction();
			}
			@Override
			protected void onAdd() {
				if( hiddenAction != null)
					hiddenAction.onAdd();
			}
			@Override
			public void performImageAction(ABuiltMediumData built) {
				workspace._replaceIamge(image, _ii);
			}

			
			@Override
			protected void onDispatch() {
				// Most of the times, this should be the only thing that has
				//	a handle on the CachedImage, but in special cases, the 
				//	cached image might have been passed to it, so a multi-user
				//	scheme is relevant.
				_ii.flush();
				if( hiddenAction != null)
					hiddenAction.onDispatch();
				freed = true;
			}
			
			@Override
			protected void finalize() throws Throwable {
				// Shouldn't be necessary
				if( !freed)
					_ii.flush();
				super.finalize();
			}
		}
		
		/**
		 * ReplaceImageAction is a special kind of KeyframeAction in which the
		 * entire image has been replaced with another one.
		 */
		class ReplaceImageAction extends KeyframeAction {
			private final IMedium previousII;
			boolean freed = false;
			
			protected ReplaceImageAction(MediumHandle data, IMedium previous) {
				super( new NilImageAction(data));
				
				previousII = previous.dupe();
				//previousCache.reserve(this);
			}
			@Override
			protected void onAdd() {
				super.onAdd();
			}
			@Override
			protected void onDispatch() {
				previousII.flush();
				freed = true;
				super.onDispatch();
			}
			@Override
			public void performNonimageAction() {
				workspace._replaceIamge(building.handle, _ii.dupe() );
			}
			
			@Override
			public void undoAction() {
				workspace._replaceIamge(building.handle, previousII.dupe());
			}
			
			@Override
			protected void finalize() throws Throwable {
				// Is necessary if createReplaceAction is called, but never entered
				//	into the UndoEngine (but the CachedImage can't be reserved from
				//	onAdd as it might already be flushed by then).
				if( !freed) {
					previousII.flush();
					_ii.flush();
				}
				super.finalize();
			}
		}
		
		
		@Override
		protected void addAction( UndoableAction action) {
			if( !(action instanceof ImageAction)) {
				MDebug.handleError(ErrorType.STRUCTURAL, "Tried to add a non ImageAction to an ImageContext");
				return;
			}
			final ImageAction iaction = (ImageAction)action;
			
			met++;
			pointer++;
			
			// Record a keyframe if it's been a while or the action is a "HeavyAction"
			//	i.e. an action requiring a lot of computation to perform
			if( met == MAX_TICKS_PER_KEY || iaction.isHeavyAction()) {
				actions.add(new KeyframeAction( iaction));
				met = 0;
			}
			else {
				if( iaction instanceof KeyframeAction)
					met = 0;
				actions.add(iaction);
			}
		}
		
		@Override
		protected void undo() {
			pointer--;
			met--;
			if( pointer < 0) {
				MDebug.handleError(ErrorType.STRUCTURAL, "Internal Undo attempted before start of context.");
			}

			// Undo the logical action
			actions.get(pointer+1).undoAction();
			
			// Find the previous KeyframeAction
			if( met < 0) {
				int i;
				for( i = 0; i < MAX_TICKS_PER_KEY;++i) {
					if( actions.get(pointer-i) instanceof KeyframeAction) 
						break;
				}
				met = i;
			}
						
			// Refresh the Image to the current most recent keyframe
			actions.get(pointer-met).building.doOnBuiltData((built)-> {actions.get(pointer-met).performImageAction(built);});

			for( int i = pointer - met+1; i <= pointer; ++i) {
				int index = i;
				actions.get(index).building.doOnBuiltData((built)-> {actions.get(index).performImageAction(built);});
			}

			// Construct ImageChangeEvent and send it
			ImageChangeEvent evt = new ImageChangeEvent();
			evt.workspace = workspace;
			evt.dataChanged.add(this.image);
			evt.isUndoEngineEvent = true;
			workspace.triggerImageRefresh(evt);
		}

		@Override
		protected void redo() {
			pointer++;
			met++;
			if( pointer >= actions.size() || pointer == 0) {
				MDebug.handleError(ErrorType.STRUCTURAL, "Undo Outer queue desynced with inner queue.");
				return;
			}
			if( actions.get(pointer) instanceof KeyframeAction) {
				met = 0;
			}
			for( int i = pointer - met; i <= pointer; ++i) {
				actions.get(i).performAction();
			}

			// Construct ImageChangeEvent and send it
			ImageChangeEvent evt = new ImageChangeEvent();
			evt.workspace = workspace;
			evt.dataChanged.add(this.image);
			evt.isUndoEngineEvent = true;
			workspace.triggerImageRefresh(evt);
		}
		
		/***
		 * Makes it so that all information after the current marker is deleted
		 */
		@Override
		protected void cauterize() {
			List<ImageAction> subList = actions.subList(pointer+1, actions.size());
			
			// Iterate backwards so you can delete keyframes by the end
			//	so that you don't mess up indices
			ListIterator<ImageAction> it = subList.listIterator(subList.size());
			
			while( it.hasPrevious()){
				UndoableAction action = it.previous();
				action.onDispatch();
			}
			subList.clear();
		}

		
		// :::: Iterating
		private int iterMet;
		@Override
		protected void startIterate() {
			iterMet = 1;
		}

		@Override
		protected UndoableAction iterateNext() {
			UndoableAction action = actions.get(iterMet++);
			
			if( action instanceof KeyframeAction) {
				return ((KeyframeAction)action).hiddenAction;
			}
			return action;
		}


		@Override
		protected boolean isEmpty() {
			return actions.size() <= vstart+1;
		}


		@Override
		protected UndoableAction getLast() {
			if( actions.isEmpty())
				return null;
			else
				return actions.get(actions.size()-1);
		}
		
		@Override
		protected void flush() {
			for( UndoableAction action: actions) {
				action.onDispatch();
			}
		}
		@Override
		protected void clipTail() {
			if( vstart == pointer) {
				MDebug.handleError( ErrorType.STRUCTURAL_MINOR, "Tried to clip more than exists in ImageContext");
			}
			
			vstart++;
			if( actions.get(vstart) instanceof KeyframeAction) {
				// Remove the base Keyframe and all actions up until the new keyframe
				try {
//					((KeyframeAction)actions.get(0)).frameCache.flush();	// Should be handled by onDispatch
				} catch ( ClassCastException e) {
					MDebug.handleError( ErrorType.STRUCTURAL, e, "UndoEngine Corruption: First Action in ImageContext wasn't a KeyframeAction");
				}
				
				List<ImageAction> subList = actions.subList(0, vstart);
				pointer -= subList.size();
				
				for( ImageAction action : subList)
					action.onDispatch();
				subList.clear();
				vstart = 0;
			}
		}
	}

/***
 * NullContext is a special context that is not associated with any ImageData
 * As such the concept of keyframes to work from doesn't make sense.  Instead
 * Each task is stored.  But since you are not strictly working forwards, the
 * UndoActions associated with NullContext must have two-way methods for
 * performing AND undoing.
 * 
 * Since it's just a straight queue storing logical data, its behavior is simple
 * compared to ImageContext.
 */
public class NullContext extends UndoContext {
	private final LinkedList<NullAction> actions = new LinkedList<>();
	private ListIterator<NullAction> pointer = null;
	
	public NullContext() {
		super(null);
	}
	
	protected Collection<MediumHandle> getImageDependency() {
		LinkedHashSet<MediumHandle> set = new LinkedHashSet<>();
		
		
		for( NullAction action : actions){
			if( action.reliesOnData()) {
				set.addAll( action.getDependencies());
			}
		}
		
		return set;
	}

	@Override
	protected void addAction(UndoableAction action) {
		if( action instanceof NullAction) {
			actions.add( (NullAction) action);
		}
		else 
			MDebug.handleError(ErrorType.STRUCTURAL_MINOR, "Attempting to give a null context a non-null Action.");
	}
	@Override
	protected void undo() {
		if( pointer == null)
			pointer = actions.listIterator(actions.size());
		
		if( !pointer.hasPrevious() || pointer == null) {
			MDebug.handleError(ErrorType.STRUCTURAL, "Undo Outer queue desynced with inner queue (Null Undo).");
			return;
		}
		
		pointer.previous().undoAction();
	}
	
	@Override
	protected void redo() {
		if( pointer == null || !pointer.hasNext()) {
			MDebug.handleError(ErrorType.STRUCTURAL, "Undo Outer queue desynced with inner queue (Null Redo).");
			return;
		}
		
		pointer.next().performAction();
	}


	@Override
	protected void cauterize() {
		if( pointer != null) {
			List<NullAction> subList = actions.subList(pointer.nextIndex(), actions.size());
			
			for( NullAction action : subList) {
				action.onDispatch();
			}
			subList.clear();
			pointer = null;
		}
	}


	Iterator<NullAction> iter = null;
	@Override
	protected void startIterate() {
		iter = actions.iterator();
	}
	
	@Override
	protected UndoableAction iterateNext() {
		if( !iter.hasNext()) {
			MDebug.handleError(ErrorType.STRUCTURAL, "Undo Outer queue desynced with inner queue (Null Redo).");
			return null;
		}
		else
			return iter.next();
	}

	@Override
	protected boolean isEmpty() {
		return actions.isEmpty();
	}

	@Override
	protected UndoableAction getLast() {
		if( actions.isEmpty())
			return null;
		else
			return actions.getLast();
	}

	@Override
	protected void clipTail() {
		actions.getFirst().onDispatch();
		actions.removeFirst();
	}
	
	@Override
	protected void flush() {
		for( NullAction action : actions)
			action.onDispatch();
	}
}

	/**
	 * The CompositeContext is a special Context which can store multiple actions
	 * (both Image and Null) in a single action.  A CompositeAction is performed 
	 * in order that they are added to the list and undone in reverse order.
	 */
	private class CompositeContext extends UndoContext {
		LinkedList<CompositeAction> actions = new LinkedList<>();
		private ListIterator<CompositeAction> pointer = null;

		CompositeContext() {
			super(null);
		}

		protected Collection<MediumHandle> getImageDependency() {
			LinkedHashSet<MediumHandle> set = new LinkedHashSet<>();
			
			for( CompositeAction action : actions){
				for( UndoableAction uaction : action.actions) {
					if( uaction instanceof NullAction) {
						NullAction naction = (NullAction)uaction;
						if( naction.reliesOnData())
							set.addAll(naction.getDependencies());
					}
				}
			}
			
			return set;
		}

		@Override
		protected void addAction(UndoableAction action) {
			CompositeAction composite = (CompositeAction)action;
			
			for( int i=0; i<composite.contexts.length; ++i) {
				composite.contexts[i].addAction(composite.actions[i]);
			}
			actions.add(composite);
		}

		@Override
		protected void undo() {
			if( pointer == null)
				pointer = actions.listIterator(actions.size());
			
			if( !pointer.hasPrevious() || pointer == null) {
				MDebug.handleError(ErrorType.STRUCTURAL, "Undo Outer queue desynced with inner queue (Null Undo).");
				return;
			}
			
			CompositeAction composite = pointer.previous();

			for( int i = composite.contexts.length-1; i>=0; --i) {
				composite.contexts[i].undo();
			}
		}

		@Override
		protected void redo() {
			if( pointer == null || !pointer.hasNext()) {
				MDebug.handleError(ErrorType.STRUCTURAL, "Undo Outer queue desynced with inner queue (Null Redo).");
				return;
			}

			CompositeAction composite = pointer.next();
			for( UndoContext context : composite.contexts) {
				context.redo();
			}
		}

		@Override
		protected void cauterize() {
			if( pointer != null) {
				// All the real heavy-work will be handled by the other contexts
				List<CompositeAction> subList = actions.subList(pointer.nextIndex(), actions.size());
				subList.clear();
				pointer = null;
			}
		}

		@Override
		protected void clipTail() {
			CompositeAction composite = actions.getFirst();
			
			for( UndoContext context : composite.contexts) {
				context.clipTail();
			}
			actions.removeFirst();
			
		}

		Iterator<CompositeAction> iter = null;
		@Override
		protected void startIterate() {
			// Note even though the iterator position in other contexts can depend
			//	on the CompositeContext, since they're all started at the same time
			//	and in the same place we don't need to check for them here, but
			//	that might not be true in the future.
			iter = actions.iterator();
		}
		
		@Override
		protected UndoableAction iterateNext() {
			if( !iter.hasNext()) {
				MDebug.handleError(ErrorType.STRUCTURAL, "Undo Outer queue desynced with inner queue (Null Redo).");
				return null;
			}
			else {
				CompositeAction composite = iter.next();
				
				for( UndoContext context : composite.contexts)
					context.iterateNext();
				return composite;
			}
		}

		@Override
		protected boolean isEmpty() {
			return actions.isEmpty();
		}

		@Override
		protected UndoableAction getLast() {
			if( actions.isEmpty())
				return null;
			else
				return actions.getLast();
		}
		
		
		@Override
		protected void flush() {
			// Shouldn't need to flush since the other contexts will be flushed
			//	as well.
		}
	}
	
	
	
	/***
	 *  UndoActions
	 *  
	 *  Not only store the data needed to recreate the actions on the image data,
	 *  but also implements the methods to recreate them.
	 */
	public static abstract class UndoableAction {
		protected String description = "";
		protected abstract void performAction();
		protected abstract void undoAction();
		protected void onDispatch() {}
		protected void onAdd() {}
		public String getDescription() {return description;}
	}
	
	/** ImageActions are associated with ImageContexts, they have two components
	 * to them: a logical component, which calls performAction and undoAction
	 * sequentially as normal, but also has a performImageAction which writes
	 * to the imageData (additively from the last keyframe). */
	public static abstract class ImageAction extends UndoableAction {
		protected final BuildingMediumData building;
		protected ImageAction( BuildingMediumData data) {
			this.building = data;
		}
		@Override protected final void performAction() 
		{
			performNonimageAction();
			building.doOnBuiltData((built) -> {performImageAction(built);});
		}
		@Override protected void undoAction() {}
		protected void performNonimageAction() {}
		protected abstract void performImageAction(ABuiltMediumData built );
		protected boolean isHeavyAction() {return false;}
	}
	
	// For internal use only.  Why would you want make an empty Image Action?
	class NilImageAction extends ImageAction {
		protected NilImageAction(MediumHandle data) {
			super( new BuildingMediumData(data, 0, 0));
		}
		@Override		protected void performImageAction(ABuiltMediumData built) {}
		
	}
	
	

	/** Associated with the NullContext, a NullAction strictly requires you to
	 * have an UndoAction since there is always a logical component assosciated
	 * with it (otherwise it would have no Actions to it)
	 */
	public static abstract class NullAction extends UndoableAction {
		public boolean reliesOnData() {return false;}
		public Collection<MediumHandle> getDependencies() { return null;}
	}
	
	/** A StackableAction is an action that if performed multiple times in
	 * a row will automatically group into a single action by calling 
	 * <code>stackNewAction</code> on the old action if <code>canStack</code>
	 * returns true.  */
	public interface StackableAction {
		public void stackNewAction( UndoableAction newAction);
		public boolean canStack( UndoableAction newAction);
	}
	
	/** A CompositeAction is composed of two or more (or less, but what's the point)
	 * actions which are performed in a single "undo/redo" event in sequential order.
	 * The actions it composites can be any combination of actions with any Contexts.
	 */
	public class CompositeAction extends UndoableAction {
		protected final UndoContext contexts[];
		protected final UndoableAction actions[];
		
		public CompositeAction( List<UndoableAction> actions, String description) 
		{
			List<UndoableAction> sanatized = new ArrayList<>();
			
			// Clear all CompositeActions from the list and replace them with
			//	their list of action.
			Iterator<UndoableAction> it = actions.iterator();
			while( it.hasNext()) {
				UndoableAction action = it.next();
				if( action instanceof CompositeAction) {
					CompositeAction composite = (CompositeAction)action;
					it.remove();
					for( UndoableAction inAction : composite.actions) {
						sanatized.add(inAction);
					}
				}
				else
					sanatized.add(action);
			}
			
			// Constructs a CompositeAction from the given lists of actions
			this.description = description;
			
			int len = sanatized.size();
			this.contexts = new UndoContext[len];
			this.actions = new UndoableAction[len];
			
			for( int i=0; i < len; ++i) {
				UndoableAction action = sanatized.get(i);
				
				this.actions[i] = action;
				
				if( action instanceof NullAction)
					this.contexts[i] = UndoEngine.this.contexts.get(0);
				else if( action instanceof ImageAction) {
					this.contexts[i] = contextOf( ((ImageAction)action).building.handle);
				}
			}
		}
		public List<UndoableAction> getActions() {
			return Arrays.asList(actions);
		}
		/**
		 * Even though the CompositeContext executes "redo" by calling the
		 * relevant context for redo, many components perform commands the
		 * initial time by constructing an UndoableAction and then calling
		 * performAction, so we implement it here.
		 * 
		 * It even performs the ImageActions which aren't normally performed
		 * using the performAction 
		 */
		@Override		protected void performAction() { 
			// 
			for( int i = 0; i<contexts.length; ++i) {
				actions[i].performAction();
			}
		}
		@Override		protected void undoAction() {throw new UnsupportedOperationException();}
		
		@Override
		protected void onAdd() {
			for( UndoableAction action :actions) {
				action.onAdd();
			}
		}
	}

	/** A stackableCompositeAction is identical to a normal CompositeAction
	 * except it stack any non-composite StackableAction into any of the 
	 * sub-actions that can stack.
	 */
	public class StackableCompositeAction extends CompositeAction 
		implements StackableAction
	{
		public StackableCompositeAction(List<UndoableAction> actions, String description) {
			super(actions, description);
		}

		@Override
		public void stackNewAction(UndoableAction newAction) {
			List<UndoableAction> toCheck;
			if( newAction instanceof CompositeAction) {
				toCheck = Arrays.asList(((CompositeAction)newAction).actions);
			}
			else {
				toCheck = new ArrayList<>(1);
				toCheck.add(newAction);
			}
			
			for( UndoableAction action : toCheck) {
				for( int i=0; i<actions.length; ++i) {
					if( actions[i] instanceof StackableAction
						&& ((StackableAction)actions[i]).canStack(action)) {
						((StackableAction)actions[i]).stackNewAction(action);
					}
				}
			}
		}

		@Override
		public boolean canStack(UndoableAction newAction) {
			List<UndoableAction> toCheck;
			if( newAction instanceof CompositeAction) {
				toCheck = Arrays.asList(((CompositeAction)newAction).actions);
			}
			else {
				toCheck = new ArrayList<>(1);
				toCheck.add(newAction);
			}
			
			for( UndoableAction action : toCheck) {
				for( int i=0; i<actions.length; ++i) {
					if( actions[i] instanceof StackableAction
						&& ((StackableAction)actions[i]).canStack(action)) {
						return true;
					}
				}
			}
			return false;
		}
		
	}
	
	// Typically non-special UndoActions shouldn't go here but I don't see w
	public static class DrawImageAction extends ImageAction {
		private final RawImage stored;
//		private final int dx;
	//	private final int dy;
		
		public DrawImageAction(BuildingMediumData data, RawImage other) {
			super(data);
			this.stored = other;
		}
		@Override protected void onAdd() {
		}
		@Override protected void onDispatch() {
			stored.flush();
		}
		@Override
		protected void performImageAction(ABuiltMediumData built) {
			built.doOnGC((gc) -> {
				gc.drawImage(stored, 0, 0);
			});
		}
	}
	
	
	// :::: MUndoEngineObserver
	private final ObserverHandler<MUndoEngineObserver> undoObs = new ObserverHandler<>();
    public void addUndoEngineObserver( MUndoEngineObserver obs) { undoObs.addObserver(obs);}
    public void removeUndoEngineObserver( MUndoEngineObserver obs) { undoObs.removeObserver(obs); }
	
    public static interface MUndoEngineObserver {
    	public void historyChanged(List<UndoIndex> undoHistory);
    	public void undo();
    	public void redo();
    }

    private void triggerHistoryChanged() {
    	List<UndoIndex> list = constructUndoHistory();
    	undoObs.trigger((MUndoEngineObserver obs) -> {obs.historyChanged(list);});
    } 
    private void  triggerUndo() {
    	undoObs.trigger((MUndoEngineObserver obs) -> {obs.undo();});
    }
    private void  triggerRedo() {
    	undoObs.trigger((MUndoEngineObserver obs) -> {obs.redo();});
    }

	public void cleanup() {
		for( UndoContext context : contexts)
			context.flush();
	}
}
