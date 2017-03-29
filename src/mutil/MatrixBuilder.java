package mutil;

import java.awt.geom.AffineTransform;

/**
 * A Package containing methods which construct certain frequently-used
 * Matrixes
 * 
 * @author Rory Burks
 */
public class MatrixBuilder {
	public static float[] orthagonalProjectionMatrix( 
			float d, float e,
			float f, float g,
			float near, float far)
	{
		return new float[] {
			2 / (float)(e - d), 0, 0, -(e + d)/(float)(e-d),
			0, 2 / (float)(f - g), 0, -(f + g)/(float)(f-g),
			0, 0, -2 / (float)(far - near), -(far + near)/(float)(far - near),
			0, 0, 0, 1
		};
	}
	
	/** Converts an AffineTransform into a Quaternion Transformation Matrix
	 * which can be fed into OpenGL to behave in the expected way.
	 * 
	 * !!NOTE since AWT Image Format has the Y-axis flipped relative to the 
	 * way OpenGL handles images (in AWT +y is down, in GL +y is up), 
	 * the Y-translation of the AffineTransform is flipped, so care must be
	 * taken when using this method for more general purposes. */
	public static float[] wrapAffineTransformAs4x4( AffineTransform trans) {
		return new float[] {
			(float) trans.getScaleX(), (float) trans.getShearX(),0, (float) trans.getTranslateX(),
			(float) trans.getShearY(), (float) trans.getScaleY(), 0, (float) -trans.getTranslateY(),
			0, 0, 1, 0,
			0, 0, 0, 1
		};
	}
}
