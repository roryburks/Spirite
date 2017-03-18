package spirite.panel_work;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

import jpen.PButton;		// BAD
import jpen.PButtonEvent;	// BAD
import spirite.MUtil;
import spirite.brains.MasterControl;
import spirite.brains.PaletteManager;
import spirite.brains.RenderEngine;
import spirite.brains.RenderEngine.RenderSettings;
import spirite.brains.ToolsetManager;
import spirite.brains.ToolsetManager.Tool;
import spirite.brains.ToolsetManager.ToolSettings;
import spirite.image_data.DrawEngine;
import spirite.image_data.DrawEngine.Method;
import spirite.image_data.DrawEngine.StrokeParams;
import spirite.image_data.GroupTree;
import spirite.image_data.GroupTree.LayerNode;
import spirite.image_data.GroupTree.Node;
import spirite.image_data.ImageWorkspace;
import spirite.image_data.ImageWorkspace.BuiltImageData;
import spirite.image_data.SelectionEngine;
import spirite.image_data.SelectionEngine.Selection;
import spirite.image_data.SelectionEngine.SelectionType;
import spirite.image_data.UndoEngine;
import spirite.image_data.UndoEngine.UndoableAction;
import spirite.image_data.layers.SpriteLayer;
import spirite.image_data.layers.SpriteLayer.Part;
import spirite.panel_work.WorkPanel.Zoomer;
import spirite.pen.PenTraits.PenState;

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
	implements KeyEventDispatcher, ActionListener
{
	// Contains "Image to Screen" and "Screen to Image" methods.
	//	Could possibly wrap them in an interface to avoid tempting Penner 
	//	with UI controls
	private final WorkPanel context;
	private final Zoomer zoomer;	
	
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
	private boolean holdingAlt = false;
	
	// Naturally, being a mouse (or drawing tablet)-based input handler with
	//	a variety of states, a large number of coordinate sets need to be 
	//	remembered.  While it's possible that some of these could be condensed
	//	into fewer variables, it wouldn't be worth it just to save a few bytes
	//	of RAM
	//private int startX;	// Set at the start 
//	private int startY;
	private int oldX;	// OldX and OldY are the last-checked X and Y primarily used
	private int oldY;	// 	for things that only happen if they change
	private int rawX;	// raw position are the last-recorded coordinates in pure form
	private int rawY;	// 	(screen coordinates relative to the component Penner watches over)
	private int oldRawX;	// Similar to oldX and oldY but for 
	private int oldRawY;
	
	
	private float pressure = 1.0f;
	
	private int x, y;

	
	private StateBehavior behavior;
	
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
	
	/** Pen/Mouse input should not necessarily change the image every time
	 * a raw input is detected, because the input might stair-step, updating
	 * X and Y at different times, instead step should be called at regular
	 * short intervals (no slower than 50 times per second, preferably) to
	 * update all move behavior.*/
	public void step() {
		// Perform state-based "on-pen/mouse move" code
		if( behavior != null)

		if( (oldX != x || oldY != y) && behavior != null) {
			behavior.onMove();
			if( behavior instanceof DrawnStateBehavior)
				context.repaint();
			
			context.refreshCoordinates(x, y);
		}
		
		if( behavior != null)
			behavior.onTock();
		
		oldX = x;
		oldY = y;
		oldRawX = rawX;
		oldRawY = rawY;
		

	}
	
	/**
	 */
	public void penDownEvent(PButtonEvent pbe) {
		// TODO: This Should not be using JPen objects
		if( pbe.button.typeNumber > 3) return;	// Shit/Ctrl/Etc events

		PButton.Type button = pbe.button.getType();
		if( button != PButton.Type.LEFT && button != PButton.Type.RIGHT && button != PButton.Type.CENTER)
			return;
		
		if( behavior != null)
			behavior.onPenDown();
		else if( workspace.getReferenceManager().isEditingReference()) {
			// Special Reference behavior
			if( holdingCtrl) {
				behavior = new ZoomingReference();
				behavior.start();
			}
			else {
				behavior = new GlobalRefMove();
				behavior.start();
			}
		}
		else {
			// Tool-based State-starting
			Tool tool = toolsetManager.getSelectedTool();
			
			switch( tool) {
			case PEN:
				if( holdingCtrl) 
					behavior = new PickBehavior(button == PButton.Type.LEFT);
				else 
					behavior = new PenBehavior((button == PButton.Type.LEFT) ? 
									paletteManager.getActiveColor(0)
									: paletteManager.getActiveColor(1));
				break;
			case ERASER:
				behavior = new EraseBehavior();
				break;
			case FILL:
				fill( button == PButton.Type.LEFT);
				break;
			case BOX_SELECTION: {
				Selection selection = selectionEngine.getSelection();
				
				if( selection != null && 
						selection.contains(x-selectionEngine.getOffsetX(),y-selectionEngine.getOffsetY())) 
					behavior = new MovingSelectionBehavior();
				else 
					behavior = new FormingSelectionBehavior();
				break;}
			case MOVE:{
				Selection selection = selectionEngine.getSelection();
				
				if(selection != null)
					behavior = new MovingSelectionBehavior();
				else if(workspace.getSelectedNode() != null) 
					behavior = new MovingNodeBehavior(workspace.getSelectedNode());

				break;}
			case COLOR_PICKER:
				behavior = new PickBehavior(button == PButton.Type.LEFT);
				break;
			case PIXEL:
				if( holdingCtrl)  {
					behavior = new PickBehavior(button == PButton.Type.LEFT);
				}
				else {
					behavior = new PixelBehavior((button == PButton.Type.LEFT) ? 
							paletteManager.getActiveColor(0)
							: paletteManager.getActiveColor(1));
				}
				break;
			case CROP:
				behavior = new CroppingBehavior();
				break;
			case COMPOSER:
				Node node = workspace.getSelectedNode();
				if( !(node instanceof LayerNode)
					|| (!(((LayerNode)node).getLayer() instanceof SpriteLayer))) 
					break;
				
				SpriteLayer rig = (SpriteLayer)(((LayerNode)workspace.getSelectedNode()).getLayer());
				SpriteLayer.Part part = rig.grabPart(x-node.getOffsetX(), y-node.getOffsetY(), true);
				
				if( part == null) part = rig.getActivePart();
				
				if( holdingShift)
					behavior = new MovingRigPart(rig, part);
				
				break;
			case FLIPPER:
				behavior = new FlippingBehavior();
				break;
			case RESHAPER:
				if( button == PButton.Type.LEFT) {
					UndoableAction ra = workspace.getUndoEngine().createReplaceAction(
							workspace.buildActiveData().handle, 
							drawEngine.scale(workspace.buildActiveData().handle.deepAccess()));
					workspace.getUndoEngine().performAndStore(ra);
				}
				else {
				}
				break;
			case COLOR_CHANGE:

				drawEngine.changeColor(
						workspace.buildActiveData(),
						paletteManager.getActiveColor(0),
						paletteManager.getActiveColor(1));
				break;
			}
			
			if( behavior != null)
				behavior.start();
		}
	}
	
	public void penUpEvent( PButtonEvent pbe)
	{
		// PenUp
		if( behavior != null) {
			behavior.onPenUp();
		}
	}

	// :::: Single-click actions that don't require StateBehavior
	private void fill( boolean leftClick) {
		// Determine Color
		Color c = (leftClick) ? 
				paletteManager.getActiveColor(0)
				: paletteManager.getActiveColor(1);
				
		if( holdingCtrl) c = new Color(0,0,0,0);

		// Grab the Active Data
		BuiltImageData data = workspace.buildActiveData();
		GroupTree.Node node = workspace.getSelectedNode();
		
		if( data != null && node != null) {
			// Perform the fill Action, only store the UndoAction if 
			//	an actual change is made.
			drawEngine.fill( x, y, c, data);
		} 
	}

	
	// :::: Methods to feed raw data into the Penner for it to interpret.
	// !!!! Note: these methods should behave as if there's no potential behavior
	//	problem if they aren't running on the AWTEvent thread.
	public void rawUpdateX( int raw) {
		rawX = raw;
		x = zoomer.stiXm(rawX);
	}
	public void rawUpdateY( int raw) {
		rawY = raw;
		y = zoomer.stiYm( rawY);
	}
	public void rawUpdatePressure( float pressure) {
		this.pressure = pressure;
	}
	
	// By design, StateBehavior has and should make use of all local variables
	//	relevant to it, variables passed to it (if any) are for convenience only
	//	as the StateBehavior could have either accessed them or caculated them
	//	itself.
	abstract class StateBehavior {
		public abstract void start();
		public abstract void onTock();
		public abstract void onMove();
		
		// For most StateBehavior, onPenDown will be irrelevant/not make sense
		//	because their penUp action is to cancel the state.
		public void onPenDown() {}
		public void onPenUp() {
			end();
		}
		
		public void end() {
			// This effectively ends the state behavior
			behavior = null;
		}
	}
	
	abstract class DrawnStateBehavior extends StateBehavior {
		public abstract void paintOverlay( Graphics g);
	}
	
	abstract class StrokeBehavior extends StateBehavior {
		int shiftX = rawX;
		int shiftY = rawY;
		int dx = x;
		int dy = y;
		private int shiftMode = -1;	// 0 : accept any, 1 : horizontal, 2: vertical
		
		public void startStroke (StrokeParams stroke) {
			if( workspace != null && workspace.buildActiveData() != null) {
				shiftX = rawX;
				shiftY = rawY;
				BuiltImageData data = workspace.buildActiveData();
				GroupTree.Node node = workspace.getSelectedNode();
				
				if( !drawEngine.startStroke(stroke, new PenState(x,y,pressure), data))
					end();
			}
		}
		
		@Override
		public void onTock() {
			if( holdingShift) {
				if( shiftMode == -1) {
					shiftMode = 0;
					shiftX = rawX;
					shiftY = rawY;
				}
				if( shiftMode == 0) {
					if( Math.abs(shiftX - rawX) > 10)
						shiftMode = 1;
					else if( Math.abs(shiftY - rawY) > 10)
						shiftMode = 2;
				}
				
				if( shiftMode == 1)
					dx = x;
				if( shiftMode == 2)
					dy = y;
			}
			else {
				shiftMode = -1;
				dx = x;
				dy = y;
			}
			drawEngine.stepStroke( new PenState( dx, dy, pressure));

		}

		@Override
		public void onPenUp() {
			drawEngine.endStroke();
			super.onPenUp();
		}
		
		@Override public void onMove() {}
	}
	
	class EraseBehavior extends StrokeBehavior {
		@Override
		public void start() {
			ToolSettings settings = toolsetManager.getToolSettings(Tool.ERASER);
			StrokeParams stroke = new StrokeParams();
			stroke.setMethod( Method.ERASE);
			stroke.setWidth((float)settings.getValue("width"));

			// Start the Stroke
			startStroke( stroke);
		}
	}
	

	class PenBehavior extends StrokeBehavior {
		final Color color;
		PenBehavior( Color color) {
			this.color = color;
		}
		@Override
		public void start() {
			ToolSettings settings = toolsetManager.getToolSettings(Tool.PEN);
			StrokeParams stroke = new StrokeParams();
			stroke.setColor( color);
			stroke.setWidth((float)settings.getValue("width"));
			stroke.setAlpha((float)settings.getValue("alpha"));
			
			// Start the Stroke
			startStroke( stroke);
		}
	}
	class PixelBehavior extends StrokeBehavior {
		final Color color;
		PixelBehavior( Color color) {
			this.color = color;
		}
		@Override
		public void start() {
			ToolSettings settings = toolsetManager.getToolSettings(Tool.PIXEL);
			StrokeParams stroke = new StrokeParams();
			stroke.setMethod( Method.PIXEL);
			stroke.setAlpha((float)settings.getValue("alpha"));
			stroke.setColor( color);
			startStroke( stroke);
		}
	}
	
	class PickBehavior extends StateBehavior {
		final boolean leftClick;
		
		PickBehavior( boolean leftClick) {
			this.leftClick = leftClick;
		}
		
		private void pickColor() {
			// Get the composed image
			RenderSettings settings = new RenderSettings(
					renderEngine.getDefaultRenderTarget(workspace));
			BufferedImage img = renderEngine.renderImage(settings);
			
			if( !MUtil.coordInImage(x, y, img))
				return;
			paletteManager.setActiveColor(
					(leftClick)?0:1, new Color(img.getRGB(x, y)));
		}

		@Override
		public void start() {
			pickColor();
		}
		@Override
		public void onMove() {
			pickColor();
		}
		@Override public void onTock() {}
	}
	
	class MovingNodeBehavior extends StateBehavior {
		final Node node;
		
		MovingNodeBehavior( Node node) {
			this.node = node;
		}
		@Override public void start() {}
		@Override public void onTock() {}
		@Override
		public void onMove() {
			if( node != null && (oldX != x || oldY != y))
				node.setOffset( node.getOffsetX() + (x - oldX), 
								 node.getOffsetY() + (y - oldY));
		}
	}
	class MovingSelectionBehavior extends StateBehavior {
		@Override public void start() {}
		@Override public void onTock() {}
		@Override
		public void onMove() {
			if( oldX != x || oldY != y) 
				selectionEngine.setOffset(
						selectionEngine.getOffsetX() + (x - oldX),
						selectionEngine.getOffsetY() + (y - oldY));
		}
	}
	class MovingRigPart extends StateBehavior {
		private final SpriteLayer rig;
		private final Part part;
		MovingRigPart( SpriteLayer rig, Part part) {
			this.rig = rig;
			this.part = part;
		}

		@Override public void start() {}
		@Override public void onTock() {}
		@Override
		public void onMove() {
			undoEngine.performAndStore(
					rig.createModifyPartAction( part, 
							part.getOffsetX() + (x - oldX),
							part.getOffsetY() + (y - oldY), 
							part.getDepth(), 
							part.getTypeName(), 
							part.isVisible(), 
							part.getAlpha())
				);
		}
	}
	class ZoomingReference extends StateBehavior {
		@Override public void start() {}
		@Override public void onTock() {}
		@Override
		public void onMove() {
			context.refzoomer.setFineZoom((float) (context.refzoomer.getRawZoom() * Math.pow(1.0015, 1+(rawY - oldRawY))));
		}
	}
	class GlobalRefMove extends StateBehavior {
		@Override public void start() {}
		@Override public void onTock() {}
		@Override
		public void onMove() {
			context.refzoomer.setCX( Math.round(context.refzoomer.getCX()+ (x - oldX)*context.refzoomer.getZoom()/context.refzoomer.getRawZoom()));
			context.refzoomer.setCY( Math.round(context.refzoomer.getCY()+ (y - oldY)*context.refzoomer.getZoom()/context.refzoomer.getRawZoom()));
		}
		
	}
	
	class FormingSelectionBehavior extends StateBehavior {
		@Override
		public void start() {
			selectionEngine.startBuildingSelection(SelectionType.RECTANGLE, x, y);
		}
		@Override
		public void onMove() {
			selectionEngine.updateBuildingSelection(x, y);
		}
		@Override
		public void onPenUp() {
			selectionEngine.finishBuildingSelection();
			super.onPenUp();
		}
		@Override
		public void onTock() {
			selectionEngine.updateBuildingSelection(x, y);
		}
	}
	class CroppingBehavior extends DrawnStateBehavior {
		boolean building = false;
		boolean modifying = false;
		Rectangle cropSection = null;
		Rectangle middle;
		Rectangle topRight;
		Rectangle topLeft;
		Rectangle bottomRight;
		Rectangle bottomLeft;
		int startx, starty;
		//	0x1 : Top
		//	0x2 : Bottom
		//	0x4 : Left
		//	0x8 : Right
		byte cardinalMap = 0x00;

		static final byte TOPMASK = 0x01;
		static final byte BOTTOMMASK = 0x02;
		static final byte LEFTMASK = 0x04;
		static final byte RIGHTMASK = 0x08;
//		Rectangle 
		
		private void buildCrop( ) {
			middle = MUtil.scaleRect( cropSection, 0.6f);
			topLeft = MUtil.scaleRect( cropSection, 0.2f);
			topLeft.x = cropSection.x;
			topLeft.y = cropSection.y;
			topRight = new Rectangle(topLeft);
			topRight.x = cropSection.x + cropSection.width - topRight.width;
			topRight.y = cropSection.y;
			bottomLeft = new Rectangle(topLeft);
			bottomLeft.x = cropSection.x;
			bottomLeft.y = cropSection.y + cropSection.height - bottomLeft.height;
			bottomRight = new Rectangle(topLeft);
			bottomRight.x = cropSection.x + cropSection.width - bottomRight.width;
			bottomRight.y = cropSection.y + cropSection.height - bottomRight.height;
		}

		@Override
		public void start() {
			building = true;
			startx = x;
			starty = y;
			cropSection = new Rectangle( x, y, 0, 0);
		}

		@Override
		public void onPenUp() {
	
			ToolSettings settings = toolsetManager.getToolSettings(Tool.CROP);
	
			cardinalMap = 0;


			if( building) {
				cropSection = MUtil.rectFromEndpoints(
						startx, starty, x, y);
				if( (Boolean)settings.getValue("quickCrop")) {
					workspace.cropNode(
						workspace.getSelectedNode(), 
						cropSection,
						(Boolean)settings.getValue("shrinkOnly"));
					end();
				}
				else
					buildCrop();
				
				building = false;
			}
		}
		
		@Override public void onTock() {}

		@Override
		public void onMove() {

			if( building) {
				cropSection = MUtil.rectFromEndpoints(
						startx, starty, x, y);
			}
			else if( modifying) {
				if( (cardinalMap & TOPMASK) != 0 ) {
					cropSection.y += y - oldY;
					cropSection.height -= (y - oldY);
				}
				if( (cardinalMap & BOTTOMMASK) != 0) {
					cropSection.height += (y - oldY);
				}
				if( (cardinalMap & LEFTMASK) != 0) {
					cropSection.x += (x - oldX);
					cropSection.width -= (x - oldX);
				}
				if( (cardinalMap & RIGHTMASK)!= 0) {
					cropSection.width+= (x - oldX);
				}
				buildCrop();
			}
		}
		
		@Override
		public void onPenDown() {
			if( toolsetManager.getSelectedTool() != Tool.CROP) { end(); return;}
			
			if( cropSection == null || !cropSection.contains(x, y)) {
				building = true;
				startx = x;
				starty = y;
			}
			else {
				cardinalMap = 0;
				
				if( middle.contains( x, y)) {
					workspace.cropNode(
							workspace.getSelectedNode(), 
							cropSection,
							(Boolean)toolsetManager.getToolSettings(Tool.CROP).getValue("shrinkOnly"));
					
					end();
				}
				else if( topRight.contains(x, y)) 
					cardinalMap = TOPMASK | RIGHTMASK;
				else if( topLeft.contains(x,y))
					cardinalMap = TOPMASK | LEFTMASK;
				else if( bottomLeft.contains(x,y))
					cardinalMap = BOTTOMMASK| LEFTMASK;
				else if( bottomRight.contains(x,y))
					cardinalMap = BOTTOMMASK | RIGHTMASK;
				
				if( cardinalMap != 0)
					modifying = true;
			}
		}

		@Override
		public void paintOverlay(Graphics g) {
			Graphics2D g2 = (Graphics2D)g;
			
			Stroke s = g2.getStroke();
			
			// Outline
            Stroke new_stroke = new BasicStroke(
            		1, 
            		BasicStroke.CAP_BUTT, 
            		BasicStroke.JOIN_BEVEL, 
            		0, 
            		new float[]{8,4}, 0);
            g2.setStroke(new_stroke);
            
            
            Rectangle r = context.zoomer.itsRm(cropSection);
            g.drawRect(r.x, r.y, r.width, r.height);
			

            // Grey area outside
			Composite c = g2.getComposite();
			int x1 = context.zoomer.itsXm(0);
			int y1 = context.zoomer.itsYm(0);
			int x2 = context.zoomer.itsXm(workspace.getWidth());
			int y2 = context.zoomer.itsYm(workspace.getHeight());

			if( r.x < x1) { r.width -= x1 - r.x; r.x = x1;}
			if( r.x + r.width > x2) { r.width = x2 - r.x;}
			
			g2.setComposite( AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
			g2.fillRect( x1, y1, r.x - x1 - 1, y2-y1);
			g2.fillRect( r.x-1, y1, r.width+2, r.y - y1 - 1);
			g2.fillRect( r.x-1, r.y + r.height+1, r.width+2, y2 - (r.height+ r.y) + 1);
			g2.fillRect( r.x + r.width+1,  y1, x2 - (r.width+r.x)+1, y2-y1);
			
			// The various inner rectangles represenging the modification points
			if( !building) {
				g2.setStroke(new BasicStroke(2.0f));
				
				if( middle.contains(x,y)) {
					r = context.zoomer.itsRm( middle);
					g2.setColor(Color.YELLOW);
		            g.drawRect(r.x, r.y, r.width, r.height);
				}

				if( topRight.contains(x,y))
					g2.setColor(Color.YELLOW);
				else
					g2.setColor(Color.WHITE);
				r = context.zoomer.itsRm(topRight);
	            g.drawRect(r.x, r.y, r.width, r.height);

				if( topLeft.contains(x,y))
					g2.setColor(Color.YELLOW);
				else
					g2.setColor(Color.WHITE);
				r = context.zoomer.itsRm(topLeft);
	            g.drawRect(r.x, r.y, r.width, r.height);

				if( bottomLeft.contains(x,y))
					g2.setColor(Color.YELLOW);
				else
					g2.setColor(Color.WHITE);
				r = context.zoomer.itsRm(bottomLeft);
	            g.drawRect(r.x, r.y, r.width, r.height);

				if( bottomRight.contains(x,y))
					g2.setColor(Color.YELLOW);
				else
					g2.setColor(Color.WHITE);
				r = context.zoomer.itsRm(bottomRight);
	            g.drawRect(r.x, r.y, r.width, r.height);
			}

    		g2.setComposite(c);
    		g2.setStroke(s);
		}

	}
	

	class FlippingBehavior extends StateBehavior {
		int startX, startY;
		
		@Override
		public void start() {
			startX = x;
			startY = y;
		}
		@Override
		public void onMove() {
			
		}
		@Override
		public void onPenUp() {
			BuiltImageData data =  workspace.buildActiveData();
			if( data != null) {
				if( MUtil.distance(x , y, startX, startY) < 5 ||
					Math.abs(x - startX) > Math.abs(y - startY))
					drawEngine.flip( data, true);
				else
					drawEngine.flip( data, false);
			}
			
			super.onPenUp();
		}
		@Override public void onTock() {}
	}
	
	// :::: KeyEventDispatcher
	@Override
	public boolean dispatchKeyEvent(KeyEvent evt) {
		boolean shift =(( evt.getModifiers() & KeyEvent.SHIFT_MASK) != 0);
		boolean ctrl = (( evt.getModifiers() & KeyEvent.CTRL_MASK) != 0);
		boolean alt = (( evt.getModifiers() & KeyEvent.ALT_MASK) != 0);
			
		holdingShift = shift;
		holdingCtrl = ctrl;
		holdingAlt = alt;
		return false;
	}
	
	// :::: ActionListener
	@Override
	public void actionPerformed(ActionEvent evt) {
	}
	

	/** Cleans up resources that have a global-level context in Swing to avoid
	 * Memory Leaks. */
	public void cleanUp() {
		KeyboardFocusManager.getCurrentKeyboardFocusManager()
			.removeKeyEventDispatcher(this);
		
	}

	public boolean drawsOverlay() {
		return behavior instanceof DrawnStateBehavior;
	}

	public void paintOverlay(Graphics g) {
		if( behavior instanceof DrawnStateBehavior) {
			((DrawnStateBehavior)behavior).paintOverlay(g);
		}
	}

}
