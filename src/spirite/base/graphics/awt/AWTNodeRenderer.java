package spirite.base.graphics.awt;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

import spirite.base.brains.RenderEngine;
import spirite.base.brains.RenderEngine.NodeRenderer;
import spirite.base.brains.RenderEngine.RenderSettings;
import spirite.base.brains.RenderEngine.TransformedHandle;
import spirite.base.graphics.GraphicsContext;
import spirite.base.image_data.ImageHandle;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.GroupTree.GroupNode;
import spirite.base.image_data.GroupTree.LayerNode;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.ImageWorkspace.BuiltImageData;
import spirite.hybrid.HybridHelper;
import spirite.hybrid.MDebug;
import spirite.hybrid.MDebug.ErrorType;

public class AWTNodeRenderer extends NodeRenderer {
	float ratioW, ratioH;	// TODO
	BufferedImage buffer[];
	ImageWorkspace workspace;
	
	AWTNodeRenderer( GroupNode node, RenderEngine context) {
		context.super(node);
		this.workspace = node.getContext();
	}
	
	@Override
	public void render(RenderSettings settings, GraphicsContext context, AffineTransform trans) {		
		try {
			AWTContext awtc = (AWTContext)context;
			
			buildCompositeLayer(workspace);
			
			// Step 1: Determine amount of data needed
			int n = _getNeededImagers( settings);
			if( n <= 0) return;
			
			buffer = new BufferedImage[n];
			for( int i=0; i<n; ++i) {
				buffer[i] = new BufferedImage( settings.width, settings.height, HybridHelper.BI_FORMAT);
			}

			// Step 3: Recursively draw the image
			ratioW = settings.width / (float)workspace.getWidth();
			ratioH = settings.height / (float)workspace.getHeight();

			_render_rec( root, 0, settings);
			
			// TODO: can remove 1 buffer
			Graphics g = awtc.getGraphics();
			
			g.drawImage(buffer[0], 0, 0, null);
			
			// Flush the data we only needed to build the image
			for( int i=0; i<n;++i)
				buffer[i].flush();
			g.dispose();
		}
		finally {
			buffer = null;
			clearCompositeImage();
		}
	}
	
	private BufferedImage compositionImage;
	private ImageHandle compositionContext = null;
	private void buildCompositeLayer(ImageWorkspace workspace) {
		BuiltImageData dataContext= workspace.buildActiveData();
		if( dataContext != null) {
			if( workspace.getSelectionEngine().getLiftedImage() != null 
				||  workspace.getDrawEngine().strokeIsDrawing()) {

				compositionImage= new BufferedImage( 
						dataContext.getWidth(), dataContext.getHeight(),
						HybridHelper.BI_FORMAT);
				compositionContext = dataContext.handle;
				

				Graphics2D g2 = (Graphics2D)compositionImage.getGraphics();
				
				// Draw Base Image
				g2.setTransform(dataContext.getCompositeTransform());
				g2.translate(dataContext.handle.getDynamicX(), 
						dataContext.handle.getDynamicY());
				g2.drawImage(dataContext.handle.deepAccess(), 0, 0, null);
			
				if( workspace.getSelectionEngine().getLiftedImage() != null ){
					// Draw Lifted Image
					g2.setTransform( dataContext.getScreenToImageTransform());
					g2.transform( workspace.getSelectionEngine().getDrawFromTransform());
					
					g2.drawImage( workspace.getSelectionEngine().getLiftedImage(), 0, 0, null);
				}
				if( workspace.getDrawEngine().strokeIsDrawing()) {
					// Draw
					g2.setTransform(new AffineTransform());
					workspace.getDrawEngine().getStrokeEngine().drawStrokeLayer(new AWTContext(g2));
				}
				g2.dispose();
			}
		}
	}
	private void clearCompositeImage() {
		if( compositionImage != null)
			compositionImage.flush();
		compositionContext = null;
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
		
		Graphics g = buffer[n].getGraphics();
		Graphics2D g2 = (Graphics2D)g;
		if( settings.hints != null)
			g2.setRenderingHints(settings.hints);
		
		// Go through the node's children (in reverse), drawing any visible group
		//	found recursively and drawing any Layer found plainly.
		
		// Step 1: Construct a list of all components that need to be rendered
		int count = 0;	// This subDepth counter is used to make sure Renderables of
						// the same depth are rendered in the correct order.
		
		
		ListIterator<Node> it = node.getChildren().listIterator(node.getChildren().size());
		List< Drawable> renderList = new ArrayList<>();
		while( it.hasPrevious()) {
			Node child = it.previous();
			if( child.isVisible()) {
				if( child instanceof GroupNode) {
					if( n == buffer.length-1) {
						// Note: the code can reach here if all the children are invisible.
						// There might be other, unintended ways for the code to reach here.
						continue;
					}
					
					Drawable renderable;
					renderable =  new GroupRenderable(
							(GroupNode) child, n, settings);
					renderable.subDepth = count++;
					renderList.add(renderable);
				}
				else {
					List<TransformedHandle> sub = ((LayerNode)child).getLayer().getDrawList();
					
					for( TransformedHandle subRend : sub) {
						Drawable renderable = new TransformedRenderable(
								(LayerNode) child, subRend, settings);
						renderable.subDepth = count++;
						renderList.add(renderable );
					}
				}
			}
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
			renderable.draw(g2);
		}

		g.dispose();
	}
	
	private abstract class Drawable {
		private int subDepth;
		protected int depth;
		public abstract void draw(Graphics g);
	}

	private Composite cc;
	private void _setGraphicsSettings( Graphics g, Node node, RenderSettings settings) {
		final Graphics2D g2 = (Graphics2D)g;
		 cc = g2.getComposite();
		
		if( node.getAlpha() != 1.0f) 
			g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, node.getAlpha()));
	}
	private void _resetRenderSettings( Graphics g, Node r, RenderSettings settings) {
		((Graphics2D)g).setComposite(cc);
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
		public void draw(Graphics g) {
			_render_rec(node, n+1, settings);
			_setGraphicsSettings(g, node,settings);
			g.drawImage( buffer[n+1],
					0, 0, 
					null);
			_resetRenderSettings(g, node,settings);
		}
	}
	private class TransformedRenderable extends Drawable {
		private final TransformedHandle renderable;
		private final RenderSettings settings;
		private final LayerNode node;
		private AffineTransform transform;
		TransformedRenderable( LayerNode node, TransformedHandle renderable, RenderSettings settings) {
			this.node = node;
			this.renderable = renderable;
			this.depth = renderable.depth;
			this.settings = settings;
			this.transform = renderable.trans;
			this.transform.translate(node.getOffsetX(), node.getOffsetY());
		}
		@Override
		public void draw(Graphics g) {
			
			_setGraphicsSettings(g, node,settings);
			Graphics2D g2 = (Graphics2D)g;
			AffineTransform oldTransform = g2.getTransform();
			
			AffineTransform drawTrans = new AffineTransform( transform);
			if( compositionContext == renderable.handle) {
				if( renderable.handle.isDynamic())
					drawTrans = new AffineTransform();
				g2.setTransform(drawTrans);
				g2.drawImage( compositionImage, 0, 0, null);
			}
			else {
				g2.setTransform(drawTrans);
				g2.translate(renderable.handle.getDynamicX(), 
						renderable.handle.getDynamicY());
				g2.drawImage( renderable.handle.deepAccess(), 0, 0, null);
			}
			
			g2.setTransform(oldTransform);
			_resetRenderSettings(g, node,settings);
		}
		
	}
}