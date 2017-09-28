package spirite.pc.ui.panel_layers;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import javax.management.RuntimeErrorException;
import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Group;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import spirite.base.brains.MasterControl;
import spirite.base.brains.MasterControl.MWorkspaceObserver;
import spirite.base.graphics.RenderProperties;
import spirite.base.image_data.Animation;
import spirite.base.image_data.AnimationManager.AnimationState;
import spirite.base.image_data.AnimationManager.MAnimationStateEvent;
import spirite.base.image_data.AnimationManager.MAnimationStateObserver;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.ImageWorkspace.MSelectionObserver;
import spirite.base.image_data.UndoEngine.CompositeAction;
import spirite.base.image_data.UndoEngine;
import spirite.base.image_data.animation_data.FixedFrameAnimation;
import spirite.base.image_data.animation_data.FixedFrameAnimation.AnimationLayer;
import spirite.base.image_data.animation_data.FixedFrameAnimation.AnimationLayer.Frame;
import spirite.hybrid.Globals;
import spirite.base.image_data.animation_data.FixedFrameAnimation.Marker;
import spirite.base.util.Colors;
import spirite.pc.graphics.ImageBI;
import spirite.pc.ui.components.OmniEye;
import spirite.pc.ui.dialogs.RenderPropertiesDialog;
import spirite.pc.ui.generic.SquarePanel;


/***
 * AnimationSchemePanel is a grid 
 */
