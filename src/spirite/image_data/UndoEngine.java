package spirite.image_data;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import spirite.MDebug;
import spirite.MDebug.ErrorType;
import spirite.MUtil;
import spirite.brains.CacheManager;
import spirite.brains.CacheManager.CachedImage;
import spirite.image_data.DrawEngine.StrokeEngine;
import spirite.image_data.DrawEngine.StrokeParams;
import spirite.image_data.ImageWorkspace.ImageChangeEvent;
import spirite.image_data.ImageWorkspace.StackableStructureChange;
import spirite.image_data.ImageWorkspace.StructureChange;
import spirite.image_data.SelectionEngine.Selection;

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
 * Events which update the undo engine with undo-able commands should come from
 *  a limited number of places to promote code maintainability.  As of now, only:
 * -ImageWorkspace
 * -DrawEngine
 * -SelectionEngine
 * 
 * !!!! MASSIVE TODO: Figure out the best way to make selection actions undoable.
 * I want to have each of the following actions undoable:
 * -Form the Selection
 * -Move the selection
 * -Anchor the selection.
 * 
 * The problem is that "Move the Selection" is an action that will change the image
 * data, but is also an action that has logical repercussions (it changes the form
 * and placement of the selection), so it can't be handled by the NullContext
 * without screwing up the ImageContext pipeline and if handled by the 
 * ImageContext, not only would it require a lot of redundant code, but it'd
 * probably screw up the pipeline anyway.
 * 
 * @author Rory Burks
 *
 */
public class UndoEngine {
	private static final int MAX_TICKS_PER_KEY = 10;
	private int absoluteMaxCache = 200000000;	// 200 m
	private int maxCacheSize = 50000000;	// 50 m
	private int maxQueueSize = 100;
	private final List<UndoContext> contexts = new ArrayList<UndoContext>();
	private final LinkedList<UndoContext> queue = new LinkedList<>();
	private ListIterator<UndoContext> queuePosition = null;
	
