package spirite.image_data;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import javax.imageio.ImageIO;

import spirite.MDebug;
import spirite.MDebug.ErrorType;
import spirite.image_data.DrawEngine.StrokeEngine;
import spirite.image_data.DrawEngine.StrokeParams;
import spirite.image_data.ImageWorkspace.StructureChange;

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
 * draw commands in the Penner program might seem like redundant code for
 * many tasks, for ones that require continuous input (in particular, Strokes)
 * they can't be generalized in that way.
 * 
 * Events which update the undo engine with undo-able commands should come from
 *  a limited number of places to promote code maintainability.  As of now, only:
 * -ImageWorkspace
 * -DrawEngine
 * 
 * @author Rory Burks
 *
 */
public class UndoEngine {
	static final int MAX_TICKS_PER_KEY = 10;
	int absoluteMaxCache = 200000000;	// 200 mb
	int maxCacheSize = 50000000;	// 50 mb
	int maxQueueSize = 100;
	int cacheSize = 0;
	final List<UndoContext> contexts = new ArrayList<UndoContext>();
	final LinkedList<UndoContext> queue = new LinkedList<>();
	ListIterator<UndoContext> queuePosition = null;
	final ImageWorkspace workspace;
	
	// TODO Implement 
	enum CullBehavior {
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
	CullBehavior cullBehavior = CullBehavior.CACHE_AND_COUNT;
	
	UndoAction getMostRecentAction() {
		if( queue.size() == 0)
			return null;
		return queue.getLast().getLast();
	}
	
