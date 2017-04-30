package spirite.base.graphics.gl;

import java.awt.image.BufferedImage;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;

import spirite.base.graphics.gl.engine.GLEngine;
import spirite.base.graphics.gl.engine.GLMultiRenderer;
import spirite.base.graphics.gl.engine.GLParameters;
import spirite.base.graphics.gl.engine.GLEngine.ProgramType;
import spirite.base.graphics.gl.engine.GLMultiRenderer.GLRenderer;
import spirite.base.graphics.gl.engine.GLParameters.GLFBOTexture;
import spirite.base.graphics.gl.engine.GLParameters.GLImageTexture;
import spirite.base.graphics.gl.engine.GLParameters.GLParam1i;
import spirite.base.graphics.gl.engine.GLParameters.GLParam4f;
import spirite.hybrid.HybridHelper;
import spirite.pc.graphics.ImageBI;

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
				params.texture = new GLImageTexture( new ImageBI(bi));
				engine.applyPassProgram(ProgramType.PASS_BORDER, params, null, false, _gl.getGL2());
			}
		});
		

		GLMultiRenderer glmub = new GLMultiRenderer(
				w, h, gl);
		glmub.init();
		glmub.render( new GLRenderer() {
			@Override
			public void render(GL gl) {
				GLParameters params = new GLParameters(w, h);
				params.addParam( new GLParam1i("uCycle", cycle));
				params.texture = new GLFBOTexture(glmu);
				engine.applyPassProgram(ProgramType.PASS_BORDER, params, null, true, gl.getGL2());
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

        BufferedImage im = engine.glSurfaceToImage(HybridHelper.BI_FORMAT);
       
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
