package spirite.gl;

import spirite.pen.PenTraits.PenState;

import java.awt.Graphics;
import java.nio.FloatBuffer;

import com.jogamp.opengl.GL4;
import com.jogamp.opengl.util.GLBuffers;

import spirite.image_data.DrawEngine.StrokeParams;
import spirite.image_data.ImageWorkspace.BuiltImageData;
import spirite.image_data.SelectionEngine.BuiltSelection;
import spirite.pen.StrokeEngine;

public class GLStrokeEngine extends StrokeEngine {
	private final GLEngine engine = GLEngine.getInstance();
	
	GLStrokeEngine() {
		GL4 gl = engine.getGL4();
	}
	@Override
	protected void finalize() throws Throwable {
		super.finalize();
	}
	
	@Override
	public boolean startStroke(StrokeParams s, PenState ps, BuiltImageData data, BuiltSelection selection) {
		return super.startStroke(s, ps, data, selection);
	}
	
	@Override
	public boolean startDrawStroke(PenState ps) {
		return false;
	}

	@Override
	public boolean stepDrawStroke(PenState fromState, PenState toState) {
		if( fromState.x == toState.x && fromState.y == toState.y)
			return false;
		
		engine.setSurfaceSize(data.getWidth(), data.getHeight());

		float fromSize = stroke.getDynamics().getSize(fromState);
		float toSize = stroke.getDynamics().getSize(toState);
		
		
		// x y z w, size pressure
		float raw[] = new float[]{
			fromState.x, fromState.y, 0.0f, 1.0f, fromSize, fromState.pressure,
			toState.x, toState.y, 0.0f, 1.0f, toSize, toState.pressure,
		};
		FloatBuffer vertexBuffer = GLBuffers.newDirectFloatBuffer(raw);
		
		

		Graphics g = strokeLayer.getGraphics();
		
		return true;
	}
	
}