public class AnimationSchemePanel extends JPanel 
	implements MWorkspaceObserver, MSelectionObserver, MAnimationStateObserver 
{
	private final MasterControl master;
	private ImageWorkspace ws = null;
	
	private FixedFrameAnimation animation;
	private final JPanel topLeft = new JPanel();
	private final JPanel bottomRight = new JPanel();
	private final MainTitleBar titleBar = new MainTitleBar();
	private final JPanel content = new JPanel() {
		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			
			int s = animation.getStart();
			int e = animation.getEnd();
			AnimationState as = ws.getAnimationManager().getAnimationState(animation);
			int selT = (int)Math.floor(as.getSelectedMetronome());
			
			for( int t=s; t<e; ++t) {
				Color c = Color.WHITE;
				if(  selT == t)
					c = Color.YELLOW;
				else {
					if( as.hasSubstateForRelativeTick(as.cannonizeRelTick(t)))
						c = Color.green;
				}
				
				if( (int)Math.floor(as.getMetronom()) == t)
					c = Colors.darken(c);
				
				//c = new Color( (int)(0xFFFFFF*Math.random()));
				
				g.setColor(c);
				g.fillRect( 0, LAYER_TITLE_BAR_HEIGHT + ROW_HEIGHT * (t - animation.getStart()), getWidth(), ROW_HEIGHT);
			}
		}
		
		@Override
		public void paint(Graphics g) {
			super.paint(g);
			if( state != null)
				state.Draw(g);
		}
	};

	private final int MAIN_TITLE_BAR_HEIGHT = 24;
	private final int LAYER_TITLE_BAR_HEIGHT = 16;
	private final int BOTTOM_BAR_HEIGHT = 16;
	private final int TL_WIDTH = 16;
	private final int ROW_HEIGHT = 32;

	private static final Color ARROW_COLOR = Color.RED;
	private static final Color DRAG_BORDER_COLOR = Color.green;
	private static final Color TITLE_BG = Color.WHITE;
	private final Color tickColor = Globals.getColor("animSchemePanel.tickBG");
	
	private Component[] titles = null;
	
	
	public AnimationSchemePanel( MasterControl master, FixedFrameAnimation fixedFrameAnimation) {
		if( fixedFrameAnimation == null)
			throw new RuntimeException("Null Animation for AnimationSchemePanel");
		this.master = master;
		this.animation = fixedFrameAnimation;
		BuildFromAnimation();
		
		master.addWorkspaceObserver(this);
		ws = master.getCurrentWorkspace();
		if( ws != null) {
			ws.addSelectionObserver(this);
			ws.getAnimationManager().addAnimationStateObserver(this);
		}
	}
	
	public void Rebuild() {
		BuildFromAnimation();	
	}
	
	private void BuildFromAnimation() {
		int start = animation.getStart();
		int end = animation.getEnd();
		
		this.removeAll();
		
		titleBar.setTitle(animation.getName());
		
		GroupLayout primaryLayout = new GroupLayout(this);
		primaryLayout.setHorizontalGroup( primaryLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
				.addComponent(titleBar)
				.addComponent(content));
		primaryLayout.setVerticalGroup( primaryLayout.createSequentialGroup()
				.addComponent(titleBar, MAIN_TITLE_BAR_HEIGHT, MAIN_TITLE_BAR_HEIGHT, MAIN_TITLE_BAR_HEIGHT)
				.addComponent(content));
		this.setLayout(primaryLayout);
		
		List<AnimationLayer> layers = animation.getLayers();
		
		GroupLayout layout = new GroupLayout(content);
		
		Group horGroup = layout.createSequentialGroup();
		Group vertGroup = layout.createSequentialGroup();
		
		Group[] horGroups = new Group[layers.size() + 1];
		for( int i=0; i < layers.size() + 1; ++i) {
			horGroups[i] = layout.createParallelGroup();
			horGroup.addGroup(horGroups[i]);
		}

		// Title Bar
		Group titleVertGroup = layout.createParallelGroup();
		vertGroup.addGroup(titleVertGroup);
		horGroups[0].addComponent(topLeft, TL_WIDTH,TL_WIDTH,TL_WIDTH);
		titleVertGroup.addComponent(topLeft, LAYER_TITLE_BAR_HEIGHT,LAYER_TITLE_BAR_HEIGHT,LAYER_TITLE_BAR_HEIGHT);
		
		titles= new Component[layers.size()];
		
		for( int i=0; i < layers.size(); ++i) {
			AnimationLayer layer = layers.get(i);
			titles[i] = new LayerTitleBar(layer.getName());
			horGroups[i+1].addComponent(titles[i]);
			titleVertGroup.addComponent( titles[i], LAYER_TITLE_BAR_HEIGHT,LAYER_TITLE_BAR_HEIGHT,LAYER_TITLE_BAR_HEIGHT);
		}
		layout.linkSize(SwingConstants.HORIZONTAL, titles);
		
		// Animation Bars
		Component[] ticks = new Component[Math.max(0,end-start)];
		Group vertMid = layout.createParallelGroup();
		vertGroup.addGroup(vertMid);
		{
			Group vertInner = layout.createSequentialGroup();
			vertMid.addGroup(vertInner);
			for( int i = start; i < end; ++i) {
				
				ticks[i-start] = new TickPanel(i);
				horGroups[0].addComponent(ticks[i-start], TL_WIDTH,TL_WIDTH,TL_WIDTH);
				vertInner.addComponent(ticks[i-start],ROW_HEIGHT,ROW_HEIGHT,ROW_HEIGHT);
			}
		}
		
		layout.linkSize(SwingConstants.VERTICAL, ticks);
		
		// Fill out the animation
		for( int col=0; col < layers.size(); ++col) {
			Group vertInner = layout.createSequentialGroup();
			vertMid.addGroup(vertInner);
			AnimationLayer layer = layers.get(col);
			
			int currentTick = start;
			
			List<Component> linked = new ArrayList<>();
			
			for(Frame frame : layer.getFrames()) {
				if( frame.getMarker() == Marker.FRAME)
				{
					int fs = Math.max( currentTick,frame.getStart());
					int fe = Math.max(fs, Math.min(end, frame.getEnd()));
					
					if( fs < fe) {
						if( fs > currentTick)
							vertInner.addGap(ROW_HEIGHT * (fs - currentTick));
						
						Component component = new FramePanel( frame, col);
						linked.add(component);
						int h = ROW_HEIGHT * (fe-fs);
						vertInner.addComponent(component,h,h,h);
						horGroups[1+col].addComponent(component);
												
						currentTick = fe;
					}
				}
			}

			linked.add(titles[col]);
			layout.linkSize(SwingConstants.HORIZONTAL, linked.toArray( new Component[linked.size()]));
		}
		
		// Lower Bar
		Group bottomVertGroup = layout.createParallelGroup();
		vertGroup.addGroup(bottomVertGroup);
		
		bottomVertGroup.addComponent(bottomRight, BOTTOM_BAR_HEIGHT, BOTTOM_BAR_HEIGHT, BOTTOM_BAR_HEIGHT);
		horGroups[0].addComponent(bottomRight, TL_WIDTH,TL_WIDTH,TL_WIDTH);

		for( int j=0; j < layers.size(); ++j) {
			AnimationLayer layer = layers.get(j);
			
			Component c = new BottomPanel(j, layer);
			
			horGroups[j+1].addComponent(c);
			bottomVertGroup.addComponent(c);
			layout.linkSize(SwingConstants.HORIZONTAL, c, titles[j]);
		}
		
		layout.setHorizontalGroup(horGroup);
		layout.setVerticalGroup(vertGroup);

		content.setLayout(layout);
	}
	
