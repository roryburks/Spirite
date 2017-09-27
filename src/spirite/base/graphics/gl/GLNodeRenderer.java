package spirite.base.graphics.gl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

import spirite.base.brains.RenderEngine;
import spirite.base.brains.RenderEngine.NodeRenderer;
import spirite.base.brains.RenderEngine.RenderMethod;
import spirite.base.brains.RenderEngine.RenderSettings;
import spirite.base.brains.RenderEngine.TransformedHandle;
import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.GraphicsContext.Composite;
import spirite.base.graphics.RenderProperties;
import spirite.base.graphics.gl.GLEngine.ProgramType;
import spirite.base.image_data.Animation;
import spirite.base.image_data.AnimationManager.AnimationState;
import spirite.base.image_data.GroupTree.AnimationNode;
import spirite.base.image_data.GroupTree.GroupNode;
import spirite.base.image_data.GroupTree.LayerNode;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.ImageHandle;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.ImageWorkspace.BuiltImageData;
import spirite.base.util.glmath.GLC;
import spirite.base.util.glmath.MatTrans;
import spirite.hybrid.MDebug;
import spirite.hybrid.MDebug.ErrorType;

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
 * 
 */
class GLNodeRenderer extends NodeRenderer {
	private GLImage buffer[];
	private float ratioW;	// TODO: UNUSED
	private float ratioH;	// TODO: UNUSED
	
	private final ImageWorkspace workspace;
	
	public GLNodeRenderer( GroupNode node, RenderEngine context) {
		context.super(node);
		this.workspace = node.getContext();
	}

	@Override
	public void render(RenderSettings settings, GraphicsContext context, MatTrans trans) {
		try {
			
			GraphicsContext glgc = context;
			
			buildCompositeLayer(workspace);
			
			// Step 1: Determine amount of data needed
			int n = _getNeededImagers( settings);

			if( n <= 0) 
				return;
			
			// Prepare the FrameBuffers needed for rendering
			buffer = new GLImage[n];
			for( int i=0; i<n; ++i) {
				buffer[i] = new GLImage(settings.width, settings.height);
				
				GraphicsContext _gc = buffer[i].getGraphics();
				_gc.clear();
			}

			// Step 3: Recursively draw the image
			ratioW = settings.width / (float)workspace.getWidth();
			ratioH = settings.height / (float)workspace.getHeight();

			_render_rec( root, 0, settings);

			// Step 4: Render the top-most FBO to the GLSurface and then that
			// surface onto a BufferedImage so that Swing can draw it.
			// TODO: This last step could be avoided if I used a GLPanel instead
			//	of a basic Swing Panel
			glgc.drawImage(buffer[0], 0, 0);

			// Flush the data we only needed to build the image
			for( int i=0; i<n;++i) {
				buffer[i].flush();
			}
		}
		catch (Exception e) {
		}
		finally {
			buffer = null;
			freeCompositeLayer();
			//buffer = null;
		}
	}
	
	private GLImage compositeLayer = null;
	private ImageHandle compositedHandle = null;
	private void buildCompositeLayer(ImageWorkspace workspace) {
		BuiltImageData dataContext= workspace.buildActiveData();
		if( dataContext != null) {
			if( workspace.getSelectionEngine().getLiftedImage() != null 
				||  workspace.getDrawEngine().strokeIsDrawing()) {
				
				compositeLayer = new GLImage(dataContext.getWidth(), dataContext.getHeight());
				compositedHandle = dataContext.handle;

				GLGraphics glgc = compositeLayer.getGraphics();
				
				// Draw Base Image
				MatTrans trans = dataContext.getCompositeTransform();
				trans.translate( dataContext.handle.getDynamicX(), 
						dataContext.handle.getDynamicY());
				glgc.setTransform(trans);
				

				GLParameters params = new GLParameters(dataContext.getWidth(), dataContext.getHeight());
				params.texture = dataContext.handle.accessGL();
				params.addParam( new GLParameters.GLParam1i("uComp", 0));
				params.addParam( new GLParameters.GLParam1f("uAlpha", 1.0f));
				GLGraphics.setCompositeBlend(params, Composite.SRC_OVER);
				glgc.applyPassProgram(ProgramType.PASS_RENDER, params, trans, 
						0, 0, params.texture.getWidth(), params.texture.getHeight());

				if( workspace.getSelectionEngine().getLiftedImage() != null ){
					// Draw Lifted Image
					trans = dataContext.getScreenToImageTransform();
					trans.concatenate(workspace.getSelectionEngine().getDrawFromTransform());
					glgc.setTransform(trans);
					
					glgc.drawImage( workspace.getSelectionEngine().getLiftedImage(), 0, 0);
				}
				if( workspace.getDrawEngine().strokeIsDrawing()) {
					// Draw Stroke Layer
					glgc.setTransform(new MatTrans());
					workspace.getDrawEngine().getStrokeEngine().drawStrokeLayer(glgc);
				}
			}
		}
	}
	private void freeCompositeLayer() {
		if( compositeLayer != null)
			compositeLayer.flush();
		compositedHandle = null;
		compositeLayer = null;
	}
	
