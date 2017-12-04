//package spirite.pc.ui.panel_layers.anim;
//
//import java.awt.Color;
//import java.awt.Component;
//import java.awt.Cursor;
//import java.awt.Font;
//import java.awt.Graphics;
//import java.awt.Point;
//import java.awt.Rectangle;
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
//import java.awt.event.MouseAdapter;
//import java.awt.event.MouseEvent;
//import java.util.ArrayList;
//import java.util.List;
//
//import javax.swing.BorderFactory;
//import javax.swing.GroupLayout;
//import javax.swing.GroupLayout.Group;
//import javax.swing.JLabel;
//import javax.swing.JOptionPane;
//import javax.swing.JPopupMenu;
//import javax.swing.SwingConstants;
//import javax.swing.SwingUtilities;
//import javax.swing.Timer;
//
//import spirite.base.brains.MasterControl;
//import spirite.base.brains.MasterControl.MWorkspaceObserver;
//import spirite.base.graphics.RenderProperties;
//import spirite.base.image_data.AnimationManager.AnimationState;
//import spirite.base.image_data.AnimationManager.MAnimationStateEvent;
//import spirite.base.image_data.AnimationManager.MAnimationStateObserver;
//import spirite.base.image_data.GroupTree.AnimationNode;
//import spirite.base.image_data.GroupTree.LayerNode;
//import spirite.base.image_data.GroupTree.Node;
//import spirite.base.image_data.ImageWorkspace;
//import spirite.base.image_data.ImageWorkspace.MNodeSelectionObserver;
//import spirite.base.image_data.animations.AnimationState;
//import spirite.base.image_data.animations.FixedFrameAnimation;
//import spirite.base.image_data.animations.FixedFrameAnimation.AnimationLayer;
//import spirite.base.image_data.animations.FixedFrameAnimation.AnimationLayer.Frame;
//import spirite.base.image_data.animations.FixedFrameAnimation.Marker;
//import spirite.base.image_data.layers.ReferenceLayer;
//import spirite.base.util.Colors;
//import spirite.gui.hybrid.SLabel;
//import spirite.gui.hybrid.SPanel;
//import spirite.gui.hybrid.SToggleButton;
//import spirite.hybrid.Globals;
//import spirite.hybrid.MDebug;
//import spirite.hybrid.MDebug.WarningType;
//import spirite.pc.graphics.ImageBI;
//import spirite.pc.ui.ContextMenus;
//import spirite.pc.ui.components.OmniEye;
//import spirite.pc.ui.dialogs.RenderPropertiesDialog;
//import spirite.pc.ui.panel_layers.anim.dialogs.ResizeLocalLoopDialog;
//
//
///***
// * AnimationSchemePanel is a grid
// */
//public class FFAnimationSchemePanel extends SPanel
//	implements MWorkspaceObserver, MNodeSelectionObserver, MAnimationStateObserver
//{
//	// :::: Control Links
//	private final MasterControl master;
//	private ImageWorkspace ws = null;
//	Point cmenuPoint;
//
//	// ::: etc
//	private Component[] titles = null;
//	private boolean expanded = true; // Needs to be set before MainTitleBar is created
//
//
//	// :::: Constants
//	private final int MAIN_TITLE_BAR_HEIGHT = 24;
//	private final int LAYER_TITLE_BAR_HEIGHT = 16;
//	private final int BOTTOM_BAR_HEIGHT = 16;
//	private final int TL_WIDTH = 16;
//	private final int ROW_HEIGHT = 32;
//
//	private static final Color ARROW_COLOR = Color.RED;
//	private static final Color DRAG_BORDER_COLOR = Color.green;
//	private static final Color DRAG_LOOP_COLOR = Color.BLACK;
//	private static final Color TITLE_BG = Color.WHITE;
//	private final Color tickColor = Globals.getColor("animSchemePanel.tickBG");
//	private final int HOLD_DELAY = 400;
//
//	private final FixedFrameAnimation animation;
//	private final AnimationNode node;
//
//
//	public FFAnimationSchemePanel( MasterControl master, AnimationNode node) {
//		if( node == null)
//			throw new RuntimeException("Null Animation for AnimationSchemePanel");
//		this.node = node;
//		this.master = master;
//		this.animation = (FixedFrameAnimation)node.getAnimation();
//
//		this.titleBar = new MainTitleBar();
//		BuildFromAnimation();
//		Rebuild();
//
//		master.addWorkspaceObserver(this);
//		ws = master.getCurrentWorkspace();
//		if( ws != null) {
//			ws.addSelectionObserver(this);
//			ws.getAnimationManager().addAnimationStateObserver(this);
//		}
//	}
//
//	// :::: Components
//	private final SPanel topLeft = new SPanel();
//	private final SPanel bottomRight = new SPanel();
//	private final MainTitleBar titleBar;
//	private final SPanel content = new SPanel() {
//		@Override
//		protected void paintComponent(Graphics g) {
//			super.paintComponent(g);
//
//			int s = animation.getStart();
//			int e = animation.getEnd();
//			AnimationState as = ws.getAnimationManager().getAnimationState(animation);
//
//			int selT = (as == null)?0:(int)Math.floor(as.getSelectedMetronome());
//
//			for( int t=s; t<e; ++t) {
//				Color c = Color.WHITE;
//				if(  selT == t)
//					c = Color.YELLOW;
//				else {
//					if( as != null && as.hasSubstateForRelativeTick(as.cannonizeRelTick(t)))
//						c = Color.green;
//				}
//
//				if( as != null && (int)Math.floor(as.getMetronom()) == t)
//					c = Colors.darken(c);
//
//				g.setColor(c);
//				g.fillRect( 0, LAYER_TITLE_BAR_HEIGHT + ROW_HEIGHT * (t - animation.getStart()), getWidth(), ROW_HEIGHT);
//			}
//		}
//
//		@Override
//		public void paint(Graphics g) {
//			super.paint(g);
//			if( state != null)
//				state.Draw(g);
//		}
//	};
//
//
//	public void Rebuild() {
//		GroupLayout primaryLayout = new GroupLayout(this);
//
//		Group hor = primaryLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(titleBar);
//		Group vert= primaryLayout.createSequentialGroup()
//						.addComponent(titleBar, MAIN_TITLE_BAR_HEIGHT, MAIN_TITLE_BAR_HEIGHT, MAIN_TITLE_BAR_HEIGHT);
//		if( expanded) {
//			hor.addComponent(content);
//			vert.addComponent(content);
//		}
//		primaryLayout.setHorizontalGroup(hor);
//		primaryLayout.setVerticalGroup(vert);
//		this.setLayout(primaryLayout);
//	}
//
//	private void BuildFromAnimation() {
//		// Called once at the beginning, would only need to be re-called if animation structure changed
//		//	(but for now the entire tree is rebuilt on animation structure change)
//		int start = animation.getStart();
//		int end = animation.getEnd();
//
//		this.removeAll();
//
//		titleBar.setTitle(animation.getName());
//		titleBar.addMouseListener(new MouseAdapter() {
//			@Override
//			public void mousePressed(MouseEvent e) {
//				if( e.getButton() == MouseEvent.BUTTON3) {
//					contextMenu.removeAll();
//
//
//					ContextMenus.constructMenu(contextMenu, new Object[][] {
//						{"&Delete Animation", "deleteAnimation"},
//					}, cmenuListener);
//
//					contextMenu.show(titleBar, e.getX(), e.getY());
//				}
//			}
//		});
//
//		List<AnimationLayer> layers = animation.getLayers();
//
//		GroupLayout layout = new GroupLayout(content);
//
//		Group horGroup = layout.createSequentialGroup();
//		Group vertGroup = layout.createSequentialGroup();
//
//		Group[] horGroups = new Group[layers.size() + 1];
//		for( int i=0; i < layers.size() + 1; ++i) {
//			horGroups[i] = layout.createParallelGroup();
//			horGroup.addGroup(horGroups[i]);
//		}
//
//		// Title Bar
//		Group titleVertGroup = layout.createParallelGroup();
//		vertGroup.addGroup(titleVertGroup);
//		horGroups[0].addComponent(topLeft, TL_WIDTH,TL_WIDTH,TL_WIDTH);
//		titleVertGroup.addComponent(topLeft, LAYER_TITLE_BAR_HEIGHT,LAYER_TITLE_BAR_HEIGHT,LAYER_TITLE_BAR_HEIGHT);
//
//		titles= new Component[layers.size()];
//
//		for( int i=0; i < layers.size(); ++i) {
//			AnimationLayer layer = layers.get(i);
//			titles[i] = new LayerTitleBar(layer.getName());
//			horGroups[i+1].addComponent(titles[i]);
//			titleVertGroup.addComponent( titles[i], LAYER_TITLE_BAR_HEIGHT,LAYER_TITLE_BAR_HEIGHT,LAYER_TITLE_BAR_HEIGHT);
//		}
//		if( titles.length > 0)
//			layout.linkSize(SwingConstants.HORIZONTAL, titles);
//
//		// Animation Bars
//		Component[] ticks = new Component[Math.max(0,end-start)];
//		Group vertMid = layout.createParallelGroup();
//		vertGroup.addGroup(vertMid);
//		{
//			Group vertInner = layout.createSequentialGroup();
//			vertMid.addGroup(vertInner);
//			for( int i = start; i < end; ++i) {
//
//				ticks[i-start] = new TickPanel(i);
//				horGroups[0].addComponent(ticks[i-start], TL_WIDTH,TL_WIDTH,TL_WIDTH);
//				vertInner.addComponent(ticks[i-start],ROW_HEIGHT,ROW_HEIGHT,ROW_HEIGHT);
//			}
//		}
//
//		if( ticks.length > 0)
//			layout.linkSize(SwingConstants.VERTICAL, ticks);
//
//		// Fill out the animation
//		for( int col=0; col < layers.size(); ++col) {
//			Group vertInner = layout.createSequentialGroup();
//			vertMid.addGroup(vertInner);
//			AnimationLayer layer = layers.get(col);
//
//			int currentTick = start;
//
//			List<Component> linked = new ArrayList<>();
//			for(Frame frame : layer.getFrames()) {
//				if( frame.getMarker() == Marker.START_LOCAL_LOOP && frame.getLength() > 0) {
//					int fs = frame.getStart();
//					int fe = frame.getEnd();
//
//					Component component1 = new SoFFramePanel(frame, col);
//					Component component2 = new EoFFramePanel(frame, col);
//					linked.add(component1);
//					linked.add(component2);
//
//					int h1 = SoFFramePanel.HEIGHT;
//					int h2 = EoFFramePanel.HEIGHT;
//					if( fs > currentTick)
//						vertInner.addGap(ROW_HEIGHT * (fs - currentTick));
//					vertInner.addComponent(component1, h1, h1, h1);
//					vertInner.addGap( ROW_HEIGHT*(fe-fs) - h1 - h2);
//					vertInner.addComponent(component2, h2, h2, h2);
//					horGroups[1+col].addComponent(component1);
//					horGroups[1+col].addComponent(component2);
//
//					currentTick = fe;
//				}
//			}
//
//
//			vertInner = layout.createSequentialGroup();
//			vertMid.addGroup(vertInner);
//			currentTick = start;
//			for(Frame frame : layer.getFrames()) {
//				if( frame.getMarker() == Marker.FRAME)
//				{
//					int fs = Math.max( currentTick,frame.getStart());
//					int fe = Math.max(fs, Math.min(end, frame.getEnd()));
//
//					if( fs < fe) {
//						if( fs > currentTick)
//							vertInner.addGap(ROW_HEIGHT * (fs - currentTick));
//
//						Component component = new FrameFramePanel( frame, col);
//						linked.add(component);
//						int h = ROW_HEIGHT * (fe-fs);
//						vertInner.addComponent(component,h,h,h);
//						horGroups[1+col].addComponent(component);
//
//						currentTick = fe;
//					}
//				}
//			}
//
//			linked.add(titles[col]);
//			layout.linkSize(SwingConstants.HORIZONTAL, linked.toArray( new Component[linked.size()]));
//		}
//
//		// Lower Bar
//		Group bottomVertGroup = layout.createParallelGroup();
//		vertGroup.addGroup(bottomVertGroup);
//
//		bottomVertGroup.addComponent(bottomRight, BOTTOM_BAR_HEIGHT, BOTTOM_BAR_HEIGHT, BOTTOM_BAR_HEIGHT);
//		horGroups[0].addComponent(bottomRight, TL_WIDTH,TL_WIDTH,TL_WIDTH);
//
//		for( int j=0; j < layers.size(); ++j) {
//			AnimationLayer layer = layers.get(j);
//
//			Component c = new BottomPanel(j, layer);
//
//			horGroups[j+1].addComponent(c);
//			bottomVertGroup.addComponent(c);
//			layout.linkSize(SwingConstants.HORIZONTAL, c, titles[j]);
//		}
//
//		layout.setHorizontalGroup(horGroup);
//		layout.setVerticalGroup(vertGroup);
//
//		content.addMouseListener( new MouseAdapter() {
//			public void mousePressed(MouseEvent e) {
//				int t = TickAtY(e.getY());
//				if( t >= 0 && t < end)
//					ws.getAnimationManager().getAnimationState(animation).setSelectedMetronome(t);
//			}
//		});
//
//		content.setLayout(layout);
//	}
//
//
//	// ======================
//	// ==== Context Menu ====
//	private final JPopupMenu contextMenu = new JPopupMenu();
//	private Object cmenuObject;
//	private final ActionListener cmenuListener = (ActionEvent e) -> {
//		_doAction(e.getActionCommand());
//	};
//	private void _doAction( String command) {
//		switch( command) {
//		case "duplicate": {
//			ws.duplicateNode(((Frame)cmenuObject).getLinkedNode());
//			break;}
//		case "insertEmptyBefore":{
//			Frame f = (Frame)cmenuObject;
//			f.setGapBefore(f.getGapBefore()+1);
//			break;}
//		case "insertEmpty":{
//			Frame f = (Frame)cmenuObject;
//			f.setGapAfter(f.getGapAfter()+1);
//			break;}
//		case "deleteNode":{
//			ws.removeNode(((Frame)cmenuObject).getLinkedNode());
//			break;}
//		case "removeGapBefore":{
//			Frame f = (Frame)cmenuObject;
//			f.setGapBefore(0);
//			break;}
//		case "removeGapAfter":{
//			Frame f = (Frame)cmenuObject;
//			f.setGapAfter(0);
//			break;}
//		case "localLoop":{
//			Frame f = (Frame)cmenuObject;
//			f.getLayerContext().wrapInLoop(f);
//			break;}
//		case "refer":{
//			LayerNode ln = ((LayerNode)((Frame)cmenuObject).getLinkedNode());
//			if( ln.getLayer() instanceof ReferenceLayer)
//				ws.addNewReferenceLayer(ln.getParent(), ((ReferenceLayer)ln.getLayer()).getUnderlying(), ln.getName());
//			else
//				ws.addNewReferenceLayer(ln.getParent(), ln, "*"+ln.getName()+"*");;
//			break;}
//		case "resizeLocalLoop":{
//			Frame frame = ((Frame)cmenuObject);
//			ResizeLocalLoopDialog dia = new ResizeLocalLoopDialog(frame.getLength(), false);
//			Point p = new Point(cmenuPoint);
//			SwingUtilities.convertPointToScreen(p, this);
//
//			dia.setLocation(p);
//			dia.setVisible(true);
//
//			if( dia.success) {
//				if( dia.inLoops) {
//					// Get "Default" Local Loop Length
//					int loopLen = 0;
//
//					Frame frameIt = frame.next();
//
//					int depth = 0;
//					while( depth >= 0) {
//						if( depth == 0)
//							loopLen += frameIt.getLength();
//						if( frameIt.getMarker() == Marker.START_LOCAL_LOOP)
//							depth++;
//						if( frameIt.getMarker() == Marker.END_LOCAL_LOOP)
//							depth--;
//						frameIt = frameIt.next();
//					}
//
//					frame.setLength(dia.length * loopLen);
//				}
//				else
//					frame.setLength(dia.length);
//			}
//			break;}
//		case "deleteAnimation":{
//			ws.getAnimationManager().removeAnimation(animation);
//			break;}
//		default:
//			MDebug.handleWarning(WarningType.REFERENCE, null, "Unrecognized Menu Item for FFAnimationPanel Context Menu: " + command);
//		}
//	}
//
//	private void _openContextMenuForFrame( int layerIndex, int tick, Point p) {
//		List<AnimationLayer> layers = animation.getLayers();
//		if( layerIndex < 0 || layerIndex >= layers.size())
//			return;
//
//		_openContextMenuForFrame(layers.get(layerIndex), tick, p);
//	}
//
//	private void _openContextMenuForFrame(AnimationLayer layer, int tick, Point p) {
//		Frame frame = layer.getFrameForMet(tick, true);
//		_openContextMenuForFrame( frame, p);
//	}
//	private void _openContextMenuForFrame(Frame frame, Point p) {
//		contextMenu.removeAll();
//		cmenuObject = frame;
//		cmenuPoint = p;
//		SwingUtilities.convertPointFromScreen(cmenuPoint, this);
//
//		switch( frame.getMarker()) {
//		case FRAME:{
//			Object[][] menuScheme = {
//				{"D&uplicate Node", "duplicate"},
//				{"Insert Empty &Before", "insertEmptyBefore"},
//				{"Insert &Empty After", "insertEmpty"},
//				//{"Wrap in Local Loop","localLoop"},
//				{"&Delete Node","deleteNode"},
//				{"Copy As Reference","refer"},
//			};
//			ContextMenus.constructMenu(contextMenu, menuScheme, cmenuListener);
//			contextMenu.show(this, cmenuPoint.x, cmenuPoint.y);
//			break;}
//		case START_LOCAL_LOOP:{
//			contextMenu.removeAll();
//
//			Object[][] menuScheme = {
//				{"&Resize Local Loop", "resizeLocalLoop"},
//			};
//			cmenuObject = frame;
//			ContextMenus.constructMenu(contextMenu, menuScheme, cmenuListener);
//			contextMenu.show(this, cmenuPoint.x, cmenuPoint.y);
//			break;}
//		default:
//			break;
//		}
//	}
//
//
//	// =====================================
//	// ==== Coordinate / Position Stuff ====
//	private Rectangle GetFrameBounds( int layer, int tick) {
//
//		int sx = TL_WIDTH;
//		int sy = LAYER_TITLE_BAR_HEIGHT + ROW_HEIGHT * (tick - animation.getStart());
//
//
//		for( int i=0; i < layer; ++i)
//			sx += titles[i].getWidth();
//
//		return new Rectangle( sx, sy, titles[layer].getWidth(), ROW_HEIGHT);
//	}
//	private int TickAtY( int y) {
//		return (y - LAYER_TITLE_BAR_HEIGHT)/ROW_HEIGHT;
//	}
//	private AnimationLayer LayerAtX( int x) {
//		int ind = (x - TL_WIDTH) / LAYER_TITLE_BAR_HEIGHT;
//		List<AnimationLayer> layers = animation.getLayers();
//
//		return (ind < 0 || ind >= layers.size()) ? null : layers.get(ind);
//	}
//
//	// :::: WorkspaceObserver
//	@Override	public void newWorkspace(ImageWorkspace newWorkspace) {	}
//	@Override	public void removeWorkspace(ImageWorkspace newWorkspace) {	}
//	@Override
//	public void currentWorkspaceChanged(ImageWorkspace selected, ImageWorkspace previous) {
//		if( ws != null) {
//			ws.removeSelectionObserver(this);
//			ws.getAnimationManager().removeAnimationStateObserver(this);
//		}
//		ws = selected;
//		if( ws != null) {
//			ws.addSelectionObserver(this);
//			ws.getAnimationManager().addAnimationStateObserver(this);
//		}
//	}
//
//	// :::: SelectionOberver
//	@Override
//	public void selectionChanged(Node newSelection) {
//		this.repaint();
//	}
//
//	private class MainTitleBar extends SPanel {
//		String title;
//		private final SLabel label = new SLabel();
//		private final SToggleButton btnExpand = new SToggleButton();
//		private final SToggleButton btnVisible = new SToggleButton();
//
//		private MainTitleBar() {
//			btnExpand.setOpaque(false);
//			btnExpand.setBackground(new Color(0,0,0,0));
//			btnExpand.setBorder(null);
//
//			btnVisible.setOpaque(false);
//			btnVisible.setBackground(new Color(0,0,0,0));
//			btnVisible.setBorder(null);
//
//			btnExpand.setIcon(Globals.getIcon("icon.expanded"));
//			btnExpand.setRolloverIcon(Globals.getIcon("icon.expandedHL"));
//			btnExpand.setSelectedIcon(Globals.getIcon("icon.unexpanded"));
//			btnExpand.setRolloverSelectedIcon(Globals.getIcon("icon.unexpandedHL"));
//
//			btnVisible.setIcon(Globals.getIcon("visible_off"));
//			btnVisible.setSelectedIcon(Globals.getIcon("visible_on"));
//
//			this.setBackground(TITLE_BG );
//
//			label.setFont( new Font("Tahoma",Font.BOLD, 12));
//			label.setText(title);
//
//			initLayout();
//			initBindings();
//
//			btnExpand.setSelected(expanded);
//			btnVisible.setSelected(node.getRender().isVisible());
//		}
//
//		private void initLayout() {
//			GroupLayout layout = new GroupLayout(this);
//
//			layout.setHorizontalGroup( layout.createSequentialGroup()
//					.addGap(2)
//					.addComponent(btnExpand, 12, 12, 12)
//					.addGap(2)
//					.addComponent(btnVisible, 24, 24, 24)
//					.addGap(4)
//					.addComponent(label)
//					.addContainerGap(0, Short.MAX_VALUE));
//			layout.setVerticalGroup( layout.createParallelGroup(GroupLayout.Alignment.CENTER)
//					.addComponent(btnExpand)
//					.addComponent(btnVisible)
//					.addComponent(label));
//
//			this.setLayout(layout);
//		}
//
//		private void initBindings() {
//			btnExpand.addActionListener(new ActionListener() {
//				@Override public void actionPerformed(ActionEvent e) {
//					expanded = btnExpand.isSelected();
//					Rebuild();
//				}
//			});
//			btnVisible.addActionListener( new ActionListener() {
//				@Override
//				public void actionPerformed(ActionEvent e) {
//					node.getRender().setVisible(btnVisible.isSelected());
//					repaint();
//				}
//			});
//		}
//
//		public void setTitle(String title) {
//			this.title = title;
//			label.setText(title);
//		}
//	}
//
//	private class LayerTitleBar extends SPanel {
//		String title;
//		private final JLabel label = new JLabel();
//
//		private LayerTitleBar( String title) {
//			//this.setLayout(new GridLayout());
//
//			this.setBackground(TITLE_BG );
//			this.title = title;
//
//			label.setFont( new Font("Tahoma",Font.BOLD, 10));
//
//			this.add(label);
//			label.setText(title);
//		}
//	}
//
//	private class TickPanel extends SPanel {
//		private int tick;
//		private final JLabel label = new JLabel();
//		private final SPanel eyeIcon = new SPanel() {
//			@Override
//			protected void paintComponent(Graphics g) {
//				AnimationState as = ws.getAnimationManager().getAnimationState(animation);
//				String icon = (as != null && as.getSubstateForRelativeTick( as.cannonizeRelTick(tick)).isVisible()) ? "icon.rig.visOn" : "icon.rig.visOff";
//				g.drawImage( Globals.getIcon(icon).getImage(), 0, 0, null);
//				super.paintComponent(g);
//			}
//		};
//
//		private TickPanel( int tick) {
//			this.setOpaque(false);
//			this.tick = tick;
//
//			label.setText(""+tick);
//			label.setFont( new Font("Tahoma",Font.BOLD, 10));
//			eyeIcon.setOpaque(false);
//
//			this.setBackground( tickColor);
//			this.addMouseListener( adapter);
//
//			GroupLayout layout = new GroupLayout(this);
//			layout.setHorizontalGroup( layout.createSequentialGroup()
//					.addGap(2)
//					.addGroup( layout.createParallelGroup(GroupLayout.Alignment.CENTER)
//						.addComponent(label)
//						.addComponent(eyeIcon, 12, 12, 12))
//					.addGap(2));
//			layout.setVerticalGroup( layout.createSequentialGroup()
//					.addComponent(label)
//					.addGap(2)
//					.addComponent(eyeIcon, 12, 12, 12));
//
//			this.setLayout(layout);
//
//			this.add(label);
//		}
//
//		private boolean rcConsumed;
//		private final MouseAdapter adapter = new MouseAdapter() {
//			Timer timer;
//
//			@Override
//			public void mouseClicked(MouseEvent e) {
//				if( e.getButton() == MouseEvent.BUTTON1) {
//					ws.getAnimationManager().getAnimationState(animation).setSelectedMetronome(tick);
//				}
//			}
//
//			@Override
//			public void mousePressed(MouseEvent e) {
//				if( e.getButton() == MouseEvent.BUTTON3) {
//					if( timer != null)
//						timer.stop();
//
//					rcConsumed = false;
//					timer =
//					(new Timer( HOLD_DELAY, new ActionListener() { @Override public void actionPerformed(ActionEvent e) {
//						if( !rcConsumed) {
//							rcConsumed = true;
//							AnimationState as = ws.getAnimationManager().getAnimationState(animation);
//							int relTick = as.cannonizeRelTick(tick);
//							RenderPropertiesDialog dialog = new RenderPropertiesDialog(as.getSubstateForRelativeTick(relTick), master);
//							JOptionPane.showConfirmDialog(TickPanel.this, dialog, null, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
//
//							as.putSubstateForRelativeTick( relTick, dialog.getResult());
//							timer = null;
//						}
//					}}));
//					timer.start();
//				}
//			}
//
//			public void mouseReleased(MouseEvent e) {
//				if( e.getButton() == MouseEvent.BUTTON3 & !rcConsumed) {
//					rcConsumed = true;
//					if( timer != null) {
//						timer.stop();
//						timer= null;
//					}
//
//					AnimationState as = ws.getAnimationManager().getAnimationState(animation);
//					int relTick = as.cannonizeRelTick(tick);
//					RenderProperties properties = as.getSubstateForRelativeTick(relTick);
//					properties.visible = !properties.visible;
//					as.putSubstateForRelativeTick(relTick, properties);
//
//
//				}
//			};
//		};
//	}
//
//	private class BottomPanel extends SPanel {
//		private final SToggleButton btnLock = new SToggleButton("X");
//		private final SToggleButton btnSettings = new SToggleButton("S");
//		private final SToggleButton omniEye = new OmniEye();
//		private final int column;
//		private final AnimationLayer layer;
//
//		private boolean visible = true;
//
//		private BottomPanel( int col, AnimationLayer layer) {
//			this.column = col;
//			this.layer = layer;
//
//			btnLock.setBorder(null);
//			btnSettings.setBorder(null);
//
//
//			GroupLayout layout = new GroupLayout(this);
//			layout.setHorizontalGroup( layout.createSequentialGroup()
//					.addGroup( layout.createParallelGroup()
//							.addComponent(btnLock)
//							.addComponent(btnSettings))
//					.addComponent(omniEye));
//			this.setBorder( BorderFactory.createLineBorder(Color.BLACK));
//			layout.setVerticalGroup( layout.createParallelGroup()
//					.addGroup( layout.createSequentialGroup()
//							.addComponent(btnLock)
//							.addComponent(btnSettings))
//					.addComponent(omniEye));
//
//			omniEye.addActionListener( (e) -> {
//				ws.getUndoEngine().doAsAggregateAction(() -> {
//					visible = !visible;
//					for( Frame frame : layer.getFrames()) {
//						if(frame.getLinkedNode() != null)
//							frame.getLinkedNode().getRender().setVisible(visible);
//					}
//				}, "Toggle Layer Visibility");
//			});
//
//			layout.linkSize( SwingConstants.VERTICAL, btnLock,btnSettings);
//
//			this.setLayout(layout);
//		}
//	}
//
//	private class SoFFramePanel extends SPanel {
//		private static final int HEIGHT = 6;
//		private final Frame frame;
//		private final int column;
//
//		SoFFramePanel( Frame frame, int column) {
//			this.frame = frame;
//			this.column = column;
//			this.setBackground(Color.ORANGE);
//
//			this.addMouseListener(adapter);
//			this.addMouseMotionListener(adapter);
//		}
//
//
//		MouseAdapter adapter = new MouseAdapter() {
//			public void mousePressed(MouseEvent e) {
//				if( e.getButton() == MouseEvent.BUTTON1)
//					setState(new ResizingLoopState(column, frame.getStart(), frame.getEnd(), frame));
//				else if( e.getButton() == MouseEvent.BUTTON3) {
//					Point p = e.getPoint();
//					SwingUtilities.convertPointToScreen(p, SoFFramePanel.this);
//					_openContextMenuForFrame( frame, p);
//				}
//			}
//			public void mouseDragged(MouseEvent e) {
//				if( state instanceof ResizingLoopState) {
//					((ResizingLoopState) state).start = TickAtY( SwingUtilities.convertMouseEvent(SoFFramePanel.this, e, content).getY());
//					content.repaint();
//				}
//			}
//			@Override
//			public void mouseReleased(MouseEvent e) {
//				setState( null);
//			}
//		};
//	}
//
//	private class EoFFramePanel extends SPanel {
//		private static final int HEIGHT = 6;
//		private final Frame frame;
//		private final int column;
//
//		EoFFramePanel( Frame frame, int column) {
//			this.frame = frame;
//			this.column = column;
//			this.setBackground(Color.ORANGE);
//
//			this.addMouseListener(adapter);
//			this.addMouseMotionListener(adapter);
//		}
//
//		MouseAdapter adapter = new MouseAdapter() {
//			public void mousePressed(MouseEvent e) {
//				if( e.getButton() == MouseEvent.BUTTON1)
//					setState(new ResizingLoopState(column, frame.getStart(), frame.getEnd(), frame));
//				else if( e.getButton() == MouseEvent.BUTTON3) {
//					Point p = e.getPoint();
//					SwingUtilities.convertPointToScreen(p, EoFFramePanel.this);
//					_openContextMenuForFrame( frame, p);
//				}
//			}
//			public void mouseDragged(MouseEvent e) {
//				if( state instanceof ResizingLoopState) {
//					((ResizingLoopState) state).end = TickAtY( SwingUtilities.convertMouseEvent(EoFFramePanel.this, e, content).getY()) +1;
//					content.repaint();
//				}
//			}
//			@Override
//			public void mouseReleased(MouseEvent e) {
//				setState( null);
//			}
//		};
//	}
//	private class ResizingLoopState extends State {
//		int column;
//		int start = 0;
//		int end = 0;
//		Frame sofFrame;
//
//		ResizingLoopState( int c, int s, int e, Frame sofFrame)
//		{this.column = c; this.start = s; this.end = e; this.sofFrame = sofFrame;}
//
//		@Override
//		void EndState() {
//			if( end >= start) {
//				sofFrame.setLength(end-start+1);
//				//sofFrame.getLayerContext().reWrap(sofFrame, start, end, true);
//			}
//		}
//
//		@Override
//		void StartState() {
//
//		}
//
//		@Override
//		void Draw(Graphics g) {
//			Rectangle rect = GetFrameBounds( column, start)
//					.union( GetFrameBounds(column, end));
//
//			g.setColor( DRAG_LOOP_COLOR);
//			g.drawRect(rect.x, rect.y, rect.width, rect.height);
//		}
//	}
//
//	private class FrameFramePanel extends SPanel {
//		private final Frame frame;
//		private final int column;
//
//		private final SPanel btnVLock = new SPanel();
//		private final SPanel btnLLock = new SPanel();
//
//		private final SPanel drawPanel = new SPanel() {
//			@Override
//			protected void paintComponent(Graphics g) {
//				super.paintComponent(g);
//
//				if( frame.getMarker() == Marker.FRAME) {
//					ImageBI img = (ImageBI)master.getRenderEngine().accessThumbnail(frame.getLinkedNode(), ImageBI.class);
//
//					if( img != null)
//						g.drawImage( img.img, 0, 0, this.getWidth(), this.getHeight(), 0, 0, img.getWidth(), img.getHeight(), null);
//				}
//
//			}
//		};
//
//		private FrameFramePanel( Frame frame, int column) {
//			this.frame = frame;
//			this.column = column;
//
//			btnVLock.setBorder(null);
//			btnLLock.setBorder(null);
//			btnVLock.setBackground(Color.RED);
//			btnLLock.setBackground(Color.ORANGE);
//
//			this.setOpaque(false);
//
//			this.addMouseListener( adapter);
//			this.addMouseMotionListener(adapter);
//			//drawPanel.addMouseListener( adapter);
//
//			GroupLayout layout = new GroupLayout(this);
//
//
//			int LBTN_WIDTH = 8;
//			Group hor = layout.createParallelGroup();
//			Group vert = layout.createSequentialGroup();
//
//			int before = frame.getGapBefore();
//			if( before > 0) {
//				Component c = new NillFramePanel(false);
//				vert.addComponent(c, before*ROW_HEIGHT,before*ROW_HEIGHT,before*ROW_HEIGHT);
//				hor.addComponent(c);
//			}
//
//			hor.addGroup(
//					layout.createSequentialGroup()
//					.addGap(2)
//					.addGroup( layout.createParallelGroup()
//							.addComponent(btnVLock, LBTN_WIDTH, LBTN_WIDTH, LBTN_WIDTH)
//							.addComponent(btnLLock, LBTN_WIDTH, LBTN_WIDTH, LBTN_WIDTH))
//					.addGap(2,2, Short.MAX_VALUE)
//					.addComponent(drawPanel)
//					.addGap(2,2, Short.MAX_VALUE));
//
//			vert.addGroup(layout.createParallelGroup()
//					.addGroup( layout.createSequentialGroup()
//							.addGap(2)
//							.addComponent(btnVLock)
//							.addGap(2)
//							.addComponent(btnLLock)
//							.addGap(2))
//					.addGroup( layout.createSequentialGroup()
//							.addGap(2)
//							.addComponent(drawPanel, ROW_HEIGHT - 4,ROW_HEIGHT - 4,ROW_HEIGHT - 4)
//							.addGap(2)));
//
//			layout.setHorizontalGroup( hor);
//			layout.setVerticalGroup( vert);
//
//			int after = frame.getGapAfter();
//			int ext = frame.getLength() - before - after;
//			if( ext > 1) {
//				Component c = new FrameExtendPanel();
//				vert.addComponent(c, ext*ROW_HEIGHT, ext*ROW_HEIGHT, ext*ROW_HEIGHT);
//				hor.addComponent(c);
//			}
//
//			if( after > 0) {
//
//				Component c = new NillFramePanel(true);
//				vert.addComponent(c, after*ROW_HEIGHT,after*ROW_HEIGHT,after*ROW_HEIGHT);
//				hor.addComponent(c);
//			}
//
//			drawPanel.setBorder( BorderFactory.createLineBorder(Color.BLACK,2));
//
//			layout.linkSize( SwingConstants.VERTICAL, btnVLock,btnLLock);
//
//			this.setLayout(layout);
//		}
//
//		@Override
//		protected void paintComponent(Graphics g) {
//			if( ws == null)
//				setBackground( Color.WHITE);
//			else if( ws.getAnimationManager().getSelectedFrame() == frame)
//				setBackground( Color.YELLOW);
//			else if( ws.getSelectedNode() == frame.getLinkedNode())
//				setBackground( new Color(122,170,170));
//			else
//				setBackground( Color.WHITE);
//
//			super.paintComponent(g);
//		}
//
//		private class FrameExtendPanel extends SPanel {
//			public FrameExtendPanel() {
//				this.setOpaque(false);
//			}
//
//			protected void paintComponent(Graphics g) {
//				super.paintComponent(g);
//				g.setColor(ARROW_COLOR);
//
//				int b = 2;
//				int tw = 2;
//				int hw = 8;
//				int hh = 8;
//				int c = this.getWidth() / 2;
//				int H = this.getHeight();
//				int W = this.getWidth();
//
//				g.fillRect( c - tw/2, b, tw, H - 2*b - hh);
//				g.fillPolygon( new int[] {(W-hw)/2,(W+hw)/2,c}, new int[] {H-b-hh,H-b-hh,H-b}, 3);
//			};
//		}
//
//		private class NillFramePanel extends SPanel {
//			NillFramePanel(boolean isAfter) {
//				this.setOpaque(false);
//
//				this.addMouseListener( new MouseAdapter() {
//					@Override
//					public void mousePressed(MouseEvent e) {
//						if( e.getButton() == MouseEvent.BUTTON3 ) {
//							contextMenu.removeAll();
//							Object[][] menuScheme = {{"Remove Gap", (isAfter)?"removeGapAfter":"removeGapBefore"}};
//							cmenuObject = frame;
//							ContextMenus.constructMenu(contextMenu, menuScheme, cmenuListener);
//							contextMenu.show(NillFramePanel.this, e.getX(), e.getY());
//						}
//
//						Point p = e.getPoint();
//						p = SwingUtilities.convertPoint(NillFramePanel.this, p, content);
//						ws.getAnimationManager().getAnimationState(animation).setSelectedMetronome(
//								TickAtY(p.y));
//					}
//				});
//			}
//
//			@Override
//			protected void paintComponent(Graphics g) {
//				super.paintComponent(g);
//
//				g.setColor(Color.CYAN);
//				g.fillRect(2, 2, getWidth()-4, getHeight()-4);
//			}
//		}
//
//		private boolean hasLowerExpand() {
//			return false;
//		}
//
//		private final MouseAdapter adapter = new MouseAdapter() {
//			@Override
//			public void mouseMoved(MouseEvent e) {
//				if( e.getY() <  DRAG_BORDER && hasLowerExpand())
//					setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
//				else if( e.getY() > getHeight() - DRAG_BORDER)
//					setCursor(Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));
//				else
//					setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
//
//				super.mouseMoved(e);
//			}
//
//			@Override
//			public void mousePressed(MouseEvent e) {
//				if( e.getButton() == MouseEvent.BUTTON1) {
//					if( state == null) {
//						if( e.getY() <  DRAG_BORDER && hasLowerExpand())
//							setState( new ResizingState(true));
//						else if( e.getY() > getHeight() - DRAG_BORDER)
//							setState( new ResizingState(false));
//						else {
//							ws.getAnimationManager().selectFrame(frame);
//							setState( new DraggingFrameState());
//						}
//					}
//
//					ws.setSelectedNode(frame.getLinkedNode());
//
//					Point p = e.getPoint();
//					p = SwingUtilities.convertPoint( FrameFramePanel.this, p, content);
//					ws.getAnimationManager().getAnimationState(animation).setSelectedMetronome(TickAtY(p.y));
//				}
//				else if( e.getButton() == MouseEvent.BUTTON3) {
//					_openContextMenuForFrame(frame, e.getPoint());
//				}
//				super.mousePressed(e);
//			}
//
//			@Override
//			public void mouseReleased(MouseEvent e) {
//				if( state instanceof FrameState)
//					setState(null);
//			};
//
//			@Override
//			public void mouseDragged(MouseEvent e) {
//				if( state instanceof FrameState) {
//					((FrameState)state).mouseDragged(e);
//					content.repaint();
//				}
//			}
//		};
//
//		private abstract class FrameState extends State {
//			abstract void mouseDragged(MouseEvent e);
//		}
//
//		private class DraggingFrameState extends FrameState {
//			int target;
//
//			DraggingFrameState() {
//				this.target = frame.getStart();
//			}
//
//			@Override
//			void mouseDragged(MouseEvent e) {
//				target = TickAtY( SwingUtilities.convertMouseEvent(FrameFramePanel.this, e, content).getY());
//				if( target > frame.getStart() && target < frame.getEnd())
//					target = frame.getStart();
//			}
//
//			@Override
//			void EndState() {
//				if( target != frame.getStart())
//					frame.getLayerContext().moveFrame(frame, frame.getLayerContext().getFrameForMet(target), target >= frame.getStart());
//
//			}
//
//			@Override void StartState() {}
//			@Override
//			void Draw(Graphics g) {
//				Rectangle rect = GetFrameBounds( column, target);
//
//				g.setColor( DRAG_BORDER_COLOR);
//				g.drawRect(rect.x, rect.y, rect.width, rect.height);
//			}
//		}
//
//		private class ResizingState extends FrameState {
//			int floatTick;
//			boolean north;
//
//			ResizingState(boolean northOrientation) {
//				this.north = northOrientation;
//				this.floatTick = (north) ? frame.getStart():frame.getEnd();
//			}
//			@Override
//			void EndState() {
//				int length = floatTick - frame.getStart();
//				frame.setLength(length);
//			}
//			@Override void StartState() {}
//
//			@Override
//			void mouseDragged(MouseEvent e) {
//				int tickAt = TickAtY( SwingUtilities.convertMouseEvent(FrameFramePanel.this, e, content).getY() + ROW_HEIGHT/2);
//
//				floatTick = (north) ?
//						Math.min( frame.getEnd(), tickAt + 1) :
//						Math.max( frame.getStart(), tickAt);
//			}
//
//			@Override
//			void Draw(Graphics g) {
//				Rectangle startRect = GetFrameBounds( column, (north)?floatTick:frame.getStart());
//				Rectangle endRect = GetFrameBounds( column, north?frame.getEnd():floatTick);
//
//				int dy = startRect.y;
//				int dh = endRect.y - startRect.y;
//
//				g.drawRect( startRect.x, dy, startRect.width, dh);
//			}
//
//		}
//	}
//
//	private static final int DRAG_BORDER = 4;
//
//	// state management
//	private State state = null;
//	private void setState( State newState) {
//		if( state == newState)
//			return;
//
//		if( state != null)
//			state.EndState();
//
//		state = newState;
//		if( state != null)
//			state.StartState();
//
//		repaint();
//	}
//
//	private abstract class State {
//		abstract void EndState();
//		abstract void StartState();
//		void Draw( Graphics g) {}
//	}
//
//	// :::: MAnimationStateObserver
//	@Override
//	public void selectedAnimationChanged(MAnimationStateEvent evt) {
//		repaint();
//	}
//	@Override
//	public void animationFrameChanged(MAnimationStateEvent evt) {
//		repaint();
//	}
//	@Override
//	public void viewStateChanged(MAnimationStateEvent evt) {
//		repaint();
//	}
//}
