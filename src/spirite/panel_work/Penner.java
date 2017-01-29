package spirite.panel_work;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

import javax.swing.Timer;

import jpen.PButton;
import jpen.PButtonEvent;
import jpen.PKindEvent;
import jpen.PLevel;
import jpen.PLevelEvent;
import jpen.PScrollEvent;
import jpen.event.PenListener;
import spirite.MUtil;
import spirite.brains.MasterControl;
import spirite.brains.PaletteManager;
import spirite.brains.ToolsetManager;
import spirite.brains.ToolsetManager.Tool;
import spirite.brains.ToolsetManager.ToolSettings;
import spirite.image_data.DrawEngine;
import spirite.image_data.DrawEngine.Method;
import spirite.image_data.DrawEngine.PenState;
import spirite.image_data.DrawEngine.StrokeAction;
import spirite.image_data.DrawEngine.StrokeEngine;
import spirite.image_data.DrawEngine.StrokeParams;
import spirite.image_data.GroupTree;
import spirite.image_data.ImageHandle;
import spirite.image_data.ImageWorkspace;
import spirite.image_data.RenderEngine;
import spirite.image_data.RenderEngine.RenderSettings;
import spirite.image_data.SelectionEngine;
import spirite.image_data.SelectionEngine.Selection;
import spirite.image_data.SelectionEngine.SelectionType;
import spirite.image_data.UndoEngine;
import spirite.panel_work.WorkPanel.Zoomer;

/***
 * The Penner translates Pen and Mouse input, particularly from the draw
 * panel and then translates them into actions to be performed by the 
 * DrawEngine.
 * 
 * Uses the JPen2 library which requires the JPen DLLs to be accessible.
 *
 * @author Rory Burks
 */