//	@Override
//	protected void paintComponent(Graphics g) {
//		super.paintComponent(g);
//
//	}
	
	private Rectangle GetFrameBounds( int layer, int tick) {
		
		int sx = TL_WIDTH;
		int sy = LAYER_TITLE_BAR_HEIGHT + ROW_HEIGHT * (tick - animation.getStart());
		
		
		for( int i=0; i < layer; ++i)
			sx += titles[i].getWidth();
		
		return new Rectangle( sx, sy, titles[layer].getWidth(), ROW_HEIGHT);
	}
	private int TickAtY( int y) {
		return (y - LAYER_TITLE_BAR_HEIGHT)/ROW_HEIGHT;
	}
	
	// :::: WorkspaceObserver
	@Override	public void newWorkspace(ImageWorkspace newWorkspace) {	}
	@Override	public void removeWorkspace(ImageWorkspace newWorkspace) {	}
	@Override
	public void currentWorkspaceChanged(ImageWorkspace selected, ImageWorkspace previous) {
		if( ws != null) {
			ws.removeSelectionObserver(this);
			ws.getAnimationManager().removeAnimationStateObserver(this);
		}
		ws = selected;
		if( ws != null) {
			ws.addSelectionObserver(this);
			ws.getAnimationManager().addAnimationStateObserver(this);
		}
		
		Rebuild();
	}
	
	// :::: SelectionOberver
	@Override
	public void selectionChanged(Node newSelection) {
		this.repaint();
	}
	
	private class MainTitleBar extends JPanel {
		String title;
		private final JLabel label = new JLabel();
		
		private MainTitleBar() {
			//this.setLayout(new GridLayout());
			
			this.setBackground(TITLE_BG );
			
			label.setFont( new Font("Tahoma",Font.BOLD, 12));

			this.add(label);
			label.setText(title);
		}
		
		public void setTitle(String title) {
			this.title = title;
			label.setText(title);
		}
	}
	
	private class LayerTitleBar extends JPanel {
		String title;
		private final JLabel label = new JLabel();
		
		private LayerTitleBar( String title) {
			//this.setLayout(new GridLayout());
			
			this.setBackground(TITLE_BG );
			this.title = title;
			
			label.setFont( new Font("Tahoma",Font.BOLD, 10));

			this.add(label);
			label.setText(title);
		}
	}
	
	private class TickPanel extends JPanel {
		private int tick;
		private final JLabel label = new JLabel();
		
		private TickPanel( int tick) {
			this.setOpaque(false);
			this.tick = tick;
			label.setText(""+tick);
			
			this.setBackground( tickColor);
			this.addMouseListener( new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e) {
					if( e.getButton() == MouseEvent.BUTTON1)
						ws.getAnimationManager().getAnimationState(animation).setSelectedMetronome(tick);
					else if( e.getButton() == MouseEvent.BUTTON3) {
						RenderPropertiesDialog dialog = new RenderPropertiesDialog(new RenderProperties());
						JOptionPane.showConfirmDialog(TickPanel.this, dialog, null, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
						
						AnimationState as = ws.getAnimationManager().getAnimationState(animation);
						as.putSubstateForRelativeTick( as.cannonizeRelTick(tick), dialog.getResult());
					}
				}
			});
			
			label.setFont( new Font("Tahoma",Font.BOLD, 10));

			this.add(label);
			
		}
	}
	
	private class BottomPanel extends JPanel {
		private final JToggleButton btnLock = new JToggleButton("X");
		private final JToggleButton btnSettings = new JToggleButton("S");
		private final JToggleButton omniEye = new OmniEye();
		private final int column;
		private final AnimationLayer layer;
		
		private boolean visible = true;
		
		private BottomPanel( int col, AnimationLayer layer) {
			this.column = col;
			this.layer = layer;
			
			btnLock.setBorder(null);
			btnSettings.setBorder(null);
			

			GroupLayout layout = new GroupLayout(this);
			layout.setHorizontalGroup( layout.createSequentialGroup()
					.addGroup( layout.createParallelGroup()
							.addComponent(btnLock)
							.addComponent(btnSettings))
					.addComponent(omniEye));
			this.setBorder( BorderFactory.createLineBorder(Color.BLACK));
			layout.setVerticalGroup( layout.createParallelGroup()
					.addGroup( layout.createSequentialGroup()
							.addComponent(btnLock)
							.addComponent(btnSettings))
					.addComponent(omniEye));
			
			omniEye.addActionListener( new ActionListener() {
				@Override public void actionPerformed(ActionEvent e) {
					UndoEngine ue = ws.getUndoEngine();
					ue.pause();

					visible = !visible;
					for( Frame frame : layer.getFrames()) {
						if(frame.getLayerNode() != null)
							frame.getLayerNode().getRender().setVisible(visible);
					}
					CompositeAction action = ue.unpause("Toggle Layer Visibility");
					ue.performAndStore(action);
				}
			});
			
			layout.linkSize( SwingConstants.VERTICAL, btnLock,btnSettings);
			
			this.setLayout(layout);
		}
	}
	
	private class FramePanel extends JPanel {
		private final Frame frame;
		private final int column;
		
		private final JPanel btnVLock = new JPanel();
		private final JPanel btnLLock = new JPanel();
		
		private final JPanel drawPanel = new JPanel() {
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				
				ImageBI img = (ImageBI)master.getRenderEngine().accessThumbnail(frame.getLayerNode(), ImageBI.class);
				
				if( img != null)
					g.drawImage( img.img, 0, 0, this.getWidth(), this.getHeight(), 0, 0, img.getWidth(), img.getHeight(), null);
			}
		};
		
		private FramePanel( Frame frame, int column) {
			this.frame = frame;
			this.column = column;
			
			btnVLock.setBorder(null);
			btnLLock.setBorder(null);
			btnVLock.setBackground(Color.RED);
			btnLLock.setBackground(Color.ORANGE);

			this.setOpaque(false);
			
			this.addMouseListener( adapter);
			this.addMouseMotionListener(adapter);
			//drawPanel.addMouseListener( adapter);
			
			GroupLayout layout = new GroupLayout(this);
			
			int LBTN_WIDTH = 8;
			Group hor = layout.createParallelGroup();
			hor.addGroup(layout.createSequentialGroup()
					.addGap(2)
					.addGroup( layout.createParallelGroup()
							.addComponent(btnVLock, LBTN_WIDTH, LBTN_WIDTH, LBTN_WIDTH)
							.addComponent(btnLLock, LBTN_WIDTH, LBTN_WIDTH, LBTN_WIDTH))
					.addGap(2,2, Short.MAX_VALUE)
					.addComponent(drawPanel)
					.addGap(2,2, Short.MAX_VALUE));
			
			Group vert = layout.createSequentialGroup();
			vert.addGroup(layout.createParallelGroup()
					.addGroup( layout.createSequentialGroup()
							.addGap(2)
							.addComponent(btnVLock)
							.addGap(2)
							.addComponent(btnLLock)
							.addGap(2))
					.addGroup( layout.createSequentialGroup()
							.addGap(2)
							.addComponent(drawPanel, ROW_HEIGHT - 4,ROW_HEIGHT - 4,ROW_HEIGHT - 4)
							.addGap(2)));
			
			layout.setHorizontalGroup( hor);
			layout.setVerticalGroup( vert);
			
			if( frame.getLength() > 1) {
				Component c = new FrameExtendPanel();
				vert.addComponent(c);
				hor.addComponent(c);
			}

			drawPanel.setBorder( BorderFactory.createLineBorder(Color.BLACK,2));
			
			layout.linkSize( SwingConstants.VERTICAL, btnVLock,btnLLock);
			
			this.setLayout(layout);
		}
		
		@Override
		protected void paintComponent(Graphics g) {
			if( ws == null)
				setBackground( Color.WHITE);
			else if( ws.getAnimationManager().getSelectedFrame() == frame)
				setBackground( Color.YELLOW);
			else if( ws.getSelectedNode() == frame.getLayerNode())
				setBackground( new Color(122,170,170));
			else
				setBackground( Color.WHITE);
			
			super.paintComponent(g);
		}

		private class FrameExtendPanel extends JPanel {
			public FrameExtendPanel() {
				this.setOpaque(false);
			}
			
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				g.setColor(ARROW_COLOR);
				
				int b = 2;
				int tw = 2;
				int hw = 8;
				int hh = 8;
				int c = this.getWidth() / 2;
				int H = this.getHeight();
				int W = this.getWidth();

				g.fillRect( c - tw/2, b, tw, H - 2*b - hh);
				g.fillPolygon( new int[] {(W-hw)/2,(W+hw)/2,c}, new int[] {H-b-hh,H-b-hh,H-b}, 3);
			};
		}
		
		private boolean hasLowerExpand() {
			// TODO: Implement later
			return false;
		}
		
		private final MouseAdapter adapter = new MouseAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				if( e.getY() <  DRAG_BORDER && hasLowerExpand())
					setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
				else if( e.getY() > getHeight() - DRAG_BORDER)
					setCursor(Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));
				else
					setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				
				super.mouseMoved(e);
			}
			
			@Override
			public void mousePressed(MouseEvent e) {
				if( state == null) {
					if( e.getY() <  DRAG_BORDER && hasLowerExpand())
						setState( new ResizingState(true));
					else if( e.getY() > getHeight() - DRAG_BORDER)
						setState( new ResizingState(false));
					else {
						ws.getAnimationManager().selectFrame(frame);
						setState( new DraggingFrameState());
					}
				}
				
				super.mousePressed(e);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				if( state instanceof FrameState)
					setState(null);
			};
			
			@Override
			public void mouseDragged(MouseEvent e) {
				if( state instanceof FrameState) {
					((FrameState)state).mouseDragged(e);
					content.repaint();
				}
			}
		};
		
		private abstract class FrameState extends State {
			abstract void mouseDragged(MouseEvent e);
		}
		
		private class DraggingFrameState extends FrameState {
			int target;
			
			DraggingFrameState() {
				this.target = frame.getStart();
			}
			
			@Override
			void mouseDragged(MouseEvent e) {
				target = TickAtY( SwingUtilities.convertMouseEvent(FramePanel.this, e, content).getY());
				if( target > frame.getStart() && target < frame.getEnd())
					target = frame.getStart();
			}

			@Override
			void EndState() {
				if( target != frame.getStart()) {
					frame.getLayerContext().moveFrame(frame, target, target >= frame.getStart());
				}
				
				//frame.getLayerContext().moveNode(moveNode);
			}

			@Override
			void StartState() {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			void Draw(Graphics g) {
				Rectangle rect = GetFrameBounds( column, target);
				
				g.setColor( DRAG_BORDER_COLOR);
				g.drawRect(rect.x, rect.y, rect.width, rect.height);
			}
		}
		
		private class ResizingState extends FrameState {
			int floatTick;
			boolean north;
			
			ResizingState(boolean northOrientation) {
				this.north = northOrientation;
				this.floatTick = (north) ? frame.getStart():frame.getEnd();
			}
			@Override
			void EndState() {
				int length = floatTick - frame.getStart();
				frame.setLength(length);
			}
			@Override void StartState() {}
			
			@Override
			void mouseDragged(MouseEvent e) {
				int tickAt = TickAtY( SwingUtilities.convertMouseEvent(FramePanel.this, e, content).getY() + ROW_HEIGHT/2);
				
				floatTick = (north) ?
						Math.min( frame.getEnd(), tickAt + 1) :
						Math.max( frame.getStart(), tickAt);
			}
			
			@Override
			void Draw(Graphics g) {
				Rectangle startRect = GetFrameBounds( column, (north)?floatTick:frame.getStart());
				Rectangle endRect = GetFrameBounds( column, north?frame.getEnd():floatTick);
				
				int dy = startRect.y;
				int dh = endRect.y - startRect.y;
				
				g.drawRect( startRect.x, dy, startRect.width, dh);
			}
			
		}
	}

	private static final int DRAG_BORDER = 4;
	
	// state management
	private State state = null;
	private void setState( State newState) {
		if( state == newState)
			return;
		
		if( state != null)
			state.EndState();
		
		state = newState;
		if( state != null)
			state.StartState();
		
		repaint();
	}
	
	private abstract class State {
		abstract void EndState();
		abstract void StartState();
		void Draw( Graphics g) {}
	}

	// :::: MAnimationStateObserver
	@Override
	public void selectedAnimationChanged(MAnimationStateEvent evt) {
		repaint();
	}
	@Override
	public void animationFrameChanged(MAnimationStateEvent evt) {
		repaint();
	}
	@Override
	public void viewStateChanged(MAnimationStateEvent evt) {
		repaint();
	}
}
