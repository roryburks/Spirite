package spirite.gl;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Random;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.util.awt.AWTGLReadBufferUtil;

import spirite.gl.GLEngine.ProgramType;
import spirite.gl.GLParameters.GLImageTexture;
import spirite.gl.GLParameters.GLParam1i;
import spirite.gl.GLParameters.GLParam4f;;


/**
 * 
 * @author Guy
 *
 */
public class JOGLDrawer {
	private final GLEngine engine = GLEngine.getInstance();
	
	public JOGLDrawer() {
	}

    static int width = 640; 
    static int height = 480; 
    static int numPoints = 1000; 
    static Random r = new Random(); 
    
    /**
     * @param options 0th bit: 0 = ignore alpha, 1 = test alpha, 2 = change all
     */
    public void changeColor( BufferedImage bi, Color from, Color to, int options) {
    	GLParameters params = new GLParameters(bi.getWidth(), bi.getHeight());

    	params.addParam( new GLParam1i("optionMask", options));
    	params.addParam( new GLParam4f("cFrom", 
    			from.getRed()/255f, from.getGreen()/255f, from.getBlue()/255f, from.getAlpha()/255f));
    	params.addParam( new GLParam4f("cTo", 
    			to.getRed()/255f, to.getGreen()/255f, to.getBlue()/255f, to.getAlpha()/255f));

    	params.texture = new GLImageTexture(bi);

    	engine.clearSurface();
    	engine.applyPassProgram(ProgramType.CHANGE_COLOR, params, null);
    	
    	glSurfaceToImage(bi);
    }
    
    public void invert( BufferedImage bi) {
    	GLParameters params = new GLParameters(bi.getWidth(), bi.getHeight());
    	params.texture = new GLImageTexture(bi);

    	engine.clearSurface();
    	engine.applyPassProgram( ProgramType.PASS_INVERT, params, null);
    	glSurfaceToImage(bi);
    }
    
    private void glSurfaceToImage( BufferedImage bi) {
        BufferedImage im = engine.glSurfaceToImage();
        
		Graphics2D g = (Graphics2D)bi.getGraphics();
		g.setComposite(AlphaComposite.Src);
		g.drawImage(im, 0, 0, null);
    }
}
