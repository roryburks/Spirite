package spirite.base.graphics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

import spirite.base.brains.RenderEngine;
import spirite.base.brains.RenderEngine.NodeRenderer;
import spirite.base.brains.RenderEngine.RenderSettings;
import spirite.base.brains.RenderEngine.TransformedHandle;
import spirite.base.image_data.Animation;
import spirite.base.image_data.ImageHandle;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.RawImage;
import spirite.base.image_data.AnimationManager.AnimationState;
import spirite.base.image_data.GroupTree.AnimationNode;
import spirite.base.image_data.GroupTree.GroupNode;
import spirite.base.image_data.GroupTree.LayerNode;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.ImageWorkspace.BuiltImageData;
import spirite.base.util.glmath.MatTrans;
import spirite.hybrid.HybridHelper;
import spirite.hybrid.MDebug;
import spirite.hybrid.MDebug.ErrorType;

/***
 * 
 * 
 * !!! Note: 
 */
public class HybridNodeRenderer extends NodeRenderer{
	
	private final ImageWorkspace workspace;
	
	private RawImage buffer[];
	private float ratioW;
	private float ratioH;

	public HybridNodeRenderer(RenderEngine renderEngine, GroupNode root) {
		renderEngine.super(root);
		this.workspace = root.getContext();
	}

	@Override
	public void render(RenderSettings settings, GraphicsContext gc, MatTrans trans) {
		try {
			buildCompositeLayer(workspace);
			
			// Step 1: Determine the amount of data needed
			int n = _getNeededImagers( settings);
			if( n <= 0) return;
			
			buffer = new RawImage[n];
			for( int i=0; i<n; ++i) {
				buffer[i] = HybridHelper.createImage(settings.width, settings.height);
				buffer[i].getGraphics().clear();
			}
			
			// Step 2: Recursively draw the image
			ratioW = settings.width / (float)workspace.getWidth();
			ratioH = settings.height / (float)workspace.getHeight();
			
			_render_rec( root, 0, settings);
			
			gc.drawImage(buffer[0], 0, 0);
			
			// Flush the data
			for( int i=0; i<n; ++i)
				buffer[i].flush();
			gc.dispose();
		}
		finally {
			buffer = null;
			clearCompositeImage();
		}
	}

	private RawImage compositionImage;
	private ImageHandle compositionHandle = null;
	private void buildCompositeLayer(ImageWorkspace workspace) {
		BuiltImageData dataContext= workspace.buildActiveData();
		if( dataContext != null && (workspace.getSelectionEngine().getLiftedImage() != null 
				||  workspace.getDrawEngine().strokeIsDrawing())) {
			compositionImage= 
					HybridHelper.createImage(dataContext.getWidth(), dataContext.getHeight());
			compositionHandle = dataContext.handle;

			GraphicsContext gc = compositionImage.getGraphics();
			
			// Draw the Base Image
			gc.setTransform(dataContext.getCompositeTransform());
			gc.translate(dataContext.handle.getDynamicX(), 
					dataContext.handle.getDynamicY());
			gc.drawHandle( dataContext.handle, 0, 0);
			

			if( workspace.getSelectionEngine().getLiftedImage() != null ){
				// Draw Lifted Image
				MatTrans tt = dataContext.getScreenToImageTransform();
				tt.concatenate( workspace.getSelectionEngine().getDrawFromTransform());
				
				gc.setTransform(tt);
				gc.drawImage( workspace.getSelectionEngine().getLiftedImage(), 0, 0);
			}
			if( workspace.getDrawEngine().strokeIsDrawing()) {
				// Draw
				gc.setTransform(new MatTrans());
				workspace.getDrawEngine().getStrokeEngine().drawStrokeLayer( gc);
			}
//			g2.dispose();
		}
	
	}

	private void clearCompositeImage() {
		if( compositionImage != null)
			compositionImage.flush();
		compositionHandle = null;
		compositionImage = null;
	}

