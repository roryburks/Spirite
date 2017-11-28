package spirite.base.util.glu;

import java.util.ArrayList;
import java.util.List;

import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUtessellator;
import com.jogamp.opengl.glu.GLUtessellatorCallback;

import spirite.base.graphics.gl.GLGeom.Primitive;
import spirite.base.util.compaction.FloatCompactor;

public class PolygonTesselater {
	public static Primitive tesselatePolygon(int[] x, int[] y, int count) {
		GLUtessellator tess = GLU.gluNewTess();
		GLUTCB callback = new GLUTCB();

		GLU.gluTessCallback(tess, GLU.GLU_TESS_VERTEX, callback);
		GLU.gluTessCallback(tess, GLU.GLU_TESS_BEGIN, callback);
		GLU.gluTessCallback(tess, GLU.GLU_TESS_END, callback);
		GLU.gluTessCallback(tess, GLU.GLU_TESS_ERROR, callback);
		GLU.gluTessCallback(tess, GLU.GLU_TESS_COMBINE, callback);

	    GLU.gluTessProperty(tess,
	        GLU.GLU_TESS_WINDING_RULE, 
	        GLU.GLU_TESS_WINDING_ODD);
	    GLU.gluTessBeginPolygon(tess, null);
	    GLU.gluTessBeginContour(tess);
	    for( int i=0; i < count; ++i) {
			double[] buffer = new double[] {x[i],y[i],0};
	    	GLU.gluTessVertex(tess, buffer, 0, buffer);
	    }
	    GLU.gluTessEndContour(tess);
	    GLU.gluTessEndPolygon(tess);
	    GLU.gluDeleteTess(tess);
		
		return callback.buildPrimitive();
	}
	
	public static Primitive tesselatePolygon( float[] x, float[] y, int count ) {
		GLUtessellator tess = GLU.gluNewTess();
		GLUTCB callback = new GLUTCB();

		GLU.gluTessCallback(tess, GLU.GLU_TESS_VERTEX, callback);
		GLU.gluTessCallback(tess, GLU.GLU_TESS_BEGIN, callback);
		GLU.gluTessCallback(tess, GLU.GLU_TESS_END, callback);
		GLU.gluTessCallback(tess, GLU.GLU_TESS_ERROR, callback);
		GLU.gluTessCallback(tess, GLU.GLU_TESS_COMBINE, callback);

	    GLU.gluTessProperty(tess,
	        GLU.GLU_TESS_WINDING_RULE, 
	        GLU.GLU_TESS_WINDING_ODD);
	    GLU.gluTessBeginPolygon(tess, null);
	    GLU.gluTessBeginContour(tess);
	    for( int i=0; i < count; ++i) {
			double[] buffer = new double[] {x[i],y[i],0};
	    	GLU.gluTessVertex(tess, buffer, 0, buffer);
	    }
	    GLU.gluTessEndContour(tess);
	    GLU.gluTessEndPolygon(tess);
	    GLU.gluDeleteTess(tess);
		
		return callback.buildPrimitive();
	}
	
	public static class GLUTCB implements GLUtessellatorCallback {
		private final FloatCompactor data = new FloatCompactor();
		private final List<Integer> types = new ArrayList<>();
		private final List<Integer> lengths = new ArrayList<>();
		private int currentLength = 0;
		
		@Override
		public void begin(int type) {
			types.add(type);
		}
		@Override public void combine(double[] coords, Object[] data, float[] weight, Object[] out) {
			out[0] = coords;
		}
		@Override public void edgeFlag(boolean arg0) {}

		@Override
		public void end() {
			lengths.add(currentLength);
			currentLength = 0;
		}
		@Override
		public void error(int errnum) {
			String estring;
			
			estring = (new GLU()).gluErrorString(errnum);
			System.err.println("Tessellation Error: " + estring);
			System.exit(0);
		}
		@Override
		public void vertex(Object arg0) {
			double[] d = (double[])arg0;
			data.add((float)d[0]);
			data.add((float)d[1]);
			++currentLength;
		}
		
		Primitive buildPrimitive() {
			int len = Math.min(types.size(), lengths.size());
			int[] ptypes = new int[len];
			int[] plengths = new int[len];
			for( int i=0; i<len; ++i) {
				ptypes[i] = types.get(i);
				plengths[i] = lengths.get(i);
			}
			
			
			return new Primitive( data.toArray(), new int[]{2}, ptypes, plengths);
		}

		@Override public void edgeFlagData(boolean arg0, Object arg1) {}
		@Override public void beginData(int type, Object polygonData) {}
		@Override public void combineData(double[] coords, Object[] data, float[] weight, Object[] out, Object arg4) {}
		@Override public void endData(Object arg0) {}
		@Override public void errorData(int arg0, Object arg1) {}
		@Override public void vertexData(Object arg0, Object arg1) {}
		
	}

}
