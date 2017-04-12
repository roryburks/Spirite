package spirite.graphics.gl;

import java.awt.AlphaComposite;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;

import spirite.MDebug;
import spirite.MDebug.ErrorType;
import spirite.brains.RenderEngine;
import spirite.brains.RenderEngine.NodeRenderer;
import spirite.brains.RenderEngine.RenderMethod;
import spirite.brains.RenderEngine.RenderSettings;
import spirite.brains.RenderEngine.TransformedHandle;
import spirite.graphics.gl.GLEngine.ProgramType;
import spirite.graphics.gl.GLMultiRenderer.GLRenderer;
import spirite.image_data.GroupTree.GroupNode;
import spirite.image_data.GroupTree.LayerNode;
import spirite.image_data.GroupTree.Node;
import spirite.image_data.ImageWorkspace;

/** 
 * This updated version uses JOGL rendering algorithms instead of 
 * AWT rendering, so that custom fragment shaders can be injected 
 * without performace-loss.
 * 
 * I will attempt to explain how the shaders work in relation to each
 * other and why they are built that way:
 * 
 * To begin, we are using Porter/Duff composition rules (in particular
 * Source Over Destination), which requires using pre-multiplied pixel
 * format.  Now it is possible 
 * 
 * Any time that new colors are being drawn to a GLSurface, they must
 * be multiplied with their alpha to convert it to premultiplied form.
 * When GLSurfaces are drawn to GLSurfaces, the data is already premultiplied
 * so it can be drawn normally.  Finally, data must be converted to 
 * non-premultiplied form since that is the internal format that is used.
 * This is done using the PASS_ESCALATE shader.  All other actions are
 * accomplished using PASS_RENDER and appropriate uComp values.
 * (see "pass_render.frag" for appropriate values).
 */
class GLNodeRenderer extends NodeRenderer {
	GLMultiRenderer glmu[];
	float ratioW;
	float ratioH;
	ImageWorkspace workspace;
	private final GLEngine engine = GLEngine.getInstance();
	private final RenderEngine context;
	
