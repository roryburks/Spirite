package mutil;

import java.awt.geom.Point2D;
import java.util.List;

public class Interpolation {
	public static class LagrangeInterpolator {
		double[] coef;
		public LagrangeInterpolator( List<Point2D> points) {
			final int N = points.size();
			coef = new double[N];
			double[] pi_coef = new double[ N];
			double[] pi_coef2 = new double[ N];
			
			// Calculate the coefficience for the Lagrange polynomial
			for( int i=0; i<N; ++i) {
				double divisor = 1;
				
				// Calculate the divisor of the coefficient
				Point2D p_i = points.get(i);
				Point2D p_j;
				
				for( int j=0; j<N; ++j) {
					if( j == i) continue;
					p_j = points.get(j);
					divisor *= (p_i.getX() - p_j.getX());
				}
				
				// Calculate the denominator coefficients that p_i contribute to the 
				//	polynomial
				for( int j=0; j<N; ++j) {
					pi_coef[i] = 0;
					pi_coef2[i] = 0;
				}
				pi_coef[0] = 1;
				for( int j=0; j<N; ++j) {
					if( j == i) continue;
					p_j = points.get(j);
					
					// * (x - x_j)
					//	- x_j)
					for( int k=0; k<N; ++k){
						pi_coef2[k] = pi_coef[k] *(- p_j.getX());
					}
					// (x 
					for( int k=N-2; k>=0; --k){
						pi_coef[k+1] = pi_coef[k];
					}
					pi_coef[0]= 0;
					
					// combine the two
					for( int k=0; k<N; ++k){
						pi_coef[k] += pi_coef2[k];
					}
				}
				// Add the calculated coefficients to the final coefficients
				for( int j=0; j<N; ++j) {
					coef[j] += (pi_coef[j] * p_i.getY()) / divisor;
				}
			}
		}
		
		public double f( double x) {
			if( coef.length == 0) return 0;
			double ret = coef[0];
			double x_to_n = 1;
			
			for( int i=1; i<coef.length; ++i) {
				x_to_n *= x;
				ret += x_to_n * coef[i];
			}
			return ret;
		}
	}
}
