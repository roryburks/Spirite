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

import spirite.brains.MasterControl;
import spirite.draw_engine.DrawEngine.StrokeEngine;
import spirite.draw_engine.DrawEngine.StrokeParams;
import spirite.image_data.ImageData;

public class UndoEngine {
	MasterControl master;
	static final int TICKS_PER_KEY = 10;
	int maxCacheSize = 2000000;
	int cacheSize = 0;
	List<UndoContext> contexts = new ArrayList<UndoContext>();
	LinkedList<UndoContext> queue = new LinkedList<>();
	ListIterator<UndoContext> queuePosition = null;
	
	public UndoEngine( MasterControl master) {
		this.master = master;
	}
	
	public void prepareContext( ImageData data) {
		for( UndoContext context : contexts) {
			if( context.image == data)
				return;
		}
		
		contexts.add(new UndoContext( data));
	}
	
	public void storeAction( UndoAction action, ImageData data) {
		// Delete all actions stored after the current iterator point
		if( queuePosition != null) {
			Iterator<UndoContext> caret = queue.descendingIterator();
			
			if(queuePosition.hasPrevious()) {
				UndoContext marker = queuePosition.previous();
				while( caret.hasNext()) {
					UndoContext check = caret.next();
					if( check == marker)
						break;
					caret.remove();
				}
			}

			for( UndoContext context : contexts) {
				context.cauterize();
			}
		}
		
		// Determine if the Context for the given ImageData
		// Note: the Context should have already been created using prepareContext
		//	if it is not, then the first undoable action on the ImageData will not be
		//	undoable.
		UndoContext storedContext = null;

		
		for( UndoContext context : contexts) {
			if( context.image == data) {
				// Add the action to the queue
				context.addAction(action);
				storedContext = context;
				
				
				queue.add(storedContext);
				queuePosition = null;
				return;
			}
		}
		if( storedContext == null) {
			storedContext = new UndoContext( data);
			contexts.add(storedContext);
		}
		
	}
	
	public boolean undo() {
		if( queuePosition == null) 
			queuePosition = queue.listIterator(queue.size());
		
		if( !queuePosition.hasPrevious())
			return false;
		else {
			UndoContext toUndo = queuePosition.previous();
			toUndo.undo();
			return true;
		}
	}
	
	public boolean redo() {
		return false;
	}
	
	
	/***
	 * An Undo Context is tied to a ImageData object.  It stores the ImageData's previous
	 * 	BufferedImage states.  Instead of saving one BufferedImage per undo action, it 
	 *  stores only one every N undo actions, while storing the action 
	 *
	 *
	 */
	private class UndoContext {
		ImageData image;
		List<BufferedImage> keyframes = new ArrayList<>();
		List<UndoAction> actions = new ArrayList<>();
		int met = 0;
		
		UndoContext( ImageData data) {
			this.image = data;
			BufferedImage toCopy = data.getData();
			
			cacheSize += toCopy.getWidth() * toCopy.getHeight() + 4;
			
			// DeepCopy
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
			System.out.println(action);
		}
		
		public void undo() {
			System.out.println("Inner Undo");
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
				System.out.println(met);
				actions.get(i).performAction(image);
			}
			
			master.refreshImage();
		}
		
		/***
		 * Makes it so that all information after the curent marker is deleted
		 */
		protected void cauterize() {
			actions.subList(met, actions.size()).clear();
		}
	}
	
	
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
		
		@Override
		public void performAction( ImageData data) {
			StrokeEngine engine = master.getDrawEngine().createStrokeEngine(data);
			
			engine.startStroke(params, points[0].x, points[0].y);
			
			for( int i = 1; i < points.length; ++i) {
				engine.updateStroke( points[i].x, points[i].y);
				engine.stepStroke();
			}
			
			engine.endStroke();
		}
	}
	public class VisibilityAction extends UndoAction {
		
	}
}
