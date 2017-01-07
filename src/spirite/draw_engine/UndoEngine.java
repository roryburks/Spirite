package spirite.draw_engine;

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
import spirite.draw_engine.DrawEngine.StrokeEngine;
import spirite.draw_engine.DrawEngine.StrokeParams;
import spirite.image_data.GroupTree;
import spirite.image_data.ImageData;
import spirite.image_data.ImageWorkspace;

/***
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
 * @author Rory Burks
 *
 */
public class UndoEngine {
	static final int TICKS_PER_KEY = 10;
	int maxCacheSize = 2000000;
	int cacheSize = 0;
	List<UndoContext> contexts = new ArrayList<UndoContext>();
	LinkedList<UndoContext> queue = new LinkedList<>();
	ListIterator<UndoContext> queuePosition = null;
	ImageWorkspace workspace;
	
	public UndoEngine(ImageWorkspace workspace) {
		this.workspace = workspace;
		contexts.add( new NullContext());
	}
	
	
	public int getQueuePosition() {
		if( queuePosition == null) 
			return queue.size();
		else
			return queuePosition.nextIndex();
	}
	
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
	 * Undo History that makes very little sense to the Undo Engine but is
	 * more easily interpreted into user-readable data
	 */
	public List<UndoIndex> constructUndoHistory() {
		// !!!! NOTE: this leaks data access
		List<UndoIndex> list = new ArrayList<>(queue.size());
		
		// Prepare all the UndoContexts to create a queue from their first data
		for( UndoContext context : contexts ) {
			context.startSeek();
		}
		
		// Get Each UndoAction from 
		Iterator<UndoContext> it = queue.iterator();
		while( it.hasNext()) {
			UndoContext context= it.next();
			
			list.add( new UndoIndex( context.image, context.seekGet()));
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

			System.out.println(queue.size());
			

			for( UndoContext context : contexts) {
				context.cauterize();
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
				return;
			}
		}
		if( storedContext == null) {
			storedContext = (data == null) ?
					new NullContext() :
					new ImageContext( data);
			contexts.add(storedContext);
		}
		
		
	}
	
	/***
	 * Attempts to undo an action.
	 * @return true if it was successful, false otherwise (if it has no earlier data)
	 */
	public boolean undo() {
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
	public boolean redo() {
		if( queuePosition == null || !queuePosition.hasNext()) {
			return false;
		}
		else {
			queuePosition.next().redo();
			triggerRedo();
			return true;
		}
	}
	
	private abstract class UndoContext {
		ImageData image;

		UndoContext( ImageData data) {
			this.image = data;
		}

		abstract void addAction( UndoAction action);
		abstract void undo();
		abstract void redo();
		abstract void cauterize();

		abstract void startSeek();
		abstract UndoAction seekGet();
	}
	
	/***
	 * An Undo Context is tied to a ImageData object.  It stores the ImageData's previous
	 * 	BufferedImage states.  Instead of saving one BufferedImage per undo action, it 
	 *  stores only one every N undo actions, while storing the action 
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
			g2.setColor( new Color(0,0,0,0));
			g.drawImage( keyframes.get(0), 0, 0,  null);

			g2.setComposite( c);
			
			met--;
			for( int i = 0; i < met; ++i) {
				actions.get(i).performAction(image);
			}
			
			workspace.refreshImage();
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
			workspace.refreshImage();
		}
		
		/***
		 * Makes it so that all information after the current marker is deleted
		 */
		@Override
		void cauterize() {
			actions.subList(met, actions.size()).clear();
		}

		
		// :::: Seeking
		int seekMet;
		@Override
		void startSeek() {
			seekMet = 0;
		}

		@Override
		UndoAction seekGet() {
			return actions.get(seekMet++);
		}


	}
	
	/***
	 * NullContext is a special context that is not associated with any ImageData
	 * As such the concept of keyframes to work from doesn't make sense.  Instead
	 * Each task is stored.  But since you are not strictly working forwards, the
	 * UndoActions assosciated with NullContext must have two-way methods for
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
				actions.subList(pointer.nextIndex(), actions.size()).clear();
				pointer = null;
			}
		}


		Iterator<NullAction> seekPointer = null;
		@Override
		void startSeek() {
			seekPointer = actions.iterator();
		}
		
		@Override
		UndoAction seekGet() {
			if( !seekPointer.hasNext()) {
				MDebug.handleError(ErrorType.STRUCTURAL, this, "Undo Outer queue desynced with inner queue (Null Redo).");
				return null;
			}
			else
				return seekPointer.next();
				
		}
	}
	
	/***
	 *  UndoActions
	 *
	 */
	public class UndoAction {
		public void performAction( ImageData data) {}
	}
	
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
			StrokeEngine engine = de.createStrokeEngine(data);
			de.fill(p.x, p.y, c, data);
		}
	}
	
	public abstract class NullAction extends UndoAction {
		public abstract void undoAction();
	}
	public class VisibilityAction extends NullAction {
		GroupTree.Node node;
		boolean setTo;
		public VisibilityAction( GroupTree.Node node, boolean setTo) {
			this.node = node;
			this.setTo = setTo;
		}
		@Override
		public void performAction( ImageData data) {
			node.setVisible(this.setTo, false);
		}
		@Override
		public void undoAction() {
			node.setVisible(!this.setTo, false);
		}
	}
	
	// Undo Engine
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