	public UndoEngine(ImageWorkspace workspace) {
		this.workspace = workspace;
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
	 * TODO : Consider whether it's worth centralizing these kind of things 
	 * in the ImageWorskpace with a series of Checkouts/Checkins
	 */
	public void prepareContext( ImageData data) {
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
	 * if it is not, then the first undoable action on the ImageData will not be
	 * undoable.
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
	
	private void cull() {
		while( cacheSize > absoluteMaxCache)
			clipTail();
		
		switch( cullBehavior) {
		case CACHE_AND_COUNT:
			while( cacheSize > maxCacheSize && queue.size() > cacheSize)
				clipTail();
			break;
		case ONLY_CACHE:
			while( cacheSize > maxCacheSize)
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
	 * An Undo Context is tied to a ImageData object.  It stores the ImageData's previous
	 * 	BufferedImage states.  Instead of saving one BufferedImage per undo action, it 
	 *  stores only one every N undo actions, while storing the action 
	 */
	private abstract class UndoContext {
		ImageData image;

		UndoContext( ImageData data) {
			this.image = data;
		}

		abstract void addAction( UndoAction action);
		abstract void undo();
		abstract void redo();
		abstract void cauterize();
		abstract void clipTail();

		abstract void startIterate();
		abstract UndoAction iterateNext();
		abstract UndoAction getLast();
		
		abstract boolean isEmpty();
		
		void flush() {}
	}
	
	/***
	 * ImageContext is the default UndoContext
	 */
	private class ImageContext extends UndoContext {
//		List<BufferedImage> keyframes = new ArrayList<>();
		List<UndoAction> actions = new ArrayList<>();
		int pointer = 0;	// The position on the actionsList
		int met = 0;	// Amount of actions it's been since a Keyframe
		int vstart = 0;	// The first "valid" action.  As the tail is clipped,
						// this increments until it hits a Keyframe, then it removes
						// the old base keyframe and adjusts
		
		ImageContext( ImageData data) {
			super(data);
			
			actions.add(new KeyframeAction(deepCopy(data.getData()), null));
			met = 0;
			pointer = 0;
		}

		class KeyframeAction extends UndoAction {
			BufferedImage frame;
			UndoAction hiddenAction;
			KeyframeAction( BufferedImage frame, UndoAction action) {
				this.frame = frame;
				this.hiddenAction = action;
			}
			@Override		
			public void performAction(ImageData data) {	
				resetToKeyframe(frame);
			}
		}
		
		private BufferedImage deepCopy( BufferedImage toCopy) {
			cacheSize += toCopy.getWidth() * toCopy.getHeight() * 4;
			System.out.println(cacheSize);
			
			return new BufferedImage( 
					toCopy.getColorModel(),
					toCopy.copyData(null),
					toCopy.isAlphaPremultiplied(),
					null);
			
			
		}
		private void resetToKeyframe( BufferedImage frame) {
			Graphics g = image.getData().getGraphics();
			Graphics2D g2 = (Graphics2D)g;
			Composite c = g2.getComposite();
			g2.setComposite( AlphaComposite.getInstance(AlphaComposite.SRC));
			g2.drawImage( frame, 0, 0,  null);
			g2.setComposite( c);
			
		}
		
		
		public void addAction( UndoAction action) {
			
			met++;
			pointer++;
			
			// The second half of this conditional is mostly debug to
			//	test if the dynamic keyframe distance is working, but 
			if( met == MAX_TICKS_PER_KEY || action instanceof FillAction) {
				actions.add(new KeyframeAction(deepCopy(image.getData()), action));
				met = 0;
			}
			else {
				actions.add(action);
			}
		}
		
		@Override
		void undo() {
			pointer--;
			met--;
			if( pointer < 0) {
				MDebug.handleError(ErrorType.STRUCTURAL, this, "Internal Undo attempted before start of context.");
			}
			
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
			actions.get(pointer-met).performAction(image);

			for( int i = pointer - met; i <= pointer; ++i) {
				actions.get(i).performAction(image);
			}
			
			workspace.triggerImageRefresh();
		}

		@Override
		void redo() {
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
				actions.get(i).performAction(image);
			}
			workspace.triggerImageRefresh();
		}
		
		/***
		 * Makes it so that all information after the current marker is deleted
		 */
		@Override
		void cauterize() {
			List<UndoAction> subList = actions.subList(pointer+1, actions.size());
			
			// Iterate backwards so you can delete keyframes by the end
			//	so that you don't mess up indices
			ListIterator<UndoAction> it = subList.listIterator(subList.size());
			
			while( it.hasPrevious()){
				UndoAction action = it.previous();
				
				if( action instanceof KeyframeAction) {
					// Remove the keyframe and flush its data to make absolutely
					//	sure there are no leaks.
					BufferedImage image = ((KeyframeAction)action).frame;
					cacheSize -= image.getWidth() * image.getHeight() * 4;
					image.flush();
				}
			}
			subList.clear();
		}

		
		// :::: Iterating
		int iterMet;
		@Override
		void startIterate() {
			iterMet = 1;
		}

		@Override
		UndoAction iterateNext() {
			UndoAction action = actions.get(iterMet++);
			
			if( action instanceof KeyframeAction) {
				return ((KeyframeAction)action).hiddenAction;
			}
			return action;
		}


		@Override
		boolean isEmpty() {
			return actions.size() <= vstart+1;
		}


		@Override
		UndoAction getLast() {
			if( actions.isEmpty())
				return null;
			else
				return actions.get(actions.size()-1);
		}
		
		@Override
		void flush() {
			for( UndoAction action: actions) {
				if( action instanceof KeyframeAction) {
					BufferedImage img = ((KeyframeAction)action).frame;
					cacheSize -= 4*img.getWidth()*img.getHeight();
					img.flush();
				}
			}
		}
		@Override
		void clipTail() {
			if( vstart == pointer) {
				MDebug.handleError( ErrorType.STRUCTURAL_MINOR, this, "Tried to clip more than exists in ImageContext");
			}
			
			vstart++;
			if( actions.get(vstart) instanceof KeyframeAction) {
				// Remove the base Keyframe and all actions up until the new keyframe
				try {
					BufferedImage img = ((KeyframeAction)actions.get(0)).frame;
					cacheSize -= 4*img.getWidth()*img.getHeight();
					img.flush();
				} catch ( ClassCastException e) {
					MDebug.handleError( ErrorType.STRUCTURAL, this, "UndoEngine Corruption: First Action in ImageContext wasn't a KeyframeAction");
				}
				
				List<UndoAction> subList = actions.subList(0, vstart);
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
		LinkedList<NullAction> actions = new LinkedList<>();
		ListIterator<NullAction> pointer = null;
		
		NullContext() {
			super(null);
		}

		@Override
		public void addAction(UndoAction action) {
			if( action instanceof NullAction) {
				actions.add( (NullAction) action);
			}
			else 
				MDebug.handleError(ErrorType.STRUCTURAL_MINOR, this, "Attempting to give a null context a non-null Action.");
		}
		@Override
		void undo() {
			if( pointer == null)
				pointer = actions.listIterator(actions.size());
			
			if( !pointer.hasPrevious() || pointer == null) {
				MDebug.handleError(ErrorType.STRUCTURAL, this, "Undo Outer queue desynced with inner queue (Null Undo).");
				return;
			}
			
			pointer.previous().undoAction();
		}
		
		@Override
		void redo() {
			if( pointer == null || !pointer.hasNext()) {
				MDebug.handleError(ErrorType.STRUCTURAL, this, "Undo Outer queue desynced with inner queue (Null Redo).");
				return;
			}
			
			pointer.next().performAction(null);
		}


		@Override
		void cauterize() {
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
		void startIterate() {
			iter = actions.iterator();
		}
		
		@Override
		UndoAction iterateNext() {
			if( !iter.hasNext()) {
				MDebug.handleError(ErrorType.STRUCTURAL, this, "Undo Outer queue desynced with inner queue (Null Redo).");
				return null;
			}
			else
				return iter.next();
		}

		@Override
		boolean isEmpty() {
			return actions.isEmpty();
		}

		@Override
		UndoAction getLast() {
			if( actions.isEmpty())
				return null;
			else
				return actions.getLast();
		}

		@Override
		void clipTail() {
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
		public abstract void performAction( ImageData data);
		public void onCauterize() {}
	}
	
	
	// :::: Image UIndoActions
	//	Actions working directly on the image data
	
	public class StrokeAction extends UndoAction {
		Point[] points;
		StrokeParams params;
		
		public StrokeAction( StrokeParams params, Point[] points){			
			this.params = params;
			this.points = points;
		}
		
		public StrokeParams getParams() {
			// TODO LEAK
			return params;
		}
		
		@Override
		public void performAction( ImageData data) {
			StrokeEngine engine = (new DrawEngine()).createStrokeEngine(data);
			
			engine.startStroke(params, points[0].x, points[0].y);
			
			for( int i = 1; i < points.length; ++i) {
				engine.updateStroke( points[i].x, points[i].y);
				engine.stepStroke();
			}
			
			engine.endStroke();
		}
	}
	public class FillAction extends UndoAction {
		Point p;
		Color c;
		
		public FillAction( Point p, Color c) {
			this.p = p;
			this.c = c;
		}

		@Override
		public void performAction( ImageData data) {
			DrawEngine de = new DrawEngine();
			de.fill(p.x, p.y, c, data);
		}
	}
	
	
	/***
	 * NullAction
	 * 
	 * Associated with the NullContext, a NullAction must not only have
	 * a performAction method, but also an undoAction() since it's a
	 * two-way Action isntead of building from stored states.
	 */
	public abstract class NullAction extends UndoAction {
		public abstract void undoAction();
	}
	public class StructureAction extends NullAction {
		public final StructureChange change;
		
		public StructureAction(StructureChange change) {
			this.change = change;
		}
		
		@Override
		public void performAction(ImageData data) {
			change.execute();
			change.alert(false);
		}
		@Override
		public void undoAction() {
			change.unexecute();
			change.alert(true);
		}
		
		@Override
		public void onCauterize() {
			change.cauterize();
		}
	}
	
	// :::: MUndoEngineObserver
    List<MUndoEngineObserver> undoObservers = new ArrayList<>();

    public void addUndoEngineObserver( MUndoEngineObserver obs) { undoObservers.add(obs);}
    public void removeUndoEngineObserver( MUndoEngineObserver obs) { undoObservers.remove(obs); }

    private void  triggerHistoryChanged() {
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
    
    public static interface MUndoEngineObserver {
    	public void historyChanged(List<UndoIndex> undoHistory);
    	public void undo();
    	public void redo();
    }
}