	public GLNodeRenderer( GroupNode node, RenderEngine context) {
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
			engine.setSurfaceSize(settings.width, settings.height);
			
			// Prepare the FrameBuffers needed for rendering
			glmu = new GLMultiRenderer[n];
			for( int i=0; i<n; ++i) {
				glmu[i] = new GLMultiRenderer(
						settings.width, settings.height, engine.getGL2());
				glmu[i].init();
				glmu[i].render(new GLRenderer() {
					@Override public void render(GL gl) {
						engine.clearSurface(gl.getGL2());
					}
				});
			}

			// Step 3: Recursively draw the image
			ratioW = settings.width / (float)workspace.getWidth();
			ratioH = settings.height / (float)workspace.getHeight();

			_render_rec( root, 0, settings);

			// Step 4: Render the top-most FBO to the GLSurface and then that
			// surface onto a BufferedImage so that Swing can draw it.
			// TODO: This last step could be avoided if I used a GLPanel instead
			//	of a basic Swing Panel
			engine.setSurfaceSize(settings.width, settings.height);
			GLParameters params = new GLParameters(settings.width, settings.height);
			params.texture = new GLParameters.GLFBOTexture(glmu[0]);
	    	engine.clearSurface(engine.getGL2());
			engine.applyPassProgram(ProgramType.PASS_ESCALATE, params, null, 
					0, 0, settings.width, settings.height, true, engine.getGL2());
			
			BufferedImage bi = engine.glSurfaceToImage();

			// Flush the data we only needed to build the image
			for( int i=0; i<n;++i) {
				glmu[i].cleanup();
			}
			
			return bi;
		}
		finally {
			glmu = null;
			//buffer = null;
		}
	}
	
	private void _render_rec(GroupNode node, int n, RenderSettings settings) 
	{
		if( n < 0 || n >= glmu.length) {
			MDebug.handleError(ErrorType.STRUCTURAL, "Error: propperRender exceeds expected image need.");
			return;
		}
		
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
					if( n == glmu.length-1) {
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
			renderable.draw(n);
		}
	}
	
	// ==================
	// ==== Node-specific Drawing
	private void setParamsFromNode(
			Node node, GLParameters params, boolean fullPremult, float moreAlpha) 
	{
		
		int method_num = 0;
		
		boolean premult = true;
		RenderMethod method;
		int renderValue = 0;
		int stage = workspace.getStageManager().getNodeStage(node);
		
		if( stage == -1) {
			method = node.getRenderMethod();
			renderValue = node.getRenderValue();
		}
		else {
			method = RenderMethod.COLOR_CHANGE;
			switch( stage % 3) {
			case 0: renderValue = 0xFF0000;break;
			case 1: renderValue = 0x00FF00;break;
			case 2: renderValue = 0x0000FF;break;
			}
		}
		
		
		switch( method) {
		case COLOR_CHANGE:
			method_num = 1;
			break;
		case DEFAULT:
			break;
		case LIGHTEN:
			method_num = 0;
			params.setBlendModeExt(
					GL2.GL_ONE, GL2.GL_ONE, GL2.GL_FUNC_ADD,
					GL2.GL_ZERO, GL2.GL_ONE, GL2.GL_FUNC_ADD);
			break;
		case SUBTRACT:
			method_num = 0;
			params.setBlendModeExt(
					GL2.GL_ZERO, GL2.GL_ONE_MINUS_SRC_COLOR, GL2.GL_FUNC_ADD,
					GL2.GL_ZERO, GL2.GL_ONE, GL2.GL_FUNC_ADD);
			break;
		case MULTIPLY:
			method_num = 0;
			params.setBlendModeExt(GL2.GL_DST_COLOR, GL2.GL_ONE_MINUS_SRC_ALPHA, GL2.GL_FUNC_ADD,
					GL2.GL_ZERO, GL2.GL_ONE, GL2.GL_FUNC_ADD);
			break;
		case SCREEN:
			// C = (1 - (1-DestC)*(1-SrcC) = SrcC*(1-DestC) + DestC
			method_num = 0;
			params.setBlendModeExt(GL2.GL_ONE_MINUS_DST_COLOR, GL2.GL_ONE, GL2.GL_FUNC_ADD,
					GL2.GL_ZERO, GL2.GL_ONE, GL2.GL_FUNC_ADD);
			break;
		case OVERLAY:
			method_num = 3;
			break;
		}
		params.addParam( new GLParameters.GLParam1f("uAlpha", node.getAlpha()*moreAlpha));
		params.addParam( new GLParameters.GLParam1ui("uValue", renderValue));
		params.addParam( new GLParameters.GLParam1i("uComp", (method_num << 1) | ((fullPremult&&premult)?1:0)));
	}
	
	private abstract class Drawable {
		private int subDepth;
		protected int depth;
		public abstract void draw(int ind);
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
		public void draw(int ind) {
			glmu[n+1].render( engine.clearRenderer);
			_render_rec(node, n+1, settings);

			glmu[n].render( new GLRenderer() {
				@Override
				public void render(GL _gl) {
					GLParameters params = new GLParameters(settings.width, settings.height);
					setParamsFromNode( node, params, false, 1);
					params.texture = new GLParameters.GLFBOTexture(glmu[n+1]);
					engine.applyPassProgram(ProgramType.PASS_RENDER, params, null,
							0, 0, params.width, params.height, true, engine.getGL2());
				}
			});
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
		public void draw(int ind) {

			glmu[ind].render( new GLRenderer() {
				@Override
				public void render(GL _gl) {
					
					GLParameters params = new GLParameters(settings.width, settings.height);
					
					float alpha = 1;
					if( renderable.comp instanceof AlphaComposite)
						alpha *= ((AlphaComposite)renderable.comp).getAlpha();
					setParamsFromNode( node, params, true, alpha);
					
					AffineTransform trans = new AffineTransform(transform);

					BufferedImage bi =  context.getCompositeLayer(renderable.handle);
					if( bi == null) {
						bi = renderable.handle.deepAccess();
						trans.translate(renderable.handle.getDynamicX(), 
								renderable.handle.getDynamicY());
					}
					
					params.texture = new GLParameters.GLImageTexture(bi);
					engine.applyPassProgram(
							ProgramType.PASS_RENDER, params, trans,
							0, 0, bi.getWidth(), bi.getHeight(), false, engine.getGL2());
				}
			});
		}
		
	}
}
