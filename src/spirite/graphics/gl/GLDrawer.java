package spirite.graphics.gl;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import com.jogamp.opengl.GL2;

import spirite.brains.RenderEngine;
import spirite.brains.RenderEngine.NodeRenderer;
import spirite.graphics.GraphicsDrawer;
import spirite.graphics.gl.engine.GLEngine;
import spirite.graphics.gl.engine.GLParameters;
import spirite.graphics.gl.engine.GLEngine.ProgramType;
import spirite.graphics.gl.engine.GLParameters.GLImageTexture;
import spirite.graphics.gl.engine.GLParameters.GLParam1i;
import spirite.graphics.gl.engine.GLParameters.GLParam4f;
import spirite.image_data.GroupTree.GroupNode;
import spirite.pen.StrokeEngine;;


/**
 * GLDrawer is a mostly-static class that changes and creates new BufferedImages
 * corresponding to various rendering behaviors.
 * 
 * @author Rory Burks
 */
public class GLDrawer extends GraphicsDrawer {
	private final GLEngine engine = GLEngine.getInstance();
	
	// TODO: Singly probably isn't the best paradigm for this.
	private static GLDrawer singly;
	public static GLDrawer getInstance() {if( singly == null) singly = new GLDrawer(); return singly;}
	private GLDrawer() {}

	private final GLStrokeEngine strokeEngine = new GLStrokeEngine();
	@Override
	public NodeRenderer createNodeRenderer(GroupNode node, RenderEngine context) {
		return new GLNodeRenderer( node, context);
	}

	@Override
	public StrokeEngine getStrokeEngine() {
		return strokeEngine;
	}
    

	@Override
	public BufferedImage renderToImage(RenderRoutine renderable, int width, int height) {
		engine.setSurfaceSize(width, height);

		renderable.render( new GLGraphics( engine.getAutoDrawable(), false));

		return engine.glSurfaceToImage();
	}

	@Override
	public void changeColor(BufferedImage image, Color from, Color to, int mode) {
    	GLParameters params = new GLParameters(image.getWidth(), image.getHeight());

    	GL2 gl = engine.getGL2();
    	engine.setSurfaceSize(image.getWidth(), image.getHeight());
    	
    	params.addParam( new GLParam1i("optionMask", mode));
    	params.addParam( new GLParam4f("cFrom", 
    			from.getRed()/255f, from.getGreen()/255f, from.getBlue()/255f, from.getAlpha()/255f));
    	params.addParam( new GLParam4f("cTo", 
    			to.getRed()/255f, to.getGreen()/255f, to.getBlue()/255f, to.getAlpha()/255f));

    	params.texture = new GLImageTexture(image);

    	engine.clearSurface(gl);
    	engine.applyPassProgram(ProgramType.CHANGE_COLOR, params, null, false, gl);
		
    	glSurfaceToImage(image);
	}

	@Override
	public void invert(BufferedImage image) {

    	GL2 gl = engine.getGL2();
    	engine.setSurfaceSize(image.getWidth(), image.getHeight());

    	GLParameters params = new GLParameters(image.getWidth(), image.getHeight());
    	params.texture = new GLImageTexture(image);

    	engine.clearSurface(engine.getGL2());
    	engine.applyPassProgram( ProgramType.PASS_INVERT, params, null, false, engine.getGL2());
		
    	glSurfaceToImage(image);
		
	}
	
	/** Puts the active GL RenderingSurface onto an existing BufferedImage. */
    private void glSurfaceToImage( BufferedImage bi) {
        BufferedImage im = engine.glSurfaceToImage();
        
		Graphics2D g = (Graphics2D)bi.getGraphics();
		g.setComposite(AlphaComposite.Src);
		g.drawImage(im, 0, 0, null);
    }
}
