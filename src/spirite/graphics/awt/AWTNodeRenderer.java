package spirite.graphics.awt;

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

import spirite.Globals;
import spirite.MDebug;
import spirite.MDebug.ErrorType;
import spirite.brains.RenderEngine;
import spirite.brains.RenderEngine.NodeRenderer;
import spirite.brains.RenderEngine.RenderSettings;
import spirite.brains.RenderEngine.TransformedHandle;
import spirite.image_data.GroupTree.GroupNode;
import spirite.image_data.GroupTree.LayerNode;
import spirite.image_data.GroupTree.Node;
import spirite.image_data.ImageWorkspace;

public class AWTNodeRenderer extends NodeRenderer {
	float ratioW;
	BufferedImage buffer[];
	float ratioH;
	ImageWorkspace workspace;
	private final RenderEngine context;
	
	AWTNodeRenderer( GroupNode node, RenderEngine context) {
		context.super(node);
		this.context = context;
		this.workspace = node.getContext();
	}
	
	@Override
	public BufferedImage render(RenderSettings settings) {		
		try {
			// Step 1: Determine amount of data needed
			int n = _getNeededImagers( settings);
			if( n <= 0) return null;
			
			buffer = new BufferedImage[n];
			for( int i=0; i<n; ++i) {
				buffer[i] = new BufferedImage( settings.width, settings.height, Globals.BI_FORMAT);
			}

			// Step 3: Recursively draw the image
			ratioW = settings.width / (float)workspace.getWidth();
			ratioH = settings.height / (float)workspace.getHeight();

			_render_rec( root, 0, settings);
			
			// Flush the data we only needed to build the image
			for( int i=1; i<n;++i)
				buffer[i].flush();
			
			return buffer[0];
		}
		finally {buffer = null;}
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
			AffineTransform transform = g2.getTransform();
			g2.scale( ratioW, ratioH);
			renderable.handle.drawLayer(g2, this.transform, renderable.comp);
			
			g2.setTransform(transform);
			_resetRenderSettings(g, node,settings);
		}
		
	}
}