	private void _render_rec(GroupNode node, int n, RenderSettings settings) 
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
					renderable =  new GroupRenderable(
							(GroupNode) child, n, settings);
					renderable.draw(n);
//					renderable.subDepth = count++;
//					renderList.add(renderable);
				}
				else if( child instanceof LayerNode){
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
						renderable.draw(n);
					}
				}
				else if( child instanceof AnimationNode) {
					Animation anim = ((AnimationNode)child).getAnimation();
					AnimationState as = workspace.getAnimationManager().getAnimationState(anim);
					List<List<TransformedHandle>> table = anim.getDrawTable(as.getSelectedMetronome(), as);
					
					for( List<TransformedHandle> list : table) {
						for( TransformedHandle th : list) {
							(new TransformedRenderable( new RenderProperties(), th, settings, 0, 0)).draw(n);
						}
					}
				}
			}
		}
	}
	
	// ==================
	// ==== Node-specific Drawing
	private void setParamsFromNode(
			RenderProperties properties, GLParameters params, boolean fullPremult, float moreAlpha) 
	{
		
		int method_num = 0;
		
		RenderMethod method;
		int renderValue = 0;
//		int stage = workspace.getStageManager().getNodeStage(node);
//		
//		if( stage == -1) {
//			method = node.getRender().getMethod();
//			renderValue = node.getRender().getRenderValue();
//		}
//		else {
//			method = RenderMethod.COLOR_CHANGE;
//			switch( stage % 3) {
//			case 0: renderValue = 0xFF0000;break;
//			case 1: renderValue = 0x00FF00;break;
//			case 2: renderValue = 0x0000FF;break;
////			}
//		}
		method = properties.getMethod();
		renderValue = properties.getRenderValue();
		
		switch( method) {
		case COLOR_CHANGE:
			method_num = 1;
			GLGraphics.setCompositeBlend(params, Composite.SRC_OVER);
			break;
		case DEFAULT:
			GLGraphics.setCompositeBlend(params, Composite.SRC_OVER);
			break;
		case LIGHTEN:
			method_num = 0;
			params.setBlendModeExt(
					GLC.GL_ONE, GLC.GL_ONE, GLC.GL_FUNC_ADD,
					GLC.GL_ZERO, GLC.GL_ONE, GLC.GL_FUNC_ADD);
			break;
		case SUBTRACT:
			method_num = 0;
			params.setBlendModeExt(
					GLC.GL_ZERO, GLC.GL_ONE_MINUS_SRC_COLOR, GLC.GL_FUNC_ADD,
					GLC.GL_ZERO, GLC.GL_ONE, GLC.GL_FUNC_ADD);
			break;
		case MULTIPLY:
			method_num = 0;
			params.setBlendModeExt(GLC.GL_DST_COLOR, GLC.GL_ONE_MINUS_SRC_ALPHA, GLC.GL_FUNC_ADD,
					GLC.GL_ZERO, GLC.GL_ONE, GLC.GL_FUNC_ADD);
			break;
		case SCREEN:
			// C = (1 - (1-DestC)*(1-SrcC) = SrcC*(1-DestC) + DestC
			method_num = 0;
			params.setBlendModeExt(GLC.GL_ONE_MINUS_DST_COLOR, GLC.GL_ONE, GLC.GL_FUNC_ADD,
					GLC.GL_ZERO, GLC.GL_ONE, GLC.GL_FUNC_ADD);
			break;
		case OVERLAY:
			method_num = 3;
			break;
		}
		params.addParam( new GLParameters.GLParam1f("uAlpha", properties.getAlpha()*moreAlpha));
		params.addParam( new GLParameters.GLParam1ui("uValue", renderValue));
		params.addParam( new GLParameters.GLParam1i("uComp", (method_num << 1) ));
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
			GLGraphics glgc = buffer[n+1].getGraphics();
			glgc.clear();
//			engine.setTarget(buffer[n+1]);
//			engine.clearSurface(gl.getGL2());
//			engine.setTarget(0);
			
			_render_rec(node, n+1, settings);

			glgc = buffer[n].getGraphics();
//			engine.setTarget(buffer[n]);
			GLParameters params = new GLParameters(settings.width, settings.height);
			setParamsFromNode( node.getRender(), params, false, 1);
			params.texture = new GLParameters.GLImageTexture(buffer[n+1]);
			glgc.applyPassProgram(ProgramType.PASS_RENDER, params, null,
					0, 0, params.width, params.height);
//			engine.setTarget(0);
		}
	}
	private class TransformedRenderable extends Drawable {
		private final TransformedHandle renderable;
		private final RenderSettings settings;
		private final RenderProperties properties;
		//private final LayerNode node;
		private MatTrans transform;
		TransformedRenderable( RenderProperties properties, TransformedHandle renderable, RenderSettings settings, int ox, int oy) {
			//this.node = node;
			this.properties = properties;
			this.renderable = renderable;
			this.depth = renderable.depth;
			this.settings = settings;
			this.transform = renderable.trans;
			this.transform.translate(ox, oy);
		}
		@Override
		public void draw(int ind) {

			GLGraphics glgc = buffer[ind].getGraphics();
//			engine.setTarget( buffer[ind]);
					
			GLParameters params = new GLParameters(settings.width, settings.height);
			
			float alpha = 1;
			alpha *= renderable.alpha;
			setParamsFromNode( properties, params, true, alpha);
			
			MatTrans trans = new MatTrans(transform);

			GLGraphics.setCompositeBlend(params, Composite.SRC_OVER);
			if( compositedHandle == renderable.handle) {
				if( renderable.handle.isDynamic())
					trans = new MatTrans();
				params.texture = new GLParameters.GLImageTexture(compositeLayer);
				glgc.applyPassProgram(
						ProgramType.PASS_RENDER, params, trans,
						0, 0, compositeLayer.getWidth(), compositeLayer.getHeight());
			}
			else {
				trans.translate(renderable.handle.getDynamicX(), 
						renderable.handle.getDynamicY());
				params.texture = renderable.handle.accessGL();
				glgc.applyPassProgram(
						ProgramType.PASS_RENDER, params, trans,
						0, 0, renderable.handle.getWidth(), renderable.handle.getHeight());
			}
		}
	}
}
