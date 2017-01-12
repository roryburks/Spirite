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
	static final int TICKS_PER_KEY = 10;
	int maxCacheSize = 2000000;
	int cacheSize = 0;
	final List<UndoContext> contexts = new ArrayList<UndoContext>();
	final LinkedList<UndoContext> queue = new LinkedList<>();
	ListIterator<UndoContext> queuePosition = null;
	final ImageWorkspace workspace;
	
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
				return;
			}
		}
		if( storedContext == null) {
			storedContext = (data == null) ?
					new NullContext() :
					new ImageContext( data);
			contexts.add(storedContext);
		}
		
		

		cleanUnusedContexts();
	}
	
	private void cleanUnusedContexts() {
		Iterator<UndoContext> it = contexts.iterator();
		
		while( it.hasNext()) {
			UndoContext uc = it.next();
			if( !(uc instanceof NullContext) && uc.isEmpty()) {
				System.out.println("Removing unused context");
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

		abstract void startIterate();
		abstract UndoAction iterateNext();
		abstract UndoAction getLast();
		
		abstract boolean isEmpty();
	}
	
	/***
	 * ImageContext is the default UndoContext
	 */
	private class ImageContext extends UndoContext {
		List<BufferedImage> keyframes = new ArrayList<>();
		List<UndoAction> actions = new ArrayList<>();
		int met = 0;
		
		ImageContext( ImageData data) {
			super(data);
			BufferedImage toCopy = data.getData();
			
			cacheSize += toCopy.getWidth() * toCopy.getHeight() + 4;
			
			// DeepCopy the BufferedImage
			BufferedImage copy = new BufferedImage( 
					toCopy.getColorModel(),
					toCopy.copyData(null),
					toCopy.isAlphaPremultiplied(),
					null);
			keyframes.add( copy);
		}
		
		
		public void addAction( UndoAction action) {
			if( met == TICKS_PER_KEY) {
				// TODO
			}
			actions.add(action);
			met++;
		}
		
		@Override
		void undo() {
			Graphics g = image.getData().getGraphics();
			
			// Refresh the Image to the current base keyframe
			Graphics2D g2 = (Graphics2D)g;
			Composite c = g2.getComposite();
			g2.setComposite( AlphaComposite.getInstance(AlphaComposite.DST_IN));
			//g2.setColor( new Color(0,0,0,0));
			g.drawImage( keyframes.get(0), 0, 0,  null);

			g2.setComposite( c);
			
			met--;
			for( int i = 0; i < met; ++i) {
				actions.get(i).performAction(image);
			}
			
			workspace.triggerImageRefresh();
		}

		@Override
		void redo() {
			met++;
			if( met > actions.size()) {
				MDebug.handleError(ErrorType.STRUCTURAL, this, "Undo Outer queue desynced with inner queue.");
				return;
			}
			for( int i = 0; i < met; ++i) {
				actions.get(i).performAction(image);
			}
			workspace.triggerImageRefresh();
		}
		
		/***
		 * Makes it so that all information after the current marker is deleted
		 */
		@Override
		void cauterize() {
			List<UndoAction> subList = actions.subList(met, actions.size());
			
			for( UndoAction action : subList) {
				action.onCauterize();
			}
			subList.clear();
		}

		
		// :::: Iterating
		int iterMet;
		@Override
		void startIterate() {
			iterMet = 0;
		}

		@Override
		UndoAction iterateNext() {
			return actions.get(iterMet++);
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
				return actions.get(actions.size()-1);
		}
	}
	
	/***
	 * NullContext is a special context that is not associated with any ImageData
	 * As such the concept of keyframes to work from doesn't make sense.  Instead
	 * Each task is stored.  But since you are not strictly working forwards, the
	 * UndoActions associated with NullContext must have two-way methods for
	 * performing AND undoing
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
