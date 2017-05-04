package spirite.base.graphics.gl.engine;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;

import com.jogamp.opengl.GL2;

import spirite.base.graphics.GraphicsContext;
import spirite.base.image_data.ImageHandle;
import spirite.base.image_data.RawImage;
import spirite.base.util.Colors;
import spirite.base.util.DataCompaction.FloatCompactor;
import spirite.base.util.MUtil;
import spirite.base.util.glmath.GLC;
import spirite.base.util.glmath.MatTrans;
import spirite.base.util.glmath.Rect;
import spirite.base.graphics.gl.engine.GLEngine.PolyType;
import spirite.base.graphics.gl.engine.GLEngine.ProgramType;
import spirite.base.graphics.gl.engine.GLParameters.*;

/**
 * GLGraphics is a GraphicsContext using the GLEngine, duplicating (or at least
 * substituting) the necessary features of AWT Graphics using OpenGL hardware
 * rendering.
 * 
 * @author Rory Burks
 *
 */
public class GLGraphics extends GraphicsContext{
	private final GLEngine engine = GLEngine.getInstance();
	
	private GL2 gl;
	private int width, height;
	private boolean flip = false;
	private MatTrans contextTransform = new MatTrans();
	private int color = Colors.BLACK;
	private Composite composite = Composite.SRC_OVER;
	private float alpha = 1.0f;
	private LineAttributes lineAttributes = defaultLA;
	private final GLImage image;
	private Rect clipRect;

	public GLGraphics( int width, int height, boolean flip) {
		this.flip = flip;
		setDimensions( width, height);
		this.image = null;
	}
	public GLGraphics( GLImage glImage ) {
		this.image = glImage;
		setDimensions(glImage.getWidth(), glImage.getHeight());
	}
	

	public boolean isFlip() {return flip;}
	public void setFlip(boolean b) { flip = b;}
	public int getWidth() {return width;}
	public int getHeight() { return height;}
	public GL2 getGL() { reset();return gl;}
	
	private void setDimensions(int width, int height) {
		if( this.width == width && this.height == height)
			return;
		this.width = width;
		this.height = height;
		if( cachedPolyParams != null) {
			cachedPolyParams.width = width;
			cachedPolyParams.height= height;
		}
		if( cachedImgParams != null) {
			cachedImgParams.width = width;
			cachedImgParams.height= height;
		}
	}
	
	public synchronized void reset() {
		this.gl = engine.getGL2();
		
		engine.setTarget(image);
	}

	@Override
	public void drawBounds(RawImage mask, int c) {
//		drawImage(mask, 0, 0);
		
		GLImage img = new GLImage( width, height);
		
		GLGraphics other = img.getGraphics();
		other.clear();
		
		GLParameters params2 = new GLParameters(getImgParams());
		params2.texture = new GLImageTexture( mask);
		other.applyPassProgram( ProgramType.PASS_BASIC, params2, contextTransform,
				0, 0, mask.getWidth(), mask.getHeight());
		
		
		// Clean up and Apply the surface to an image
		params2 = new GLParameters(width, height);
		params2.addParam( new GLParam1i("uCycle", c));
		params2.texture = new GLImageTexture(img);
		applyPassProgram( ProgramType.PASS_BORDER, params2, null);
		
		img.flush();
	}


	private static final int c1 = 0xFFA8A8A8;
	private static final int c2 = 0xFFC0C0C0;
	public void drawTransparencyBG( Rect rect) {
		reset();
		drawTransparencyBG( rect, 4);
	}
	public void drawTransparencyBG( Rect rect, int size) {	
		if( rect.isEmpty())
			return;
		reset();
		
		GLParameters params = new GLParameters(width, height);
		params.flip = isFlip();
		params.addParam(new GLParameters.GLParam3f("uColor1", 
				Colors.getRed(c1)/255.0f, Colors.getGreen(c1)/255.0f, Colors.getBlue(c1)/255.0f));
		params.addParam(new GLParameters.GLParam3f("uColor2", 
				Colors.getRed(c2)/255.0f, Colors.getGreen(c2)/255.0f, Colors.getBlue(c2)/255.0f));
		params.addParam(new GLParameters.GLParam1i("uSize", size));
		params.useBlendMode = false;
		
		applyPassProgram(ProgramType.GRID, params, null,
				rect.x, rect.y, rect.x + rect.width, rect.y+rect.height);
	}
	