public class Penner 
	implements PenListener, KeyEventDispatcher, ActionListener
{
	// Contains "Image to Screen" and "Screen to Image" methods.
	//	Could possibly wrap them in an interface to avoid tempting Penner 
	//	with UI controls
	private final WorkPanel context;
	private final Zoomer zoomer;	
	private final Timer update_timer;
	private StrokeEngine strokeEngine = null;
	
	// It might be easier to just link Master instead of linking every
	//	single Manager in the kitchen, but I like the idea of encouraging
	//	thinking about why you need a component before actually linking it.
	private final ImageWorkspace workspace;
	private final SelectionEngine selectionEngine;
	private final UndoEngine undoEngine;
	private final DrawEngine drawEngine;
	private final ToolsetManager toolsetManager;
	private final PaletteManager paletteManager;
	private final RenderEngine renderEngine;	// used for color picking.
												// might not belong here, maybe in DrawEngine
	

	private boolean holdingShift = false;
	private boolean holdingCtrl = false;
	private int shiftMode = 0;	// 0 : accept any, 1 : horizontal, 2: vertical
	
	// Naturally, being a mouse (or drawing tablet)-based input handler with
	//	a variety of states, a large number of coordinate sets need to be 
	//	remembered.  While it's possible that some of these could be condensed
	//	into fewer variables, it wouldn't be worth it just to save a few bytes
	//	of RAM
	private int startX;	// Set at the start 
	private int startY;
	private int shiftX;	//shiftX/Y are used to try and determine if, when holding
	private int shiftY;	//	shift, you are drawing vertically or horizontally
						// 	there are a few pixels of leniency before it determines
	private int wX;		// wX and wY are the semi-raw coordinates which are just
	private int wY;		// 	the raw positions converted to ImageSpace whereas x and y
						// 	sometimes do not get updated (e.g. when shift-locked)
	private int oldX;	// OldX and OldY are the last-checked X and Y primarily used
	private int oldY;	// 	for things that only happen if they change
	private int rawX;	// raw position are the last-recorded coordinates in pure form
	private int rawY;	// 	(screen coordinates relative to the component Penner watches over)
	private int oldRawX;	// Similar to oldX and oldY but for 
	private int oldRawY;
	
	
	private float pressure = 1.0f;
	
	private Tool activeTool = null;
	
	private int x, y;
	
	private enum STATE { 
		READY, DRAWING, FORMING_SELECTION, MOVING_SELECTION, MOVING_NODE,
		PICKING,
		
		// stateVar for CROPPING:
		//	0 Means you're building a new Crop
		//	1 Means you're not hovering over anything in particular (but a 
		//		Cropping Rectangle is built
		//	3rd Bit (0x04) represents the user Hovering over a certain section
		//		if no other bits are set it's the middle section.
		//	4th Bit (0x08) is set if you're resizing (as determined by 3rd Hex)
		//	
		//	The 3rd Hex word determines which dimensions he's resizing
		//	0x100 : Top
		//	0x200 : Bottom
		//	0x400 : Left
		//	0x800 : Right
		// stateObj stores the built CroppingRectangle if stateVar != 0
		CROPPING, 
		
		// Reference-Related
		REF_GLOBAL_MOVE, REF_FINE_ZOOM,
		REF_NODE_MOVE
	};
	private STATE state = STATE.READY;
	private int stateVar = 0;
	private Object stateObj = null;
	
	public Penner( DrawPanel draw_panel, MasterControl master) {
		this.zoomer = draw_panel.zoomer;
		this.context = draw_panel.context;
		this.workspace = draw_panel.workspace;
		this.selectionEngine = workspace.getSelectionEngine();
		this.undoEngine = workspace.getUndoEngine();
		this.drawEngine = workspace.getDrawEngine();
		this.toolsetManager = master.getToolsetManager();
		this.paletteManager = master.getPaletteManager();
		this.renderEngine = master.getRenderEngine();

		// Add Timer and KeyDispatcher
		//	Note: since these are utilities with a global focus that you're
		//	giving references of Penner to, you will need to clean them up
		//	so that Penner (and everything it touches) gets GC'd
		update_timer = new Timer(16, this);
		update_timer.setRepeats(true);
		update_timer.start();
		
		KeyboardFocusManager.getCurrentKeyboardFocusManager()
			.addKeyEventDispatcher(this);
	}
	
	// Since Penner mostly deals in Image Coordinates, not Screen Coordinates,
	//	when the Image Space changes (such as on zoom-in), the coordinates need
	//	to be updated.
	public void refreshCoordinates() {
		rawUpdateX(rawX);
		rawUpdateY(rawY);
	}

	// :::: PenListener
	@Override	public void penScrollEvent(PScrollEvent arg0) {}
	@Override	public void penTock(long arg0) {
		
	}
	@Override
	public void penButtonEvent(PButtonEvent pbe) {
		
		if( pbe.button.getType() == PButton.Type.SHIFT &&
				pbe.button.value) 
		{
			shiftX = wX;
			shiftY = wY;
			shiftMode = 0;
		}
		if( pbe.button.typeNumber > 3) return;	// Shit/Ctrl/Etc events
		
		if( pbe.button.value == true) {
			x = wX;
			y = wY;
			startX = x;
			startY = y;
			shiftX = wX;
			shiftY = wY;
			shiftMode = 0;
			
			
			if( workspace.isEditingReference()) {
				if( holdingCtrl)
					setState( STATE.REF_FINE_ZOOM);
				else
					setState(  STATE.REF_GLOBAL_MOVE);
				
			}
			else {
				PButton.Type button = pbe.button.getType();
				
				if( button != PButton.Type.LEFT && button != PButton.Type.RIGHT && button != PButton.Type.CENTER)
					return;
				
				Tool tool = toolsetManager.getSelectedTool();
				
				
				switch( tool) {
				case PEN:
					if( holdingCtrl)  {
						pickColor(button == PButton.Type.LEFT);
						setState(  STATE.PICKING, (button == PButton.Type.LEFT)?1:0);
					}
					else
						startPen( button == PButton.Type.LEFT);
					break;
				case ERASER:
					startErase();
					break;
				case FILL:
					fill( button == PButton.Type.LEFT);
					break;
				case BOX_SELECTION:
					startSelection();
					break;
				case MOVE:
					startMove();
					break;
				case COLOR_PICKER:
					pickColor( button == PButton.Type.LEFT);
					setState( state = STATE.PICKING);
					stateVar = (button == PButton.Type.LEFT)?1:0;
					break;
				case PIXEL:
					if( holdingCtrl)  {
						pickColor(button == PButton.Type.LEFT);
						setState( state = STATE.PICKING);
						stateVar = (button == PButton.Type.LEFT)?1:0;
					}
					else
						startPixel( button == PButton.Type.LEFT);
					break;
				case CROP:
					if( state == STATE.CROPPING)
					{
						if( (Rectangle)stateObj == null ||
							!((Rectangle)stateObj).contains(x, y)) {
							setState(STATE.CROPPING);
							break;
						}
						
						if( stateVar == 0x04) {
							workspace.cropNode(workspace.getSelectedNode(), (Rectangle)stateObj);
							setState( STATE.READY);
						}
						else  {
							stateVar |= 0x08;
						}
					}
					else
						setState( STATE.CROPPING);
					break;
				}

				activeTool = tool;
			}
			
			
/*			if( selectionEngine.getSelection() != null
					&& state != STATE.FORMING_SELECTION
					&& state != STATE.MOVING_SELECTION) 
			{
				selectionEngine.unselect();
			}*/
			
		}
		else {
			// Pen up
			switch( state) {
			case DRAWING:
				if( strokeEngine != null) {
					strokeEngine.endStroke();
					
					StrokeAction stroke = drawEngine.new StrokeAction( 
							strokeEngine.getParams(), 
							strokeEngine.getHistory(),
							strokeEngine.getLastSelection(),
							strokeEngine.getImageData());
					undoEngine.storeAction( stroke);
					strokeEngine = null;
				}
				setState( STATE.READY);
				break;
			case FORMING_SELECTION:
				selectionEngine.finishBuildingSelection();
				setState( STATE.READY);
				break;
			case CROPPING: {
				ToolSettings settings = toolsetManager.getToolSettings(Tool.CROP);

				if( (stateVar & 0x8) != 0)
					stateVar ^= 0x8;
				
				if( stateVar == 0) {
					Rectangle rect = MUtil.rectFromEndpoints(
							startX, startY, x, y);
					stateObj = rect;
					if( (Boolean)settings.getValue("quickCrop")) {
						workspace.cropNode(
								workspace.getSelectedNode(), 
								new Rectangle( rect.x, rect.y, rect.width, rect.height));
						setState( STATE.READY);
					}
					stateVar = 1;
				}
				break;}
			default:
				setState( STATE.READY);
				break;
			}
		}
		
	}

	// :::: Start Methods
	private void startPen( boolean leftClick) {
		ToolSettings settings = toolsetManager.getToolSettings(Tool.PEN);
		StrokeParams stroke = new StrokeParams();
		Color c = (leftClick) ? 
				paletteManager.getActiveColor(0)
				: paletteManager.getActiveColor(1);
		stroke.setColor( c);
		stroke.setWidth((float)settings.getValue("width"));
		stroke.setAlpha((float)settings.getValue("alpha"));
		
		// Start the Stroke
		startStroke( stroke);
	}
	private void startErase() {
		ToolSettings settings = toolsetManager.getToolSettings(Tool.ERASER);
		StrokeParams stroke = new StrokeParams();
		stroke.setMethod( Method.ERASE);
		stroke.setWidth((float)settings.getValue("width"));

		// Start the Stroke
		startStroke( stroke);
	}	
	private void startPixel( boolean leftClick) {
		StrokeParams stroke = new StrokeParams();
		stroke.setMethod( Method.PIXEL);
		Color c = (leftClick) ? 
				paletteManager.getActiveColor(0)
				: paletteManager.getActiveColor(1);
		stroke.setColor( c);
		startStroke( stroke);
		
	}
	private void startStroke( StrokeParams stroke) {
		if( workspace != null && workspace.getActiveData() != null) {
			ImageHandle data = workspace.getActiveData();
			GroupTree.Node node = workspace.getSelectedNode();

			strokeEngine = drawEngine.startStrokeEngine( data);
			
			if( strokeEngine.startStroke(stroke,  new PenState(
							x - node.getOffsetX(), y - node.getOffsetY(), pressure))) {
				data.refresh();
			}
			setState( STATE.DRAWING);
		}
	}
	private void startSelection() {
		Selection selection = selectionEngine.getSelection();
		
		if( selection != null && selection.contains(x-selectionEngine.getOffsetX(),y-selectionEngine.getOffsetY())) {
			setState(  STATE.MOVING_SELECTION);
		}
		else {
			selectionEngine.startBuildingSelection(SelectionType.RECTANGLE, x, y);
			setState( STATE.FORMING_SELECTION);
		}
	}
	private void startMove() {
		Selection selection = selectionEngine.getSelection();
		
		if(selection != null) {
			setState(  STATE.MOVING_SELECTION);
		}
		else {
			setState( state = STATE.MOVING_NODE);
		}
	}
	private void pickColor( boolean leftClick) {
		// Get the composed image
		RenderSettings settings = new RenderSettings();
		settings.workspace = workspace;
		BufferedImage img = renderEngine.renderImage(settings);
		
		if( !MUtil.coordInImage(x, y, img))
			return;
		paletteManager.setActiveColor(
				(leftClick)?0:1, new Color(img.getRGB(x, y)));
	}
	private void fill( boolean leftClick) {
		// Determine Color
		Color c = (leftClick) ? 
				paletteManager.getActiveColor(0)
				: paletteManager.getActiveColor(1);
				
		if( holdingCtrl) c = new Color(0,0,0,0);

		// Grab the Active Data
		ImageHandle data = workspace.getActiveData();
		GroupTree.Node node = workspace.getSelectedNode();
		
		if( data != null && node != null) {
			// Perform the fill Action, only store the UndoAction if 
			//	an actual change is made.
			Point p = new Point(x - node.getOffsetX(), y - node.getOffsetY());
			drawEngine.fill( p.x, p.y, c, data);
		} 
	}
	
	

	@Override
	public void penKindEvent(PKindEvent pke) {
		switch( pke.kind.getType()) {
		case CURSOR:
			toolsetManager.setCursor(ToolsetManager.Cursor.MOUSE);
			break;
		case STYLUS:
			toolsetManager.setCursor(ToolsetManager.Cursor.STYLUS);
			break;
		case ERASER:
			toolsetManager.setCursor(ToolsetManager.Cursor.ERASER);
			break;
		default:
			break;
		}
		
	}

	
	private void rawUpdateX( int raw) {
		rawX = raw;
		wX = zoomer.stiXm(rawX);
		if( holdingShift && state == STATE.DRAWING) {
			if( shiftMode == 2)
				return;
			if( shiftMode == 0) {
				if( Math.abs(shiftX - rawX) > 10) {
					shiftMode = 1;
				}
				else return;
			}
		}
		x = wX;
	}
	private void rawUpdateY( int raw) {
		rawY = raw;
		wY = zoomer.stiYm( rawY);
		if( holdingShift && state == STATE.DRAWING) {
			if( shiftMode == 1)
				return;
			if( shiftMode == 0) {
				if( Math.abs(shiftY - rawY) > 10) {
					shiftMode = 2;
				}
				else return;
			}
		}
		y = wY;
	}
	@Override
	public void penLevelEvent(PLevelEvent ple) {
		// Note: JPen updates PenLevels (which inform of things like position and pressure)
		//	asynchronously with press buttons and other such things, so you have to be careful.
		for( PLevel level: ple.levels) {
			switch( level.getType()) {
			case X:
				rawUpdateX(Math.round(level.value));
				break;
			case Y:
				rawUpdateY(Math.round(level.value));
				break;
			case PRESSURE:
				pressure = level.value;
				break;
			default:
				break;
			}
		}
		
		context.refreshCoordinates(x, y);
		GroupTree.Node node= workspace.getSelectedNode();// !!!! Maybe better to store which node you're moving locally
		
		// Perform state-based "on-pen/mouse move" code
		switch( state) {
		case DRAWING:
			if( strokeEngine != null && node != null) {
				strokeEngine.updateStroke( new PenState(						
						x - node.getOffsetX(), y - node.getOffsetY(), pressure));
			}
			break;
		case FORMING_SELECTION:
			selectionEngine.updateBuildingSelection(x, y);
			break;
		case MOVING_SELECTION:
			if( oldX != x || oldY != y) 
				selectionEngine.setOffset(
						selectionEngine.getOffsetX() + (x - oldX),
						selectionEngine.getOffsetY() + (y - oldY));
			break;
		case MOVING_NODE:
			if( node != null && (oldX != x || oldY != y))
				node.setOffset( node.getOffsetX() + (x - oldX), 
								 node.getOffsetY() + (y - oldY));
			break;
		case PICKING:
			pickColor( stateVar == 1);
			break;
			
		case REF_GLOBAL_MOVE:
			context.refzoomer.setCX( Math.round(context.refzoomer.getCX()+ (x - oldX)*context.refzoomer.getZoom()/context.refzoomer.getRawZoom()));
			context.refzoomer.setCY( Math.round(context.refzoomer.getCY()+ (y - oldY)*context.refzoomer.getZoom()/context.refzoomer.getRawZoom()));
			break;
		case REF_FINE_ZOOM:
			context.refzoomer.setFineZoom((float) (context.refzoomer.getRawZoom() * Math.pow(1.0005, 1+(rawY - oldRawY))));
			break;
		case CROPPING:
			Rectangle rect = (Rectangle)stateObj;

			if( (stateVar & 0x08) != 0) {
				if( (stateVar & 0x100) != 0) {
					// Top
					rect.y += (y - oldY);
					rect.height -= (y - oldY);
				}
				if( (stateVar & 0x200) != 0) {
					// Bottom
					rect.height += (y - oldY);
				}
				if( (stateVar & 0x400) != 0) {
					// Left
					rect.x += (x - oldX);
					rect.width -= (x - oldX);
				}
				if( (stateVar & 0x800) != 0) {
					// Right
					rect.width+= (x - oldX);
				}
			}
			break;
		case REF_NODE_MOVE:
		case READY:
		}
		
		if( oldX != x || oldY != y && drawsOverlay()) {
			context.repaint();
		}
		
		oldX = x;
		oldY = y;
		oldRawX = rawX;
		oldRawY = rawY;
	}

	public void paintOverlay( Graphics g) {
		Graphics2D g2 = (Graphics2D)g;
		
		switch(state) {
		case CROPPING:
			int x1, y1, x2, y2;
			
			Rectangle rect = (stateVar == 0)
					? rect = MUtil.rectFromEndpoints(startX, startY, x, y)
					: (Rectangle)stateObj;
			Rectangle screenRect = new Rectangle(
					context.zoomer.itsXm(rect.x), context.zoomer.itsYm(rect.y), 
					Math.round(context.zoomer.getZoom()*rect.width), Math.round(context.zoomer.getZoom()*rect.height));
			
			
			Stroke s = g2.getStroke();
            Stroke new_stroke = new BasicStroke(
            		1, 
            		BasicStroke.CAP_BUTT, 
            		BasicStroke.JOIN_BEVEL, 
            		0, 
            		new float[]{8,4}, 0);
            g2.setStroke(new_stroke);
			g.drawRect( screenRect.x, screenRect.y, screenRect.width,screenRect.height);

			Composite c = g2.getComposite();
			g2.setComposite( AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));

			x1 = context.zoomer.itsXm(0);
			y1 = context.zoomer.itsYm(0);
			x2 = context.zoomer.itsXm(workspace.getWidth());
			y2 = context.zoomer.itsYm(workspace.getHeight());

			// Draw Grey area outside of crop
			g2.fillRect( x1, y1, screenRect.x - x1 - 1, y2-y1);
			g2.fillRect( screenRect.x-1, y1, screenRect.width+2, screenRect.y - y1 - 1);
			g2.fillRect( screenRect.x-1, screenRect.y + screenRect.height+1, screenRect.width+2, y2 - (screenRect.height+ screenRect.y) + 1);
			g2.fillRect( screenRect.x + screenRect.width+1,  y1, x2 - (screenRect.width+screenRect.x)+1, y2-y1);
			
			// Draw Special junks
			if( stateVar != 0 ) {
				// A bit too much logic in draw events for my taste, but it's
				//	better than adding Zoom observers.

				g2.setStroke(new BasicStroke(2.0f));
				if( (stateVar & 0x8)== 0) {
					// Inner Rect that represents finilizing the crop
					Rectangle inner = MUtil.scaleRect(screenRect, 0.6f);
					
					if( inner.contains(rawX, rawY)) {
						g2.setColor(Color.YELLOW);
						g2.drawRect(inner.x, inner.y, inner.width, inner.height);
						
						stateVar = 0x04;
					}
				}

				Rectangle topLeft = MUtil.scaleRect(screenRect, 0.2f);
				topLeft.x = screenRect.x;
				topLeft.y = screenRect.y; 
				
				Rectangle topRight =new Rectangle(topLeft);
				topRight.x = screenRect.x + screenRect.width - topRight.width;
				topRight.y = screenRect.y;
				
				Rectangle bottomLeft =new Rectangle(topLeft);
				bottomLeft.x = screenRect.x;
				bottomLeft.y = screenRect.y + screenRect.height - bottomLeft.height;
				
				Rectangle bottomRight  =new Rectangle(topLeft);
				bottomRight.x = screenRect.x + screenRect.width - bottomRight.width;
				bottomRight.y = screenRect.y + screenRect.height - bottomRight.height;
				
				if( topLeft.contains(rawX, rawY)) {
					g2.setColor(Color.YELLOW);
					
					if( (stateVar & 0x8) == 0)
						stateVar = 0x04 | 0x100 | 0x400;
				}
				else 
					g2.setColor(Color.WHITE);
				g2.drawRect(topLeft.x, topLeft.y, topLeft.width, topLeft.height);
				
				if( topRight.contains(rawX, rawY)) {
					g2.setColor(Color.YELLOW);
					if( (stateVar & 0x8) == 0)
						stateVar = 0x04 | 0x100 | 0x800;
				}
				else 
					g2.setColor(Color.WHITE);
				g2.drawRect(topRight.x, topRight.y, topRight.width, topRight.height);
				
				if( bottomLeft.contains(rawX, rawY)) {
					g2.setColor(Color.YELLOW);
					if( (stateVar & 0x8) == 0)
						stateVar = 0x04 | 0x200 | 0x400;
				}
				else 
					g2.setColor(Color.WHITE);
				g2.drawRect(bottomLeft.x, bottomLeft.y, bottomLeft.width, bottomLeft.height);
				
				if( bottomRight.contains(rawX, rawY)) {
					g2.setColor(Color.YELLOW);
					if( (stateVar & 0x8) == 0)
						stateVar = 0x04 | 0x200 | 0x800;
				}
				else 
					g2.setColor(Color.WHITE);
				g2.drawRect(bottomRight.x, bottomRight.y, bottomRight.width, bottomRight.height);
			}
			
			g2.setComposite(c);
			g2.setStroke(s);
		default:
			break;
		}
	}

	public boolean drawsOverlay() {
		switch( state) {
		case CROPPING:
			return true;
		default:
			return false;
		}
	}
	
	
	private void setState( STATE newState) {
		setState( newState, 0);
	}
	
	private void setState(STATE newState, int var) {
		boolean paint = false;
		
		if( drawsOverlay()) paint = true;
		
		state = newState;
		stateVar = var;
		stateObj = null;
		
		if( paint || drawsOverlay())
			context.repaint();
		
	}
	

	// :::: KeyEventDispatcher
	@Override
	public boolean dispatchKeyEvent(KeyEvent evt) {
		boolean shift =(( evt.getModifiers() & KeyEvent.SHIFT_MASK) != 0);
		boolean ctrl = (( evt.getModifiers() & KeyEvent.CTRL_MASK) != 0);
			
		holdingShift = shift;
		holdingCtrl = ctrl;
		return false;
	}
	
	// :::: ActionListener
	@Override
	public void actionPerformed(ActionEvent evt) {
		if( strokeEngine != null && state == STATE.DRAWING) {
			if( strokeEngine.stepStroke()) {
				strokeEngine.getImageData().refresh();
			}
		}
	}
	

	/** Cleans up resources that have a global-level context in Swing to avoid
	 * Memory Leaks. */
	public void cleanUp() {
		update_timer.stop();
		KeyboardFocusManager.getCurrentKeyboardFocusManager()
			.removeKeyEventDispatcher(this);
		
	}

}