	private void _render_rec(
			GroupNode node, 
			int n, 
			RenderSettings settings) 
	{
		if( n < 0 || n >= buffer.length) {
			MDebug.handleError(ErrorType.STRUCTURAL, "Error: propperRender exceeds expected image need.");
			return;
		}
		

		// Go through the node's children (in reverse), drawing any visible group
		//	found recursively and drawing any Layer found plainly.
		
		ListIterator<Node> it = node.getChildren().listIterator(node.getChildren().size());
		List< Drawable> renderList = new ArrayList<>();
		while( it.hasPrevious()) {
			Node child = it.previous();
			if( child.getRender().isVisible()) {
				if( child instanceof GroupNode) {
					if( n == buffer.length-1) {
						// Note: the code can reach here if all the children are invisible.
						// There might be other, unintended ways for the code to reach here.
						continue;
					}
					
					Drawable renderable;
					renderable = new GroupRenderable(
							(GroupNode)child, n, settings);
					renderable.draw(buffer[n].getGraphics());
				}
				else if( child instanceof LayerNode) {
					// Step 1: Construct a list of all components that need to be rendered
					int count = 0;	// This subDepth counter is used to make sure Renderables of
									// the same depth are rendered in the correct order.
					List<TransformedHandle> sub = ((LayerNode)child).getLayer().getDrawList();
					
					for( TransformedHandle subRend : sub) {
						Drawable renderable = new TransformedRenderable( 
								child.getRender(), subRend, settings, child.getOffsetX(), child.getOffsetY());
						renderable.subDepth = count++;
						renderList.add(renderable );
					}
					
					// Step 2: Sort the list by depth then subdepth, increasing.
					renderList.sort( new Comparator<Drawable>() {
						@Override
						public int compare(Drawable o1, Drawable o2) {
							if( o1.depth == o2.depth)
								return o1.subDepth - o2.subDepth;
							return o1.depth - o2.depth;
						}
					});
					
					// Step 3: Draw each one (note: GroupRenderables will recursively call _propperRec
					for( Drawable renderable : renderList) {
						renderable.draw(buffer[n].getGraphics());
					}
				}
				else if( child instanceof AnimationNode) {
					Animation anim = ((AnimationNode)child).getAnimation();
					AnimationState as = workspace.getAnimationManager().getAnimationState(anim);
					List<List<TransformedHandle>> table = anim.getDrawTable(as.getSelectedMetronome(), as);
					
					for( List<TransformedHandle> list : table) {
						for( TransformedHandle th : list) {
							(new TransformedRenderable( new RenderProperties(), th, settings, 0, 0)).draw(buffer[n].getGraphics());
						}
					}
				}
			}
		}
	}

	private abstract class Drawable {
		private int subDepth;
		protected int depth;
		public abstract void draw(GraphicsContext gc);
	}
	

	private class GroupRenderable extends Drawable {
		private final GroupNode node;
		private final int n;
		private final RenderSettings settings;
		GroupRenderable( GroupNode node, int n, RenderSettings settings) 
		{
			this.node = node;
			this.n = n;
			this.settings = settings;
		}
		@Override
		public void draw(GraphicsContext gc) {
			buffer[n+1].getGraphics().clear();	// Not pressent in AWTNodeRenderer
			
			_render_rec(node, n+1, settings);
			gc.renderImage( buffer[n+1], 0, 0, node.getRender());
		}
	}

	private class TransformedRenderable extends Drawable {
		private final TransformedHandle renderable;
		private final RenderSettings settings;
		private final RenderProperties properties;
		private final MatTrans transform;
		
		TransformedRenderable( RenderProperties properties, TransformedHandle renderable, RenderSettings settings, int ox, int oy) {
			//this.node = node;
			this.properties = new RenderProperties(properties);
			this.properties.alpha *= renderable.alpha;
			this.renderable = renderable;
			this.depth = renderable.depth;
			this.settings = settings;
			this.transform = renderable.trans;
			this.transform.translate(ox, oy);
		}
		@Override
		public void draw(GraphicsContext gc) {
			MatTrans oldTansform = gc.getTransform();
			
			MatTrans drawTrans = new MatTrans( transform);
			if( compositionHandle == renderable.handle) {
				if( renderable.handle.isDynamic())
					drawTrans = new MatTrans();
				gc.setTransform(drawTrans);
				gc.renderImage( compositionImage, 0, 0, properties);
			}
			else {
				drawTrans.translate( renderable.handle.getDynamicX(), 
						renderable.handle.getDynamicY());
				gc.setTransform(drawTrans);
				gc.renderHandle(renderable.handle, 0, 0, properties);
			}
			
			//gc.renderImage( renderable.handle.deepAccess(), x, y, render);
			gc.setTransform(oldTansform);
			
		}
	}
}