	@Override
	public void clear() {
		reset();
		engine.clearSurface(gl);
	}
	
	// =================
	// ==== Logistical Render Settings
	@Override public MatTrans getTransform() {return new MatTrans(contextTransform);}
	@Override public void setTransform(MatTrans trans) {
		if( trans == null) trans = new MatTrans();
		else trans = new MatTrans(trans);
		contextTransform = trans;
	}
	@Override public void translate(double offsetX, double offsetY) {
		contextTransform.translate((float)offsetX, (float)offsetY);
	}
	@Override public void scale(double sx, double sy) { 
		contextTransform.scale((float)sx, (float)sy); 
	}
	@Override
	public void transform(MatTrans trans) {
		contextTransform.concatenate(trans);
	}
	
	@Override public void setColor(int argb) {
		this.color = argb;
//		updateImgParams = true;
		updatePolyParams = true;
	}
	@Override
	public void setComposite(Composite composite, float alpha) {
		this.composite = composite;
		this.alpha = alpha;
		updateImgParams = true;
		updatePolyParams = true;
	}
	public float getAlpha() {return alpha;}
	public Composite getComposite() {return composite;}
	
	// ================
	// ==== LineAttributes
	private static final LineAttributes defaultLA = new LineAttributes(1, CapMethod.NONE, JoinMethod.ROUNDED);
	@Override
	public void setLineAttributes(LineAttributes line) {
		if( line == null)
			lineAttributes = defaultLA;
		else
			lineAttributes = line;
	}
	@Override
	public LineAttributes getLineAttributes() {
		//	LineAttributes are immutable, so it's fine to pass the internal object
		return lineAttributes;
	}
	
	// ==============
	// ==== Line Drawing Methods
	@Override
	public void drawRect(int x, int y, int w, int h) {
		reset();
		int x_[] = new int[4];
		int y_[] = new int[4];

		x_[0] = x_[3] = x;
		y_[0] = y_[1] = y;
		x_[1] = x_[2] = x + w;
		y_[2] = y_[3] = y + h;
		
		GLParameters params = getLineParams();
		engine.applyComplexLineProgram( x_, y_, 4, 
				lineAttributes.cap, lineAttributes.join, true, lineAttributes.width, 
				params, contextTransform);
	}
	@Override
	public void drawOval(int x, int y, int w, int h) {
		draw( new Ellipse2D.Float(x,y,w,h));		
	}
	@Override
	public void drawPolyLine(int[] xPoints, int[] yPoints, int count) {
		reset();
		GLParameters params =getLineParams();
		engine.applyComplexLineProgram(  xPoints, yPoints, count, 
				lineAttributes.cap, lineAttributes.join, false, lineAttributes.width, 
				params, contextTransform);
	}
	@Override
	public void drawLine(int x1, int y1, int x2, int y2) {
		reset();
		int x_[] = new int[2];
		int y_[] = new int[2];
		x_[0] = x1; x_[1] = x2;
		y_[0] = y1; y_[1] = y2;
		GLParameters params =getLineParams();
		engine.applyComplexLineProgram(  x_, y_, 2, 
				lineAttributes.cap, lineAttributes.join, false, lineAttributes.width, 
				params, contextTransform);
//		engine.applyLineProgram(ProgramType.STROKE_PIXEL, x_, y_, 2, params, contextTransform, gl);
		
	}
	@Override
	public void draw(Shape shape) {
		reset();
		FloatCompactor x_ = new FloatCompactor();
		FloatCompactor y_ = new FloatCompactor();
		
		MUtil.shapeToPoints( shape, x_, y_, 1, false);

		GLParameters params =getLineParams();

		float xPoints[] = x_.toArray();
		float yPoints[] = y_.toArray();
		engine.applyComplexLineProgram( xPoints, yPoints, xPoints.length, 
				lineAttributes.cap, lineAttributes.join, true, lineAttributes.width, 
				params, contextTransform);
	}
	
