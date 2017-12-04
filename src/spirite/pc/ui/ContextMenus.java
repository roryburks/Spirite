package spirite.pc.ui;

import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import spirite.base.brains.MasterControl;
import spirite.base.image_data.Animation;
import spirite.base.image_data.GroupTree.AnimationNode;
import spirite.base.image_data.GroupTree.GroupNode;
import spirite.base.image_data.GroupTree.LayerNode;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.animations.ffa.FixedFrameAnimation;
import spirite.base.image_data.layers.Layer;
import spirite.base.image_data.layers.SpriteLayer;
import spirite.base.image_data.layers.puppet.PuppetLayer;
import spirite.hybrid.Globals;
import spirite.hybrid.MDebug;
import spirite.hybrid.MDebug.WarningType;

public class ContextMenus {
	public final static JPopupMenu cmenu = new JPopupMenu();
	private final MasterControl master;
	
	public ContextMenus( MasterControl master) {
		this.master = master;
	}
	
	public void doContextMenu(MouseEvent evt, Object object) {
		ImageWorkspace workspace = master.getCurrentWorkspace();
		if( workspace == null) return;
		
		cmenu.removeAll();
		
		String[][] menuScheme = null;
		
		if( object instanceof Animation) 
			menuScheme = constructSchemeForAnimation(workspace, (Animation)object);
		else if( object instanceof Node)
			menuScheme = constructSchemeForNode(workspace, (Node)object);
		
		constructMenu(cmenu, menuScheme, 
				(e) -> master.executeCommandString(e.getActionCommand(), object));
		
		cmenu.show(evt.getComponent(), evt.getX(), evt.getY());
	}
	
	public void doContextMenu(MouseEvent evt, Node node) {
		ImageWorkspace workspace = master.getCurrentWorkspace();
		if( workspace == null) return;
		
		cmenu.removeAll();
		
		constructMenu(
				cmenu, 
				constructSchemeForNode(workspace, node), 
				(e) -> master.executeCommandString(e.getActionCommand(), node));
		
		cmenu.show(evt.getComponent(), evt.getX(), evt.getY());
	
	}
	public static String[][] constructSchemeForAnimation(ImageWorkspace workspace, Animation anim) {

		List<String[]> menuScheme = new ArrayList<>(
			Arrays.asList(new String[][] {
				{"-"},
			})
		);
		if( anim != null)
			menuScheme.add(new String[] {"&Delete Animation", "animation.delete", null});
		
		return menuScheme.toArray(new String[0][]);
	}
	
	public static String[][] constructSchemeForNode(ImageWorkspace workspace, Node node) {
		if( workspace == null)
			return null;
		
		if( node instanceof AnimationNode)
			return constructSchemeForAnimation(workspace, ((AnimationNode) node).getAnimation());

		List<String[]> menuScheme = new ArrayList<>(
			Arrays.asList(new String[][] {
				{"&New..."},
				{".New Simple &Layer", "node.newLayer", "new_layer"},
				{".New Layer &Group", "node.newGroup", "new_group"},
				{".New &Rig Layer", "node.newRig", null},
				{".New &Puppet Layer", "node.newPuppet", null},
			})
		);
		
		if( node != null) {
			String descriptor = "...";
			if( node instanceof GroupNode)
				descriptor = "Layer Group";
			if( node instanceof LayerNode)
				descriptor = "Layer";
			

			// All-node related menu items
			menuScheme.addAll(Arrays.asList(new String[][] {
					{"-"},
					{"D&uplicate "+descriptor, "node.duplicate", null}, 
					{"&Delete  "+descriptor, "node.delete", null}, 
			}));

			// Add parts to the menu scheme depending on node type
			if( node instanceof GroupNode) {
				menuScheme.add( new String[] {"-"});
				menuScheme.add( new String[] {"&Construct Simple Animation From Group", "node.animfromgroup", null});
				if( workspace.getAnimationManager().getSelectedAnimation() instanceof FixedFrameAnimation)
					menuScheme.add( new String[]{"&Add Group To Animation As New Layer", "node.animinsert", null});
				menuScheme.add( new String[] {"&Write Group To GIF Animation", "node.giffromgroup", null});
			}
			else if( node instanceof LayerNode) {
				menuScheme.add( new String[] {"-"});
				Layer layer = ((LayerNode) node).getLayer();
				if( layer.canMerge(((LayerNode)node).getNextNode())) {
					menuScheme.add( new String[]{"&Merge Layer Down", "node.mergeDown", null});
				}
				if( layer instanceof SpriteLayer) {
					menuScheme.add( new String[] {"Construct &Rig Animation From Sprite", "node.animFromRig",null});
				}
				if( layer instanceof PuppetLayer) {
					menuScheme.add( new String[] {"Add &Derived Puppet Layer", "node.newDerivedPuppet", null});
				}
			}
		}
		
		return menuScheme.toArray(new String[0][]);
	}