	private final ImageWorkspace workspace;
	private final CacheManager cacheManager;

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
	
	
	public UndoEngine(ImageWorkspace workspace, CacheManager cacheManager) {
		this.workspace = workspace;
		this.cacheManager = cacheManager;
		contexts.add( new NullContext());
	}
	
	
	/***
	 * Usually called after loading an image or creating a new one, this
	 * method solidifies the current image state, removing any hanging
	 * undoable actions.
	 */
	public void reset() {
		for( UndoContext context : contexts)
			context.flush();
		contexts.clear();
		queue.clear();
		queuePosition = null;
		contexts.add( new NullContext());
		saveSpot = 0;
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
	
	// Called by ImageWorkapce
	boolean atSaveSpot() {
		return saveSpot == getQueuePosition();
	}
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
	public UndoAction getMostRecentAction() {
		if( queue.size() == 0)
			return null;
		return queue.getLast().getLast();
	}
	public static class UndoIndex {
		public ImageData data;
		public UndoAction action;
		public UndoIndex( ImageData data, UndoAction action) {
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
	void prepareContext( ImageData data) {
		for( UndoContext context : contexts) {
			if( context.image == data)
				return;
		}
		
		contexts.add((data == null) ?
				new NullContext() :
				new ImageContext( data));
	}
	
	/***
	 * Stores the given UndoAction into the UndoEngine on the given
	 * ImageData.
	 * 
 	 * Note: the Context should have already been created using prepareContext
	 * when the image was checked out.  If not, the first action will not be
	 * undoable because the previous keyframe will not have been stored.
	 */
	public void storeAction( UndoAction action, ImageData data) {
		
		// Delete all actions stored after the current iterator point
		if( queuePosition != null) {
			queue.subList(queuePosition.nextIndex(), queue.size()).clear();
			
			for( UndoContext context : contexts) {
				context.cauterize();
			}
			
			// Since erasing the timeline might create ghosts 
			workspace.cleanDataCache();
			
			if( queue.size() < saveSpot) {
				saveSpot = -1;
			}
		}
		

		// If the UndoAction is a StackableAction and a compatible entry
		//   is on the top of the stack, modify that entry instead of creating
		//   a new one.
		if( queue.size() != 0 && getQueuePosition() == queue.size() && action instanceof StackableAction) {
			UndoAction lastAction = getMostRecentAction();
			
			if( lastAction.getClass().equals(action.getClass())) {
				StackableAction stackAction = ((StackableAction)lastAction);
				if(stackAction.canStack(action)) {
					stackAction.stackNewAction(action);
					return;
				}
			}
		}
		
		// Determine if the Context for the given ImageData exists
		UndoContext storedContext = null;
		
		for( UndoContext context : contexts) {
			if( context.image == data) {
				// Add the action to the queue
				context.addAction(action);
				storedContext = context;
				
				
				queue.add(storedContext);
				queuePosition = null;
				triggerHistoryChanged();
			}
		}
		if( storedContext == null) {
			storedContext = (data == null) ?
					new NullContext() :
					new ImageContext( data);
			contexts.add(storedContext);
		}
		
		// Cull 
		cull();
	}
	
	/** Checks to see if there are too many UndoActions (using the behavior determined
	 * by the cullBehavior) and gets rid of the oldest undo actions until there
	 * aren't. */
	private void cull() {
		while( queue.size() > 0 && cacheManager.getCacheSize() > absoluteMaxCache) 
			clipTail();
		
		switch( cullBehavior) {
		case CACHE_AND_COUNT:
			while( cacheManager.getCacheSize() > maxCacheSize 
					&& queue.size() > cacheManager.getCacheSize())
				clipTail();
			break;
		case ONLY_CACHE:
			while( cacheManager.getCacheSize() > maxCacheSize
					&& queue.size() > 0)
				clipTail();
			break;
		case ONLY_UNDO_COUNT:
			while( queue.size() > maxQueueSize)
				clipTail();
			break;
			
		}
		// Note: it SHOULD be impossible for a action to be culled if it is
		//	the current pointer on the UndoQueue, but it might happen.
		
		cleanUnusedContexts();
	}
	
	private void clipTail() {
		queue.getFirst().clipTail();
		queue.removeFirst();
	}
	
	private void cleanUnusedContexts() {
		Iterator<UndoContext> it = contexts.iterator();

		while( it.hasNext()) {
			UndoContext uc = it.next();
			if( !(uc instanceof NullContext) && uc.isEmpty()) {
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
			queuePosition.next().redo();
			triggerRedo();
			return true;
		}
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
	private abstract class UndoContext {
		ImageData image;

		UndoContext( ImageData data) {
			this.image = data;
		}

		protected abstract void addAction( UndoAction action);
		protected abstract void undo();
		protected abstract void redo();
		protected abstract void cauterize();
		protected abstract void clipTail();

		protected abstract void startIterate();
		protected abstract UndoAction iterateNext();
		protected abstract UndoAction getLast();
		
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
		
		protected ImageContext( ImageData image) {
			super(image);
			
			actions.add(new KeyframeAction(cacheManager.createDeepCopy(image.readImage().image), null));
			met = 0;
			pointer = 0;
		}

		/** A KeyframeAction is a special kind of ImageAction which instead of
		 * storing the way the image was changed, but instead stores the
		 * entire image as it should appear after the action.  It stores the
		 * action that it's "supposed" to be in hiddenAction and performs
		 * its logical components (if it has any).*/
		private class KeyframeAction extends ImageAction {
			CachedImage frameCache;
			ImageAction hiddenAction;
			KeyframeAction( CachedImage frame, ImageAction action) {
				this.frameCache = frame;
				this.hiddenAction = action;
			}
			@Override 
			public void performAction() {
				if( hiddenAction != null)
				hiddenAction.performAction();
			}
			@Override
			public void undoAction() {
				if( hiddenAction != null)
				hiddenAction.undoAction();
			}
			@Override
			public void performImageAction(ImageData image) {
				resetToKeyframe(frameCache.access());
			}
		}
		
		private void resetToKeyframe( BufferedImage frame) {
			BufferedImage bi = workspace.checkoutImage(image);
			Graphics g = bi.getGraphics();
			Graphics2D g2 = (Graphics2D)g;
			Composite c = g2.getComposite();
			g2.setComposite( AlphaComposite.getInstance(AlphaComposite.SRC));
			g2.drawImage( frame, 0, 0,  null);
			g2.setComposite( c);
			g2.dispose();
			workspace.checkinImage(image);
		}
		
		@Override
		protected void addAction( UndoAction action) {
			if( !(action instanceof ImageAction)) {
				MDebug.handleError(ErrorType.STRUCTURAL, this, "Tried to add a non ImageAction to an ImageContext");
				return;
			}
			final ImageAction iaction = (ImageAction)action;
			
			met++;
			pointer++;
			
			// Record a keyframe if it's been a while or the action is a "HeavyAction"
			//	i.e. an action requiring a lot of computation to perform
			if( met == MAX_TICKS_PER_KEY || iaction.isHeavyAction()) {
				actions.add(new KeyframeAction(cacheManager.createDeepCopy(image.readImage().image), iaction));
				met = 0;
			}
			else {
				actions.add(iaction);
			}
		}
		
		@Override
		protected void undo() {
			pointer--;
			met--;
			if( pointer < 0) {
				MDebug.handleError(ErrorType.STRUCTURAL, this, "Internal Undo attempted before start of context.");
			}

			// Undo the logical action
			actions.get(pointer).undoAction();
			
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
			actions.get(pointer-met).performImageAction(image);

			for( int i = pointer - met; i <= pointer; ++i) {
				actions.get(i).performImageAction(image);
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
				MDebug.handleError(ErrorType.STRUCTURAL, this, "Undo Outer queue desynced with inner queue.");
				return;
			}
			if( actions.get(pointer) instanceof KeyframeAction) {
				met = 0;
			}
			for( int i = pointer - met; i <= pointer; ++i) {
				actions.get(i).performAction();
				actions.get(i).performImageAction(image);
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
				UndoAction action = it.previous();
				
				if( action instanceof KeyframeAction) {
					// Remove the keyframe and flush its data to make absolutely
					//	sure there are no leaks.
					((KeyframeAction)action).frameCache.flush();
				}
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
		protected UndoAction iterateNext() {
			UndoAction action = actions.get(iterMet++);
			
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
		protected UndoAction getLast() {
			if( actions.isEmpty())
				return null;
			else
				return actions.get(actions.size()-1);
		}
		
		@Override
		protected void flush() {
			for( UndoAction action: actions) {
				if( action instanceof KeyframeAction) {
					((KeyframeAction)action).frameCache.flush();
				}
			}
		}
		@Override
		protected void clipTail() {
			if( vstart == pointer) {
				MDebug.handleError( ErrorType.STRUCTURAL_MINOR, this, "Tried to clip more than exists in ImageContext");
			}
			
			vstart++;
			if( actions.get(vstart) instanceof KeyframeAction) {
				// Remove the base Keyframe and all actions up until the new keyframe
				try {
					((KeyframeAction)actions.get(0)).frameCache.flush();
				} catch ( ClassCastException e) {
					MDebug.handleError( ErrorType.STRUCTURAL, this, "UndoEngine Corruption: First Action in ImageContext wasn't a KeyframeAction");
				}
				
				List<ImageAction> subList = actions.subList(0, vstart);
				pointer -= subList.size();
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
	private class NullContext extends UndoContext {
		private final LinkedList<NullAction> actions = new LinkedList<>();
		private ListIterator<NullAction> pointer = null;
		
		NullContext() {
			super(null);
		}

		@Override
		protected void addAction(UndoAction action) {
			if( action instanceof NullAction) {
				actions.add( (NullAction) action);
			}
			else 
				MDebug.handleError(ErrorType.STRUCTURAL_MINOR, this, "Attempting to give a null context a non-null Action.");
		}
		@Override
		protected void undo() {
			if( pointer == null)
				pointer = actions.listIterator(actions.size());
			
			if( !pointer.hasPrevious() || pointer == null) {
				MDebug.handleError(ErrorType.STRUCTURAL, this, "Undo Outer queue desynced with inner queue (Null Undo).");
				return;
			}
			
			pointer.previous().undoAction();
		}
		
		@Override
		protected void redo() {
			if( pointer == null || !pointer.hasNext()) {
				MDebug.handleError(ErrorType.STRUCTURAL, this, "Undo Outer queue desynced with inner queue (Null Redo).");
				return;
			}
			
			pointer.next().performAction();
		}


		@Override
		protected void cauterize() {
			if( pointer != null) {
				List<NullAction> subList = actions.subList(pointer.nextIndex(), actions.size());
				
				for( NullAction action : subList) {
					action.onCauterize();
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
		protected UndoAction iterateNext() {
			if( !iter.hasNext()) {
				MDebug.handleError(ErrorType.STRUCTURAL, this, "Undo Outer queue desynced with inner queue (Null Redo).");
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
		protected UndoAction getLast() {
			if( actions.isEmpty())
				return null;
			else
				return actions.getLast();
		}

		@Override
		protected void clipTail() {
			actions.removeFirst();
		}
	}
	
	/***
	 *  UndoActions
	 *  
	 *  Not only store the data needed to recreate the actions on the image data,
	 *  but also implements the methods to recreate them.
	 */
	public abstract class UndoAction {
		public String description = "";
		protected abstract void performAction();
		protected abstract void undoAction();
		protected void onCauterize() {}
	}
	
	/** ImageActions are associated with ImageContexts, they have two components
	 * to them: a logical component, which calls performAction and undoAction
	 * sequentially as normal, but also has a performImageAction which writes
	 * to the imageData (additively from the last keyframe). */
	public abstract class ImageAction extends UndoAction {
		@Override protected void performAction() {}
		@Override protected void undoAction() {}
		protected abstract void performImageAction( ImageData image);
		protected boolean isHeavyAction() {return false;}
	}
	
	

	/** Associated with the NullContext, a NullAction strictly requires you to
	 * have an UndoAction since there is always a logical component assosciated
	 * with it (otherwise it would have no Actions to it)
	 */
	public abstract class NullAction extends UndoAction {}
	
	/** A StackableAction is an action that if performed multiple times in
	 * a row will automatically group into a single action by calling 
	 * <code>stackNewAction</code> on the old action if <code>canStack</code>
	 * returns true.  */
	public interface StackableAction {
		public void stackNewAction( UndoAction newAction);
		public boolean canStack( UndoAction newAction);
	}
	

	
	// ==== Image Undo Actions ====
	
	public class StrokeAction extends ImageAction {
		Point[] points;
		StrokeParams params;
		
		public StrokeAction( StrokeParams params, Point[] points){			
			this.params = params;
			this.points = points;
			
			switch( params.getMethod()) {
			case BASIC:
				description = "Basic Stroke Action";
				break;
			case ERASE:
				description = "Erase Stroke Action";
				break;
			}
		}
		
		public StrokeParams getParams() {
			return params;
		}
		
		@Override
		protected void performImageAction( ImageData data) {
			StrokeEngine engine = workspace.getDrawEngine().createStrokeEngine(data);
			
			engine.startStroke(params, points[0].x, points[0].y);
			
			for( int i = 1; i < points.length; ++i) {
				engine.updateStroke( points[i].x, points[i].y);
				engine.stepStroke();
			}
			
			engine.endStroke();
		}
	}
	public class FillAction extends ImageAction {
		private final Point p;
		private final Color c;
		
		public FillAction( Point p, Color c) {
			this.p = p;
			this.c = c;
			description = "Fill";
		}

		@Override
		protected void performImageAction( ImageData data) {
			DrawEngine de = workspace.getDrawEngine();
			de.fill(p.x, p.y, c, data);
		}
		public Point getPoint() { return new Point(p);}
		public Color getColor() { return new Color(c.getRGB());}
	}
	public class ClearAction extends ImageAction {
		public ClearAction() {description = "Clear Image";}
		@Override
		protected void performImageAction(ImageData image) {
			BufferedImage bi = workspace.checkoutImage(image);
			MUtil.clearImage(bi);
			workspace.checkinImage(image);
		}
		
	}
	
	
	
	// ==== Null Undo Actions ====
	
	/** NullActions which are handled by the ImageWorkspace are 
	 * StructureActions, whose behavior is defined by StructureChanges
	 * which are used both for the initial execution and the UndoEngine */
	public class StructureAction extends NullAction {
		public final StructureChange change;	// !!! Might be bad visibility
		
		public StructureAction(StructureChange change) {
			this.change = change;
		}
		
		@Override
		protected void performAction() {
			change.execute();
			change.alert(false);
		}
		@Override
		protected void undoAction() {
			change.unexecute();
			change.alert(true);
		}
		
		@Override
		protected void onCauterize() {
			change.cauterize();
		}
	}
	
	/*** A StackableStructureAction is a StructureAction with a 
	 * StackableChange.  The StackableChange implements the stack check
	 * and merge, this class just mediates.	 */
	public class StackableStructureAction extends StructureAction
		implements StackableAction 
	{
		public StackableStructureAction(StructureChange change) {
			super(change);
		}
		@Override
		public void stackNewAction(UndoAction newAction) {
			((StackableStructureChange)change).stackNewChange(
					((StackableStructureAction)newAction).change);
		}
		@Override
		public boolean canStack(UndoAction newAction) {
			return ((StackableStructureChange)change).canStack(
					((StackableStructureAction)newAction).change);
		}
	}
	
	public class SetSelectionAction extends NullAction {
		private int offsetX, offsetY;
		private Selection selection;
		private int poX, poY;
		private Selection pSelection;
		
		SetSelectionAction( Selection selection, int offsetX, int offsetY,
				Selection previousSelection, int previousOX, int previousOY) 
		{
			this.offsetX = offsetX;
			this.offsetY = offsetY;
			this.selection = selection;
			this.pSelection = previousSelection;
			this.poX = previousOX;
			this.poY= previousOY;
			description = "Selection Change.";
		}
		
		@Override
		protected void performAction() {
			workspace.getSelectionEngine().setSelection( selection, offsetX, offsetY);
		}

		@Override
		protected void undoAction() {
			workspace.getSelectionEngine().setSelection( pSelection, poX, poY);
		}
		
	}
	
	
	
	// :::: MUndoEngineObserver
    public static interface MUndoEngineObserver {
    	public void historyChanged(List<UndoIndex> undoHistory);
    	public void undo();
    	public void redo();
    }
    List<MUndoEngineObserver> undoObservers = new ArrayList<>();
    public void addUndoEngineObserver( MUndoEngineObserver obs) { undoObservers.add(obs);}
    public void removeUndoEngineObserver( MUndoEngineObserver obs) { undoObservers.remove(obs); }

    private void triggerHistoryChanged() {
    	List<UndoIndex> list = constructUndoHistory();
    	
    	for( MUndoEngineObserver obs : undoObservers) {
    		obs.historyChanged(list);
    	}
    } 
    private void  triggerUndo() {
    	for( MUndoEngineObserver obs : undoObservers) {
    		obs.undo();
    	}
    }
    private void  triggerRedo() {
    	for( MUndoEngineObserver obs : undoObservers) {
    		obs.redo();
    	}
    }
}
