package spirite.graphics.gl;

import java.awt.image.BufferedImage;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;

import spirite.graphics.gl.GLEngine.ProgramType;
import spirite.graphics.gl.GLMultiRenderer.GLRenderer;
import spirite.graphics.gl.GLParameters.GLFBOTexture;
import spirite.graphics.gl.GLParameters.GLImageTexture;
import spirite.graphics.gl.GLParameters.GLParam1i;
import spirite.graphics.gl.GLParameters.GLParam4f;

/** 
 * USE OF GLUIDRAW IS BEING PHASED OUT.  ALL CODE IS BEING MOVED TO GLGraphics
 *
 */
class GLUIDraw {
	private static final GLEngine engine = GLEngine.getInstance();
	
	enum GradientType {
		RED,
		GREEN,
		BLUE,
		HUE,
		SATURATION,
		VALUE
	}
	
	/** A debug method which demonstrates the GLMultiRender capabilities:
	 * draws a border around the image, then changes that border color to red.
	 */
	public static BufferedImage _ddbounds( 
			BufferedImage bi, int cycle )
	{
		int w = bi.getWidth();
		int h = bi.getHeight();
		GL2 gl = engine.getGL2();
		engine.setSurfaceSize(w, h);

		GLMultiRenderer glmu = new GLMultiRenderer(
				w, h, gl);
		glmu.init();
		
		
		glmu.render( new GLRenderer() {
			@Override
			public void render(GL _gl) {
				GLParameters params = new GLParameters(w, h);
				params.addParam( new GLParam1i("uCycle", cycle));
				params.texture = new GLImageTexture(bi);
				engine.applyPassProgram(ProgramType.PASS_BORDER, params, null, false, gl);
			}
		});
		

		GLMultiRenderer glmub = new GLMultiRenderer(
				w, h, gl);
		glmub.init();
		glmub.render( new GLRenderer() {
			@Override
			public void render(GL _gl) {
				GLParameters params = new GLParameters(w, h);
				params.addParam( new GLParam1i("uCycle", cycle));
				params.texture = new GLFBOTexture(glmu);
				engine.applyPassProgram(ProgramType.PASS_BORDER, params, null, true, gl);
			}
		});

		GLParameters params = new GLParameters(w, h);

    	params.addParam( new GLParam1i("optionMask", 2));
    	params.addParam( new GLParam4f("cFrom", 0, 0, 0, 1));
    	params.addParam( new GLParam4f("cTo", 1, 0, 0, 1));
    	params.texture = new GLFBOTexture(glmu);
    	
    	engine.clearSurface(gl);
    	engine.applyPassProgram(ProgramType.CHANGE_COLOR, params, null, true, gl);
        
		glmu.cleanup();
		glmub.cleanup();

        BufferedImage im = engine.glSurfaceToImage();
       
		return im;
	}
	

	public static void drawColorGradient( float fixed, GradientType type, int w, int h, GL2 gl) {
		GLParameters params = new GLParameters(w, h);
		params.addParam( new GLParameters.GLParam1i("varCol", type.ordinal()));
		params.addParam( new GLParameters.GLParam1f("fixedCol", fixed));
		
		engine.applyPassProgram(ProgramType.SQARE_GRADIENT, params, null,
				0, 0, w, h, true, gl);
	}
}
