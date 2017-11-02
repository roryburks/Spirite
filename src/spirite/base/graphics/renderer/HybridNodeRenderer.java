package spirite.base.graphics.renderer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.RawImage;
import spirite.base.graphics.RawImage.InvalidImageDimensionsExeption;
import spirite.base.graphics.RenderProperties;
import spirite.base.graphics.renderer.RenderEngine.RenderSettings;
import spirite.base.graphics.renderer.RenderEngine.TransformedHandle;
import spirite.base.image_data.Animation;
import spirite.base.image_data.AnimationManager.AnimationState;
import spirite.base.image_data.GroupTree.AnimationNode;
import spirite.base.image_data.GroupTree.GroupNode;
import spirite.base.image_data.GroupTree.LayerNode;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.GroupTree.NodeValidator;
import spirite.base.image_data.ImageHandle;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.ImageWorkspace.BuildingImageData;
import spirite.base.image_data.images.ABuiltImageData;
import spirite.base.pen.StrokeEngine;
import spirite.base.util.glmath.MatTrans;
import spirite.hybrid.HybridHelper;
import spirite.hybrid.MDebug;
import spirite.hybrid.MDebug.ErrorType;

/***
 * 
 * 
 * !!! Note: 
 */
public class HybridNodeRenderer {
	protected final GroupNode root;
	
	private final ImageWorkspace workspace;
	
	private RawImage buffer[];
	private float ratioW;
	private float ratioH;

	public HybridNodeRenderer(GroupNode root) {
		this.root= root;
		this.workspace = root.getContext();
	}

	public void render(RenderSettings settings, GraphicsContext gc, MatTrans trans) {
		try {
			buildCompositeLayer(workspace);
			
			// Step 1: Determine the amount of data needed
			int n = _getNeededImages( settings);
			if( n <= 0) return;
			
			buffer = new RawImage[n];
			for( int i=0; i<n; ++i) {
				buffer[i] = HybridHelper.createImageNonNillable(settings.width, settings.height);
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
		}catch( InvalidImageDimensionsExeption e) {}
		finally {
			buffer = null;
			clearCompositeImage();
		}
	}

	// ==== Composite Layer
	private RawImage compositionImage;
	private ImageHandle compositionHandle = null;
	private void buildCompositeLayer(ImageWorkspace workspace) throws InvalidImageDimensionsExeption 
	{
		BuildingImageData dataContext = workspace.buildActiveData();
		StrokeEngine strokeEngine = workspace.getAcrtiveStrokeEngine();
		if( dataContext != null && (workspace.getSelectionEngine().getLiftedImage() != null 
				||  (strokeEngine != null)))
		{
			dataContext.doOnBuiltData((built) -> {
				compositionImage= 
						HybridHelper.createImage(built.getWidth(), built.getHeight());
				compositionHandle = dataContext.handle;

				GraphicsContext gc = compositionImage.getGraphics();
				
				// Draw the Base Image
				MatTrans drawTrans = built.getCompositeTransform();
				drawTrans.preTranslate(dataContext.handle.getDynamicX(), 
						dataContext.handle.getDynamicY());
				gc.setTransform(drawTrans);
				
				dataContext.handle.drawBehindStroke(gc);
				

				if( workspace.getSelectionEngine().getLiftedImage() != null ){
					// Draw Lifted Image
					MatTrans tt = workspace.getSelectionEngine().getDrawFromTransform();
					tt.preConcatenate(built.getScreenToImageTransform() );
					
					gc.setTransform(tt);
					gc.drawImage( workspace.getSelectionEngine().getLiftedImage(), 0, 0);
				}
				if( strokeEngine != null) {
					// Draw the Stroke
					gc.setTransform(new MatTrans());
					strokeEngine.drawStrokeLayer(gc);
				}
				
				gc.setTransform(drawTrans);
				dataContext.handle.drawInFrontOfStroke(gc);
			});

		}
	
	}
	private void clearCompositeImage() {
		if( compositionImage != null)
			compositionImage.flush();
		compositionHandle = null;
		compositionImage = null;
	}

	// ==== Rendering
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
					List< Drawable> renderList = new ArrayList<>();
					
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
			this.renderable = renderable;
			this.depth = renderable.depth;
			this.settings = settings;
			this.transform = renderable.trans;
			this.transform.preTranslate(ox, oy);

			// Concatenate
			this.properties.alpha *= renderable.alpha;
			if( renderable.method!= null) {
				this.properties.method = renderable.method;
				this.properties.renderValue = renderable.renderValue;
			}
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
				drawTrans.preTranslate(renderable.handle.getDynamicX(),renderable.handle.getDynamicY());
				gc.setTransform(drawTrans);
				gc.renderHandle(renderable.handle, 0, 0, properties);
			}
			
			//gc.renderImage( renderable.handle.deepAccess(), x, y, render);
			gc.setTransform(oldTansform);
			
		}
	}
	
	static int cc=0;
	

	/** Determines the number of images needed to properly render 
	 * the given RenderSettings.  This number is equal to largest Group
	 * depth of any visible node. */
	protected int _getNeededImages(RenderSettings settings) {
		NodeValidator validator = new NodeValidator() {			
			@Override
			public boolean isValid(Node node) {
				return (node.getRender().isVisible() && !(node instanceof GroupNode)
						&& node.getChildren().size() == 0);
			}

			@Override
			public boolean checkChildren(Node node) {
				return (node.getRender().isVisible());
			}
		};
		
		List<Node> list = root.getAllNodesST(validator);

		int max = 0;
		for( Node ancestor : list) {
			int i = ancestor.getDepthFrom(root);
			if( i > max) max = i;
		}
		
		return max;
	}
}