	public static void setCompositeBlend( GLParameters params, Composite comp) {
		switch( comp) {
		case SRC:params.setBlendMode( GLC.GL_ONE, GLC.GL_ZERO, GLC.GL_FUNC_ADD);break;
		case SRC_OVER: params.setBlendMode( GLC.GL_ONE, GLC.GL_ONE_MINUS_SRC_ALPHA, GLC.GL_FUNC_ADD);break;
		case SRC_IN:params.setBlendMode( GLC.GL_DST_ALPHA, GLC.GL_ZERO, GLC.GL_FUNC_ADD);break;
		case SRC_ATOP:params.setBlendMode( GLC.GL_DST_ALPHA, GLC.GL_ONE_MINUS_SRC_ALPHA, GLC.GL_FUNC_ADD);break;
		case SRC_OUT:params.setBlendMode( GLC.GL_ONE_MINUS_DST_ALPHA, GLC.GL_ZERO, GLC.GL_FUNC_ADD);break;
		
		case DST:params.setBlendMode( GLC.GL_ZERO, GLC.GL_ONE, GLC.GL_FUNC_ADD);break;
		case DST_OVER:params.setBlendMode( GLC.GL_ONE_MINUS_DST_ALPHA, GLC.GL_ONE, GLC.GL_FUNC_ADD);break;
		case DST_IN:params.setBlendMode( GLC.GL_ZERO, GLC.GL_SRC_ALPHA, GLC.GL_FUNC_ADD);break;
		case DST_ATOP:params.setBlendMode( GLC.GL_ONE_MINUS_DST_ALPHA, GLC.GL_SRC_ALPHA, GLC.GL_FUNC_ADD);break;
		case DST_OUT:params.setBlendMode( GLC.GL_ZERO, GLC.GL_ONE_MINUS_SRC_ALPHA, GLC.GL_FUNC_ADD);break;
		
		case XOR:params.setBlendMode( GLC.GL_ONE_MINUS_DST_ALPHA, GLC.GL_ONE_MINUS_SRC_ALPHA, GLC.GL_FUNC_ADD);break;
		case CLEAR:params.setBlendMode( GLC.GL_ZERO, GLC.GL_ZERO, GLC.GL_FUNC_ADD);break;
		}
	}
	
