package spirite.base.util;

import java.awt.geom.AffineTransform;

/**
 * A Package containing methods which construct certain frequently-used
 * Matrixes
 * 
 * @author Rory Burks
 */
public class MatrixBuilder {
	/** Creates a transform matrix representing an Orthagonal Projection
	 * (A flat, rectangular projection in XY coordinates). */
	public static float[] orthagonalProjectionMatrix( 
			float left, float right,
			float bottom, float top,
			float near, float far)
	{
		return new float[] {
			2 / (float)(right - left), 0, 0, -(right + left)/(float)(right-left),
			0, 2 / (float)(bottom - top), 0, -(bottom + top)/(float)(bottom-top),
			0, 0, -2 / (float)(far - near), -(far + near)/(float)(far - near),
			0, 0, 0, 1
		};
	}
	
	/** Converts a 3x3 AffineTransform into a Quaternion Transformation Matrix
	 * which can be fed into OpenGL to behave in the expected way. */
	public static float[] wrapAffineTransformAs4x4( AffineTransform trans) {
		return new float[] {
			(float) trans.getScaleX(), (float) trans.getShearX(),0, (float) trans.getTranslateX(),
			(float) trans.getShearY(), (float) trans.getScaleY(), 0, (float) trans.getTranslateY(),
			0, 0, 1, 0,
			0, 0, 0, 1
		};
	}
}
