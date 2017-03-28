package mutil;

import java.awt.geom.AffineTransform;

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
	
	public static float[] wrapAffineTransformAs4x4( AffineTransform trans) {
		return new float[] {
			(float) trans.getScaleX(), (float) trans.getShearX(),0, (float) trans.getTranslateX(),
			(float) trans.getShearY(), (float) trans.getScaleY(), 0, (float) trans.getTranslateY(),
			0, 0, 1, 0,
			0, 0, 0, 1
		};
	}
}