	// ============
	// ==== Fill Methods
	@Override
	public void fillRect(int x, int y, int w, int h) {
		reset();
		int x_[] = new int[4];
		int y_[] = new int[4];

		x_[0] = x_[1] = x;
		y_[0] = y_[2] = y;
		x_[2] = x_[3] = x + w;
		y_[1] = y_[3] = y + h;
		

		GLParameters params = getPolyParams();
		engine.applyPolyProgram(ProgramType.POLY_RENDER, x_, y_, x_.length, PolyType.STRIP, params, contextTransform, gl);
		
	}
	@Override
	public void fillOval(int x, int y, int w, int h) {
		reset();

		FloatCompactor x_ = new FloatCompactor();
		FloatCompactor y_ = new FloatCompactor();

		x_.add(x + (w/2.0f));
		y_.add(y + (h/2.0f));
		
		Shape shape = new Ellipse2D.Float(x, y, w, h);
		MUtil.shapeToPoints( shape, x_, y_, 0.01, true);

		GLParameters params = getPolyParams();

		float xPoints[] = x_.toArray();
		float yPoints[] = y_.toArray();
		
		engine.applyPolyProgram( ProgramType.POLY_RENDER, xPoints, yPoints, xPoints.length, PolyType.FAN, params, contextTransform, gl);
	}
	
	
	@Override
	public void drawImage( RawImage img, int x, int y) {
		GLParameters params = getImgParams();
		params.texture = new GLParameters.GLImageTexture(img);

		applyPassProgram(ProgramType.PASS_RENDER, params, contextTransform,
				x, y, x+img.getWidth(), y+img.getHeight());
		params.texture = null;
		
	}
	@Override
	public void drawHandle(ImageHandle handle, int x, int y) {
		GLParameters params =getImgParams();
		params.texture = handle.accessGL();

		applyPassProgram(ProgramType.PASS_RENDER, params, contextTransform,
				x, y, x+params.texture.getWidth(), y+params.texture.getHeight());
		params.texture = null;
	}
	
	
	// =============
	// ==== GLParameter Management
	private GLParameters cachedPolyParams = null;
	private GLParameters cachedImgParams = null;
	private boolean updatePolyParams = true;
	private boolean updateImgParams = true;
	
	private GLParameters getLineParams() {
		return getPolyParams();
	}

	private GLParameters getPolyParams() {
		if( cachedPolyParams == null) {
			cachedPolyParams = new  GLParameters(width, height);
			updatePolyParams = true;
		}
		if( updatePolyParams) {
			cachedPolyParams.clipRect = clipRect;
			cachedPolyParams.flip = flip;
			cachedPolyParams.clearParams();
			cachedPolyParams.addParam( new GLParameters.GLParam3f("uColor", 
					Colors.getRed(color)/255.0f, Colors.getGreen(color)/255.0f, Colors.getBlue(color)/255.0f));
			cachedPolyParams.addParam( new GLParameters.GLParam1f("uAlpha", alpha* Colors.getAlpha(color)/255.0f));
			setCompositeBlend(cachedPolyParams, composite);
			updatePolyParams = false;
		}
		return cachedPolyParams;
	}
	
	private GLParameters getImgParams() {
		if( cachedImgParams == null) {
			cachedImgParams = new  GLParameters(width, height);
			updateImgParams = true;
		}
		if( updateImgParams) {
			cachedImgParams.clipRect = clipRect;
			cachedImgParams.flip = flip;
			cachedImgParams.clearParams();
			cachedImgParams.addParam( new GLParameters.GLParam1f("uAlpha", alpha));
			setCompositeBlend(cachedImgParams, composite);
			updateImgParams = false;
		}
		return cachedImgParams;
	}
	@Override
	public void fillPolygon(int[] x, int[] y, int count) {
		// TODO Auto-generated method stub
		
	}
	
	
	// =========
	// ===== Direct 
	public void applyPassProgram(ProgramType type,GLParameters params, MatTrans trans)
	{
		reset();
		engine.applyPassProgram(type, params, trans);
	}

	public void applyPassProgram( ProgramType type, GLParameters params, MatTrans trans,
			float x1, float y1, float x2, float y2)
	{
		reset();
		engine.applyPassProgram(type, params, trans, x1, y1, x2, y2);
	}

	public void applyLineProgram( ProgramType type, int[] xPoints, int[] yPoints, 
			int numPoints, GLParameters params, MatTrans trans) {
		reset();
		engine.applyLineProgram(type, xPoints, yPoints, numPoints, params, trans);
	}
	public void applyLineProgram( ProgramType type, float[] xPoints, float[] yPoints, 
			int numPoints, GLParameters params, MatTrans trans)  {
		reset();
		engine.applyLineProgram(type, xPoints, yPoints, numPoints, params, trans, gl);
	}
	@Override
	public void setClip(int x, int y, int width, int height) {
		clipRect = new Rect( x, y, width, height);
		updatePolyParams = true;
		updateImgParams = true;
	}
	
}
