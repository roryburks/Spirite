package spirite.base.brains.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import spirite.base.brains.MasterControl;
import spirite.base.brains.MasterControl.CommandExecuter;
import spirite.base.brains.ToolsetManager.Tool;
import spirite.base.brains.ToolsetManager.ToolSettings;
import spirite.base.image_data.GroupTree.LayerNode;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.ReferenceManager;
import spirite.base.image_data.animations.FixedFrameAnimation.AnimationLayer.Frame;
import spirite.base.image_data.layers.Layer;
import spirite.base.image_data.mediums.IMedium.InternalImageTypes;
import spirite.base.image_data.mediums.drawer.IImageDrawer;
import spirite.base.image_data.mediums.drawer.IImageDrawer.IClearModule;
import spirite.base.image_data.mediums.drawer.IImageDrawer.IInvertModule;
import spirite.base.image_data.mediums.drawer.IImageDrawer.ITransformModule;
import spirite.base.image_data.selection.SelectionEngine;
import spirite.base.image_data.selection.SelectionMask;
import spirite.base.pen.Penner;
import spirite.base.util.glmath.MatTrans;
import spirite.base.util.glmath.Rect;
import spirite.base.util.glmath.Vec2;
import spirite.hybrid.HybridHelper;
import spirite.hybrid.HybridUtil;
import spirite.hybrid.HybridUtil.UnsupportedImageTypeException;

/** 
 * draw.* Command Executer
 * 
 * These are commands that make direct and immediate changes to the current
 * ImageWorkspace's image data (usually the active data)
 * */
public class RelativeWorkspaceCommandExecuter implements CommandExecuter {
	private final MasterControl master;
	private final Map<String, Runnable> commandMap = new HashMap<>();
	private ImageWorkspace workspace;
	
	@Override public String getCommandDomain() {return "draw";}
	
