package spirite.base.graphics.gl;

import spirite.base.brains.RenderEngine;
import spirite.base.brains.RenderEngine.NodeRenderer;
import spirite.base.graphics.GraphicsDrawer;
import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.GraphicsContext.Composite;
import spirite.base.graphics.gl.GLEngine.ProgramType;
import spirite.base.graphics.gl.GLParameters.GLImageTexture;
import spirite.base.graphics.gl.GLParameters.GLParam1i;
import spirite.base.graphics.gl.GLParameters.GLParam4f;
import spirite.base.image_data.GroupTree.GroupNode;
import spirite.base.image_data.RawImage;
import spirite.base.pen.StrokeEngine;
import spirite.base.util.Colors;


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

	@Override public StrokeEngine getStrokeEngine() { return strokeEngine; }

	@Override
	public void changeColor(RawImage image, int from, int to, int mode) {
    	GLParameters params = new GLParameters(image.getWidth(), image.getHeight());

    	GLImage img = new GLImage( image.getWidth(), image.getHeight());
    	GLGraphics glgc = img.getGraphics();
//    	GL2 gl = engine.getGL2();
    	
    	engine.setTarget(img);
    	
    	params.addParam( new GLParam1i("optionMask", mode | 4));
    	params.addParam( new GLParam4f("cFrom", 
    			Colors.getRed(from)/255f, Colors.getGreen(from)/255f, Colors.getBlue(from)/255f, Colors.getAlpha(from)/255f));
    	params.addParam( new GLParam4f("cTo", 
    			Colors.getRed(to)/255f, Colors.getGreen(to)/255f, Colors.getBlue(to)/255f, Colors.getAlpha(to)/255f));

    	params.texture = new GLImageTexture( image);

    	glgc.clear();
    	glgc.applyPassProgram(ProgramType.CHANGE_COLOR, params, null);

    	GraphicsContext gc = image.getGraphics();
    	gc.setComposite( Composite.SRC, 1.0f);
    	gc.drawImage(img, 0, 0);
    	
    	img.flush();
	}

	@Override
	public void invert(RawImage image) {
    	//GL2 gl = engine.getGL2();
    	
    	GLImage img = new GLImage(image.getWidth(), image.getHeight());
    	
    	GLGraphics glgc = img.getGraphics();

    	GLParameters params = new GLParameters(image.getWidth(), image.getHeight());
    	params.texture = new GLImageTexture(image);

    	glgc.clear();
    	glgc.applyPassProgram( ProgramType.PASS_INVERT, params, null);
		
    	GraphicsContext gc = image.getGraphics();
    	gc.setComposite( Composite.SRC, 1.0f);
    	gc.drawImage(img, 0, 0);
    	
    	img.flush();
	}
}