	/***
	 * Constructs a menu from an array of objects corresponding to the menu scheme as such:
	 * 
	 * The menuScheme is an n-by-3 array 
	 * -The first string represents the menu title as well as its structure (see below)
	 * -The second string represents the actionCommand
	 * -The third string represents the icon associated with it (using Globals.getIcon)
	 * 
	 * Each dot before the name indicates the level it should be in.  For example one dot
	 *   means it goes inside the last zero-dot item, two dots means it should go in the last
	 *   one-dot item, etc.  Note: if you skip a certain level of dot's (eg: going from
	 *   two dots to four dots), then the extra dots will be ignored, possibly resulting
	 *   in unexpected menu form.
	 * The & character before a letter represents the Mnemonic key that should be associated
	 *   with it.
	 * If the title is simply - (perhaps after some .'s representing its depth), then it is
	 *   will simply construct a separator and will ignore the last two elements in the
	 *   array (in fact they don't need to exist).
	 * @param root the Component (be it JPopupMenu or JMenuBar or other) to construct the menu into
	 * @param menuScheme See Above
	 * @param listener the listener which will be sent the Action when an item is selected
	 */
	public static void constructMenu( JComponent root, Object menuScheme[][], ActionListener listener) {
		if( menuScheme == null)
			return;
		
		JMenuItem new_node;
		JMenuItem[] active_root_tree = new JMenuItem[UIUtil.MAX_LEVEL];
		
		// If root is a JMenuBar, make sure that all top-level nodes are JMenu's
		//	instead of JMenuItems (otherwise they glitch out the bar)
		boolean isMenuBar = (root instanceof JMenuBar);
		boolean isPopupMenu = (root instanceof JPopupMenu);
		
		// Atempt to construct menu from parsed data in menu_scheme
		int active_level = 0;
		for( int i = 0; i < menuScheme.length; ++i) {
			if( menuScheme[i].length == 0 || !(menuScheme[i][0] instanceof String))
				continue;
			
			String title = (String)menuScheme[i][0];
			char mnemonic = '\0';
			
			// Determine the depth of the node and crop off the extra .'s
			int level =UIUtil._imCountLevel(title);
			title = title.substring(level);
			
			if( level > active_level ) {
				MDebug.handleWarning(WarningType.INITIALIZATION, null, "Bad Menu Scheme.");
				level = active_level;
			}
			active_level = level+1;
			
			// If it's - that means it's a separator
			if( title.equals("-")) {
				if( level == 0 ) {
					if( isPopupMenu)
						((JPopupMenu)root).addSeparator();
				}
				else
					((JMenu)active_root_tree[level-1]).addSeparator();
				
				active_level--;
				continue;
			}
			
			// Detect the Mnemonic
			int mind = title.indexOf('&');
			if( mind != -1 && mind != title.length()-1) {
				mnemonic = title.charAt(mind+1);
				title = title.substring(0, mind) + title.substring(mind+1);
			}
			
			
			// Determine if it needs to be a Menu (which contains other options nested in it)
			//	or a plain MenuItem (which doesn't)
			if( (level != 0 || !isMenuBar) && (i+1 == menuScheme.length || UIUtil._imCountLevel((String)menuScheme[i+1][0]) <= level)) {
				new_node = new JMenuItem( title);
			}
			else {
				new_node = new JMenu( title);
			}
			if( mnemonic != '\0') {
				new_node.setMnemonic(mnemonic);
			}
			
	
			if( menuScheme[i].length > 1 && menuScheme[i][1] instanceof String) {
				new_node.setActionCommand((String)menuScheme[i][1]);
				
				if( listener != null)
					new_node.addActionListener(  listener);
			}
			
			if( menuScheme[i].length > 2 && menuScheme[i][2] instanceof String)
				new_node.setIcon(Globals.getIcon((String)menuScheme[i][2]));
			
			// Add the MenuItem into the appropriate context
			if( level == 0) {
				root.add( new_node);
			}
			else {
				active_root_tree[level-1].add(new_node);
			}
			active_root_tree[ level] = new_node;
		}
	}

}