	public RelativeWorkspaceCommandExecuter(MasterControl master) {
		this.master = master;
		
		commandMap.put("undo", ()  -> {workspace.getUndoEngine().undo();});
		commandMap.put("redo", () -> {workspace.getUndoEngine().redo();});
		commandMap.put("shiftRight", () -> {
			IImageDrawer drawer = workspace.getActiveDrawer();
			if( drawer instanceof ITransformModule )
				((ITransformModule)drawer).transform(MatTrans.TranslationMatrix(1, 0));
		});
		commandMap.put("shiftLeft", () -> {
			IImageDrawer drawer = workspace.getActiveDrawer();
			if( drawer instanceof ITransformModule )
				((ITransformModule)drawer).transform(MatTrans.TranslationMatrix(-1, 0));
		});
		commandMap.put("shiftDown", () -> {
			IImageDrawer drawer = workspace.getActiveDrawer();
			if( drawer instanceof ITransformModule ) 
				((ITransformModule)drawer).transform(MatTrans.TranslationMatrix(0, 1));
		});
		commandMap.put("shiftUp", () -> {
			IImageDrawer drawer = workspace.getActiveDrawer();
			if( drawer instanceof ITransformModule )
				((ITransformModule)drawer).transform(MatTrans.TranslationMatrix(0, -1));
		});
		commandMap.put("newLayerQuick", () -> {
			workspace.addNewSimpleLayer(workspace.getSelectedNode(), 
					workspace.getWidth(), workspace.getHeight(), 
					"New Layer", 0x00000000, InternalImageTypes.DYNAMIC);
		});
		commandMap.put("clearLayer", () -> {
			
			if( workspace.getSelectionEngine().isLifted()) {
				workspace.getSelectionEngine().clearLifted();
			}
			else {
				IImageDrawer drawer = workspace.getActiveDrawer();
				if( drawer instanceof IClearModule )
					((IClearModule) drawer).clear();
				else
					HybridHelper.beep();
			}
		});
		commandMap.put("cropSelection", () -> {
			Node node = workspace.getSelectedNode();
			SelectionEngine selectionEngine = workspace.getSelectionEngine();
			
			SelectionMask selection = selectionEngine.getSelection();
			if( selection == null) {
				HybridHelper.beep();
				return;
			}
			
			Rect rect = new Rect(selection.getDimension());
			rect.x = selection.getOX();
			rect.y = selection.getOY();
			
			workspace.cropNode(node, rect, false);
		});
		commandMap.put("autocroplayer", () -> {
			Node node = workspace.getSelectedNode();
			
			if( node instanceof LayerNode) {
				Layer layer = ((LayerNode)node).getLayer();

				try {
					Rect rect;
					rect = HybridUtil.findContentBounds(
							layer.getActiveData().handle.deepAccess(),
							1, 
							false);
					rect.x += node.getOffsetX();
					rect.y += node.getOffsetY();
					workspace.cropNode((LayerNode) node, rect, true);
				} catch (UnsupportedImageTypeException e) {
					e.printStackTrace();
				}
			}
		});
		commandMap.put("layerToImageSize", () -> {
			Node node = workspace.getSelectedNode();
			
			if( node != null)
				workspace.cropNode(node, new Rect(0,0,workspace.getWidth(), workspace.getHeight()), false);
		});
		commandMap.put("invert", () -> {
			IImageDrawer drawer = workspace.getActiveDrawer();
			
			if( drawer instanceof IInvertModule) 
				((IInvertModule) drawer).invert();
			else
				HybridHelper.beep();
		});
		commandMap.put("applyTransform", () -> {
			ToolSettings settings = master.getToolsetManager().getToolSettings( Tool.RESHAPER);
			if( workspace.getSelectionEngine().isProposingTransform()) 
				workspace.getSelectionEngine().applyProposedTransform();
			else {
				Vec2 scale = (Vec2)settings.getValue("scale");
				Vec2 translation = (Vec2)settings.getValue("translation");
				float rotation = (float)settings.getValue("rotation");

				MatTrans trans = new MatTrans();
				trans.preScale(scale.x, scale.y);
				trans.preRotate((float)(rotation * 180.0f /(Math.PI)));
				trans.preTranslate(translation.x, translation.y);
				workspace.getSelectionEngine().transformSelection(trans);
				
			}

			settings.setValue("scale", new Vec2(1,1));
			settings.setValue("translation", new Vec2(0,0));
			settings.setValue("rotation", 0f);
			
			Penner p = master.getFrameManager().getPenner();
			if( p != null)
				p.cleanseState();
		});
		commandMap.put("toggle_reference", () -> {
				ReferenceManager rm = workspace.getReferenceManager();
				rm.setEditingReference(!rm.isEditingReference());
		});
		commandMap.put("reset_reference", () -> {
				ReferenceManager rm = workspace.getReferenceManager();
				rm.resetTransform();
		});
		commandMap.put("lift_to_reference", () -> {
			SelectionEngine se = workspace.getSelectionEngine();
			ReferenceManager rm = workspace.getReferenceManager();
			
			if( se.isLifted()) {
    			rm.addReference(se.getLiftedData().readonlyAccess(), rm.getCenter(), se.getLiftedDrawTrans());
    			se.clearLifted();
			}
		});
		

		commandMap.put("addGapQuick", () -> {
			Frame frame = workspace.getAnimationManager().getSelectedFrame();
			if( frame != null) {
				frame.setGapAfter(frame.getGapAfter()+1);
			}
		});
	}

	@Override public List<String> getValidCommands() {
		return new ArrayList<>(commandMap.keySet());
	}

	@Override
	public boolean executeCommand(String command, Object extra) {
		Runnable runnable = commandMap.get(command);
		
		if( runnable != null) {
			workspace = master.getCurrentWorkspace();
			if( workspace != null) {
				runnable.run();
			}
			return true;
		}
		else
			return false;
	}
}