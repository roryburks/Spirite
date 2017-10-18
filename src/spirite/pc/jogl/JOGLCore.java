package spirite.pc.jogl;

import com.jogamp.opengl.DefaultGLCapabilitiesChooser;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLOffscreenAutoDrawable;
import com.jogamp.opengl.GLProfile;

import spirite.base.graphics.gl.wrap.GLCore;

/**
 * GLCore wraps the core initialization and binding of the OpenGL engine.
 * 
 * @author Rory Burks
 *
 */
public class JOGLCore extends GLCore {
	private static GLOffscreenAutoDrawable drawable;
	private static boolean initialized = false;
	
	public static GLContext getContext() {
		return (drawable == null) ? null : drawable.getContext();
	}
	
	public static interface OnGLLoadObserver {
		public void onLoad(GL2 gl) throws MGLException;
	}
	
	public static void init(OnGLLoadObserver obs) throws MGLException {
		if( initialized) return;
		
		// Create Offscreen OpenGl Surface
		GLProfile profile = GLProfile.getDefault();
        GLDrawableFactory fact = GLDrawableFactory.getFactory(profile);
		GLCapabilities caps = new GLCapabilities(profile);
        caps.setHardwareAccelerated(true); 
        caps.setDoubleBuffered(false); 
        caps.setAlphaBits(8); 
        caps.setRedBits(8); 
        caps.setBlueBits(8); 
        caps.setGreenBits(3); 
        caps.setOnscreen(false);

		drawable = fact.createOffscreenAutoDrawable(
				fact.getDefaultDevice(), 
				caps,
				new DefaultGLCapabilitiesChooser(), 
				1, 1);
		
		ex = null;
		
		drawable.addGLEventListener( new GLEventListener() {
			@Override public void reshape(GLAutoDrawable arg0, int arg1, int arg2, int arg3, int arg4) {}
			@Override public void dispose(GLAutoDrawable arg0) {}
			@Override public void display(GLAutoDrawable arg0) {}
			@Override public void init(GLAutoDrawable gad) {
				try {
					if( obs != null)
						obs.onLoad(gad.getGL().getGL2());
				} catch (MGLException e) {
					ex = e;
				}
				//GL gli = gad.getGL();
				//gli = gli.getContext().setGL(  GLPipelineFactory.create("com.jogamp.opengl.Trace", null, gl, new Object[] { System.err }) );
			}
		});	

		drawable.display();
		
		if( ex != null) throw ex;
		initialized = true;
	}
	private static MGLException ex = null;
	
	public static boolean isInitialized() {return initialized;}

	// ============
	// ==== Simple API
	
	private static GL2 currentGL;
	public static  GL2 getGL2() {
		if( currentGL == null) {
			currentGL = drawable.getGL().getGL2();
		}
		if( !currentGL.getContext().isCurrent())
			currentGL.getContext().makeCurrent();
		
		return currentGL;
	}
	public static void setGL( GL2 gl) {
		currentGL = gl;
	}
	
	
	@Override
	public boolean supportsGeometryShader() {
		return false;
	}
}
