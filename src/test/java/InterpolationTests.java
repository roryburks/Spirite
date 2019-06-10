package sjunit.spirite.base.util;

import org.junit.Test;
import rb.jvm.vectrix.interpolation.CubicSplineInterpolatorND;

public class InterpolationTests {

	@Test
	public void TestCubicND() {
		float[][] points = new float[][] {
			{0, 0, 0, 0, 1},
			{0.25f, 0.25f, 0.25f, 0.25f, 2},
			{1, 1, 1, 1, 3},
		};
		
		
		CubicSplineInterpolatorND csind1 = new CubicSplineInterpolatorND(points, 4, 3, false);
		CubicSplineInterpolatorND csind2 = new CubicSplineInterpolatorND(null, 4, 0, false);
		
		for(int i=0; i < points.length; ++i) {
			csind2.addPoint(points[i]);
		}
		

		//printF(csind.eval(0));
		//printF(csind.eval(0.5f));
		verifySame(csind1.eval(1), csind2.eval(1));
		verifySame(csind1.eval(1.5f), csind2.eval(1.5f));
		verifySame(csind1.eval(1.75f), csind2.eval(1.75f));
		verifySame(csind1.eval(2), csind2.eval(2));
		verifySame(csind1.eval(2.11f), csind2.eval(2.11f));
		verifySame(csind1.eval(3), csind2.eval(3));
	}
	
	private void verifySame( float[] f1, float[]f2) {
		assert( f1.length == f2.length);
		for( int i=0; i < f1.length; ++i) {
			System.out.println(f1[i] + "," + f2[i]);
			//assert( f1[i] == f2[i]);
		}
	}
	
	public void printF(float[] f) {
		for( int i=0; i < f.length; ++i) {
			System.out.print(f[i] + " ");
		}
		System.out.println();
	}
}
