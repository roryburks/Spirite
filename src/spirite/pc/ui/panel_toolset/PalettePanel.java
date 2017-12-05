// Rory Burks

package spirite.pc.ui.panel_toolset;

import spirite.base.brains.MasterControl;
import spirite.base.brains.PaletteManager;
import spirite.base.brains.PaletteManager.MPaletteObserver;
import spirite.base.brains.PaletteManager.Palette;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.util.DataBinding;
import spirite.base.util.DataBinding.ChangeExecuter;
import spirite.base.util.MUtil;
import spirite.gui.hybrid.SButton;
import spirite.gui.hybrid.SComboBox;
import spirite.gui.hybrid.SPanel;
import spirite.gui.hybrid.SScrollPane;
import spirite.hybrid.Globals;
import spirite.hybrid.HybridHelper;
import spirite.pc.ui.ContextMenus;
import spirite.pc.ui.UIUtil;
import spirite.pc.ui.dialogs.Dialogs;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

public class PalettePanel extends SPanel 
        implements MouseListener, MPaletteObserver, ActionListener
{
	private final MasterControl master;
    private final PaletteManager paletteManager;
    private final Dialogs dialogs;
    
	private static final long serialVersionUID = 1L;
	private final static int BIG_SIZE = 20;
    private final static int SMALL_SIZE = 12;

    private ColorPicker main, sub;
    private SScrollPane container;
    private PaletteSubpanel palette;
    private SButton btnCopyPalette;
    private SButton btnPastePalette;
    private SButton btnSavePalette;
    private SButton btnLoadPalette;
    private SButton btnAddPalette;
    private SComboBox<String> boxPalette;
    private SButton btnRemovePalette;
    
    private Palette wPalette;
    
    DataBinding<Integer> selectedBinding = new DataBinding<>();

    public PalettePanel( MasterControl _master) {
    	this.master = _master;
    	this.dialogs = master.getDialogs();
        paletteManager = master.getPaletteManager();

        initComponents();
        initBindings();
        palette.addMouseListener(this);
        sub.addMouseListener(this);

        paletteManager.addPaletteObserver(this);
        wPalette = paletteManager.getCurrentPalette();
        
        SwingUtilities.invokeLater(() -> colorChanged());
    }

    // Set up the GUI
    void initComponents() {
                GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);

        main = new ColorPicker( 0);
        main.addMouseListener(this);

        sub = new ColorPicker( 1);

        palette = new PaletteSubpanel();

        container = new SScrollPane(palette, SScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, SScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        container.getHorizontalScrollBar().setPreferredSize(new Dimension(5, 0));

        btnSavePalette = new SButton();
        btnLoadPalette = new SButton();
        btnAddPalette = new SButton();
        btnRemovePalette = new SButton();
        btnCopyPalette = new SButton();
        btnPastePalette = new SButton();
        btnCopyPalette.setToolTipText("Copy Palette");
        btnPastePalette.setToolTipText("Paste Palette");
        btnSavePalette.setToolTipText("Save Palette");
        btnLoadPalette.setToolTipText("Load Palette");
        btnAddPalette.setToolTipText("Add NewPalette");
        btnRemovePalette.setToolTipText("Remove Palette");
        boxPalette = new SComboBox<String>(new String[] {"Default", "2"});
        
        btnSavePalette.addActionListener( this);
        btnLoadPalette.addActionListener( this);

        btnSavePalette.setIcon(Globals.getIcon("palSavePalette"));
        btnLoadPalette.setIcon(Globals.getIcon("palLoadPalette"));
        btnAddPalette.setIcon(Globals.getIcon("palNewColor"));
        //btnRemovePalette.setIcon(Globals.getIcon("palRemPalette"));
        
        Dimension bsize = new Dimension(24, 12);
        int ddheight = 16;
        

        layout.setHorizontalGroup(layout.createParallelGroup( GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(10)
                .addComponent(main, BIG_SIZE,BIG_SIZE,BIG_SIZE)
            )
            .addGroup(layout.createSequentialGroup()
                .addGap(20)
                .addComponent(sub, BIG_SIZE,BIG_SIZE,BIG_SIZE)
            )
            .addGroup( layout.createSequentialGroup()
                .addGap(20 + BIG_SIZE + 5)
                .addGroup(layout.createParallelGroup( GroupLayout.Alignment.TRAILING)
	            	.addGroup(layout.createSequentialGroup()
	                    .addComponent(container)
	                )
	            )
            )
        	.addGroup(layout.createSequentialGroup()
            		.addComponent(boxPalette)
            		.addGap(1)
            		.addComponent(btnRemovePalette, bsize.width, bsize.width, bsize.width))
            	.addGroup(layout.createSequentialGroup()
            		.addComponent(btnCopyPalette, bsize.width, bsize.width, bsize.width)
            		.addGap(1)
            		.addComponent(btnPastePalette, bsize.width, bsize.width, bsize.width)
            		.addGap(1)
            		.addComponent(btnSavePalette, bsize.width, bsize.width, bsize.width)
            		.addGap(1)
            		.addComponent(btnLoadPalette, bsize.width, bsize.width, bsize.width)
            		.addGap(1)
            		.addComponent(btnAddPalette, bsize.width, bsize.width, bsize.width)
            	)

        );
        layout.setVerticalGroup(layout.createParallelGroup( GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                        .addGap(10)
                        .addComponent(main, BIG_SIZE,BIG_SIZE,BIG_SIZE)
                )
                .addGroup(layout.createSequentialGroup()
                        .addGap(20)
                        .addComponent(sub, BIG_SIZE,BIG_SIZE,BIG_SIZE)
                )
                .addGroup( layout.createSequentialGroup()
               		.addComponent(container)
               		.addGroup(layout.createParallelGroup()
               			.addComponent(boxPalette, ddheight,ddheight,ddheight)
               			.addComponent(btnRemovePalette, ddheight,ddheight,ddheight))
               		.addGroup( layout.createParallelGroup()
               			.addComponent(btnCopyPalette, bsize.height, bsize.height, bsize.height)
               			.addComponent(btnPastePalette, bsize.height, bsize.height, bsize.height)
               			.addComponent(btnSavePalette, bsize.height, bsize.height, bsize.height)
               			.addComponent(btnLoadPalette, bsize.height, bsize.height, bsize.height)
               			.addComponent(btnAddPalette, bsize.height, bsize.height, bsize.height)
               		)
                )
        );
    }
    
    byte[] copyPalette = null;
    String copyName = null;
    private void initBindings() {
        btnRemovePalette.addActionListener((e) -> {
        	ImageWorkspace ws = master.getCurrentWorkspace();
        	if( ws == null) return;
        	
        	if( JOptionPane.showConfirmDialog(this, "Delete Palette") == JOptionPane.YES_OPTION)
        		ws.getPaletteSet().removePalette(ws.getPaletteSet().getPalettes().indexOf(wPalette));
        });
        btnAddPalette.addActionListener((e) -> {
        	ImageWorkspace ws = master.getCurrentWorkspace();
        	if( ws == null) return;
        	
        	String newName = JOptionPane.showInputDialog(this,"New Palette", getNonDuplicateName("palette_1"));
        	if( newName != null) 
        		ws.getPaletteSet().addPalette(paletteManager.new Palette(newName), true);
        });
        btnCopyPalette.addActionListener((e) -> {
        	copyPalette = wPalette.compress();
        	copyName = wPalette.getName();
        });
        btnPastePalette.addActionListener((e) -> {
        	ImageWorkspace ws = master.getCurrentWorkspace();
        	if( ws == null) return;
        	
        	if( copyPalette != null && ws != null) {
        		ws.getPaletteSet().addPalette(paletteManager.new Palette(copyPalette, getNonDuplicateName(copyName)), true);
        	}else HybridHelper.beep();
        });
        
        
        
        // boxPalette
        boxPalette.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if( e.getStateChange() == ItemEvent.SELECTED)
	        		selectedBinding.triggerUIChanged(boxPalette.getSelectedIndex());
				
				wPalette = paletteManager.getCurrentPalette();
		        repaint();
		        palette.repaint();
			}
		});
        selectedBinding.setLink(new ChangeExecuter<Integer>() {
			@Override
			public void doUIChanged(Integer newValue) {
				ImageWorkspace ws = master.getCurrentWorkspace();
				if( ws != null) {
					ws.getPaletteSet().setSelectedPalette(newValue);
				}
			}
			@Override
			public void doDataChanged(Integer newValue) {
				doUpdate();
			}
		});
        boxPalette.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "rename");
        boxPalette.getActionMap().put("rename", UIUtil.buildAction((e) -> {
        	String name = JOptionPane.showInputDialog(null, "Rename Palette", wPalette.getName());
        	if( name != null) {
        		wPalette.setName(getNonDuplicateName(name));
        	}
        }));
    }
    
    private String getNonDuplicateName(String name) {
    	List<String> names = new ArrayList<>(boxPalette.getItemCount());
    	for(int i=0; i < boxPalette.getItemCount(); ++i)
    		names.add( boxPalette.getItemAt(i));
    	
    	return MUtil.getNonDuplicateName(names, name);
    }

    // :: MPaletteObserver
    @Override
    public void colorChanged() {

        selectedBinding.triggerDataChanged(null);

    }
    private void doUpdate() {
        main.setBackground( new Color(paletteManager.getActiveColor(0)));
        sub.setBackground( new Color(paletteManager.getActiveColor(1)));
        
        wPalette = paletteManager.getCurrentPalette();
        
        boxPalette.removeAllItems();
        ImageWorkspace ws = master.getCurrentWorkspace();
        if( ws != null) {
        	List<Palette> palettes = ws.getPaletteSet().getPalettes();
            for( Palette palette : palettes)
            	boxPalette.addItem(palette.getName());
            
            boxPalette.setSelectedIndex(palettes.indexOf(wPalette));
        }
        
        repaint();
        palette.repaint();
    }

    // :: MouseEventListener
    int startX, startY;
    long startTime;
    int currentlyOver = -1;
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
    @Override    public void mouseClicked(MouseEvent e) {}
    @Override public void mousePressed(MouseEvent e) {
        startX = e.getX();
        startY = e.getY();
        startTime = System.currentTimeMillis();
    }
    @Override 
    public void mouseReleased(MouseEvent e) {
    	boolean shortEvt = ( System.currentTimeMillis() - startTime < 200);
    	
    	Component draggedFrom = e.getComponent();
    	if( draggedFrom.contains(e.getPoint())) {
    		if( draggedFrom instanceof ColorPicker) {
    			Color c = dialogs.pickColor();
    			
    			if( c != null) {
    				int argb = c.getRGB();
    				paletteManager.setActiveColor(((ColorPicker)draggedFrom).index, argb);
    				if( !wPalette.getColors().contains(argb))
    					wPalette.addPaletteColor(argb);
    			}
    		}
    		else if( draggedFrom == palette) {
    			// A mouse drag from inside the PaletteSubpanel to itself can either
    			// start and end on 
    			int startIndex = palette.getIndexAt(startX, startY);
    			int endIndex = palette.getIndexAt(e.getX(), e.getY());

    			Integer startC = wPalette.getPaletteColor(startIndex);
    			
    			if( startIndex == -1 || endIndex == -1){}
    			else if( startIndex == endIndex || shortEvt) {
    				if( e.getClickCount() == 2 || startC == null) {
    					// Color Pick new Palette Color
    					Color c = dialogs.pickColor( (startC == null)?null:new Color(startC));
    					if( c != null)  {
    						wPalette.setPaletteColor(startIndex, c.getRGB());
        					paletteManager.setActiveColor(e.getButton()/2, c.getRGB());
    					}
    				}
    				else
    					paletteManager.setActiveColor(e.getButton()/2, wPalette.getPaletteColor(startIndex));
    			}
    			else {
    				// Move color from one place to another
    				wPalette.setPaletteColor(endIndex, startC);
    			}
    		}
    	}
    	else {
    		// You have Dragged from one component to another.
    		Component draggedTo = null;
    		if( main.contains(SwingUtilities.convertPoint(draggedFrom, e.getPoint(), main)))
    			draggedTo = main;
    		else if(sub.contains(SwingUtilities.convertPoint(draggedFrom, e.getPoint(), sub)))
    			draggedTo = sub;
    		else if (palette.contains(SwingUtilities.convertPoint(draggedFrom, e.getPoint(), palette)))
    			draggedTo = palette;
    		
    		if( draggedTo instanceof ColorPicker) {
    			// Drag from palette into the Foreground/Background: set FG/BG color as dragged color
    			if( draggedFrom == palette) {
        			int index = palette.getIndexAt(startX, startY);
        			Integer c = wPalette.getPaletteColor(index);
        			
        			if( c != null)
        				paletteManager.setActiveColor( ((ColorPicker)draggedTo).index, c);
    			}
    		}
    		else if( draggedTo == palette) {
    			// Drag from ColorPicker to palette: copy the color
    			Point converted = SwingUtilities.convertPoint( draggedFrom, e.getPoint(), draggedTo);
    			int index = palette.getIndexAt(converted.x, converted.y);
    			Color c = ((ColorPicker)draggedFrom).getColor();
    			if( index == -1)
    				wPalette.addPaletteColor(c.getRGB());
    			else
    				wPalette.setPaletteColor( index, c.getRGB());
    		}
    		else {
    			// Drag from X to out of the context
    			if( draggedFrom == palette) {
    				// Drag a Palette color out of context: remove it
        			int index = palette.getIndexAt(startX, startY);
    				if( index != -1)
    					wPalette.removePaletteColor(index);
    			}
    		}
    	}
    }

    /** Panels for the Foreground and Background colors */
    class ColorPicker extends SPanel {
		private static final long serialVersionUID = 1L;
		private final int index;

        ColorPicker( int index) {
            this.index = index;
            this.setBorder( new EtchedBorder(EtchedBorder.LOWERED));
            this.setPreferredSize( new Dimension( 24,24));
            this.setBackground( new Color(paletteManager.getActiveColor(index)));
        }
        
        public Color getColor() {
        	return getBackground();
        }
    }

    /** Panel that draws and handles all other Palette colors. */
    class PaletteSubpanel extends SPanel {
		private static final long serialVersionUID = 1L;

		public PaletteSubpanel() {
            this.setOpaque(false);
        }

        public void repaintIndex( int index) {
            this.repaint( getBoundsOfIndex(index));
        }

        public int getIndexAt( int x, int y) {
            int w = Math.max(1, this.getWidth() / SMALL_SIZE);

            if( x > SMALL_SIZE * w) return -1;
            return x/SMALL_SIZE + (y/SMALL_SIZE)*w;
        }
        
        public Rectangle getBoundsOfIndex( int i) {
        	if( i < 0)
        		return null;
        	
        	int w = Math.max(1, this.getWidth() / SMALL_SIZE);
        	int x = SMALL_SIZE * (i % w);
        	int y = SMALL_SIZE * (i / w);
        	
        	return new Rectangle( x, y, SMALL_SIZE, SMALL_SIZE);
        }

        @Override
        public void paintComponent( Graphics g) {
        	super.paintComponent(g);
            int selected1 = paletteManager.getActiveColor(0);
            int selected2 = paletteManager.getActiveColor(1);
            
            for( Entry<Integer, Integer> entry : wPalette.getPalette()) {
            	int c = entry.getValue();
            	Rectangle rect = getBoundsOfIndex( entry.getKey());
            	
            	g.setColor( new Color(c));
            	g.fillRect( rect.x, rect.y, rect.width, rect.height);
            	

                if( c == selected2) {
                    g.setColor( Color.gray);
                	g.drawRect( rect.x, rect.y, rect.width, rect.height);
                }
                if( c == selected1) {
                    g.setColor( Color.black);
                	g.drawRect( rect.x, rect.y, rect.width, rect.height);
                }
            }
        }
    }

	@Override
	public void actionPerformed(ActionEvent evt) {
		if( evt.getSource() == btnAddPalette) {
			wPalette.addPaletteColor( paletteManager.getActiveColor(0));
		}
		else if( evt.getSource() == btnLoadPalette) {
			List<String> palettes = paletteManager.getStoredPaletteNames();
			
			String[][] menuScheme = new String[palettes.size()+1][];
			menuScheme[0] = new String[] {"Load Default Palette", "loaddefault"};
			for( int i=0; i<palettes.size(); ++i) {
				menuScheme[i+1] = new String[2];
				menuScheme[i+1][0] = palettes.get(i);
				menuScheme[i+1][1] = "load." + palettes.get(i);
			}
			
			contextMenu.removeAll();
			ContextMenus.constructMenu(contextMenu, menuScheme, this);
			contextMenu.show(this, btnLoadPalette.getX() + 24, btnLoadPalette.getY() - 10);
		}
		else if( evt.getSource() == btnSavePalette) {

			List<String> palettes = paletteManager.getStoredPaletteNames();
			
			String[][] menuScheme = new String[palettes.size()+2][];
			for( int i=0; i<palettes.size(); ++i) {
				menuScheme[i] = new String[2];
				menuScheme[i][0] = palettes.get(i);
				menuScheme[i][1] = "save." + palettes.get(i);
			}
			menuScheme[palettes.size()] = new String[]{"-"};
			menuScheme[palettes.size()+1] = new String[]{"Save as New Palette", "savenew"};
			
			contextMenu.removeAll();
			ContextMenus.constructMenu(contextMenu, menuScheme, this);
			contextMenu.show(this, btnLoadPalette.getX() + 24, btnLoadPalette.getY() - 10);
		}
		else if(evt.getActionCommand() != null) {
			String cmd = evt.getActionCommand();
			if( cmd.startsWith("load.")) {
				String name = cmd.substring("load.".length());
				ImageWorkspace ws = master.getCurrentWorkspace();
				if( ws != null) {
					byte[] raw = master.getSettingsManager().getRawPalette(name);
					ws.getPaletteSet().addPalette( paletteManager.new Palette(raw, getNonDuplicateName(name)), true);
				}
			}
			if( cmd.startsWith("save.")) {
				String name = cmd.substring("save.".length());
				if( JOptionPane.showConfirmDialog(null, "Overwrite " + name +"?") == JOptionPane.YES_OPTION) {
					master.getSettingsManager().saveRawPalette(name, wPalette.compress());
				}
			}
			if( cmd.equals("savenew")) {
				String name = JOptionPane.showInputDialog("Save Palette As:");
				
				if( name == null)
					JOptionPane.showMessageDialog(null, "Cannot save as null name.");
				else {
					List<String> names = paletteManager.getStoredPaletteNames();
					
					if( !names.contains(name)
						|| JOptionPane.showConfirmDialog(null, "Overwrite " + name +"?") == JOptionPane.YES_OPTION) 
					master.getSettingsManager().saveRawPalette(name, wPalette.compress());
				}
			}
			if( cmd.equals("loaddefault")) {
				ImageWorkspace ws = master.getCurrentWorkspace();
				if( ws != null) {
					ws.getPaletteSet().addPalette(paletteManager.new Palette("default"), true);
				}
			}
		}
	}
	
	private final JPopupMenu contextMenu = new JPopupMenu();
	
}
