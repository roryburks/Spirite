package mutil;

public class MatrixBuilder {
	public static float[] orthagonalProjectionMatrix( 
			int left, int right,
			int top, int bottom,
			int near, int far)
	{
		return new float[] {
			2 / (float)(right - left), 0, 0, -(right + left)/(float)(right-left),
			0, 2 / (float)(top - bottom), 0, -(top + bottom)/(float)(top-bottom),
			0, 0, -2 / (float)(far - near), -(far + near)/(float)(far - near),
			0, 0, 0, 1
		};
	}
}
