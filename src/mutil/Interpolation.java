package mutil;

import java.awt.geom.Point2D;
import java.util.List;

/**
 * A Package which contains a set of classes for interpolating data.
 * 
 * TODO: Implement Bezier and CubicSpline interpolation
 * 
 * @author Rory Burks
 */
public class Interpolation {
	public static interface Interpolator{
		public double f(double t);
	}
	public static interface Interpolator2D{
		public Point2D f(double t);
	}
	public static class CubicSplineInterpolator
		implements Interpolator
	{

		private final double k[];
		private final double x_[];
		private final double y_[];

		public CubicSplineInterpolator(List<Point2D> points, boolean fast) {
			k = new double[points.size()];
			x_ = new double[points.size()];
			y_ = new double[points.size()];

			for( int i=0; i<k.length; ++i) {
				x_[i] = points.get(i).getX();
				y_[i] = points.get(i).getY();
			}
			k[0] = y_[1] - y_[0] / (x_[1] - y_[0]);
			int i;
			for( i=1; i<k.length-1; ++i) {
				double a1 = y_[i+1] - y_[i] / (x_[i+1] - y_[i]);
				double a2 = y_[i] - y_[i-1] / (x_[i] - y_[i-1]);
				k[i] = (a1+a2)/2;
			}
			k[i] = y_[i] - y_[i-1] / (x_[i] - y_[i-1]);
		}
		@Override
		public double f(double t) {
			int i=0;
			while( x_[i] < t) ++i;
			
			double a = k[i-1] - (x_[i] - x_[i-1]);
			double b =-k[i] + x_[i] - x_[i-1];
			
			double n = (t - x_[i-1])/(x_[i]-x_[i-1]);
			double q = (1-n)*y_[i] + n*y_[i+1] + n*(1-n)*(a*(1-n)+b*n);
			
			return q;
		}
		
	}
	
	/*
        
     // Demonstrates Cubic Spline Interpolation
        List<Point2D> points = Arrays.asList(new Point2D[]{
            	new Point2D.Double(0, 0),
            	new Point2D.Double(200, 20),
            	new Point2D.Double(260, 80),
            	new Point2D.Double(200, 200),
            	new Point2D.Double(100, 80),
        });
        CubicSplineInterpolator2D csi = new CubicSplineInterpolator2D(points, true);

        csi.addPoint(new Point2D.Double(50,50), true);
        csi.addPoint(new Point2D.Double(0,500), true);

        
        int x[] = new int[601];
        int y[] = new int[601];
        int i=0;
        for( double d = 0; d < csi.getLength(); d += 0.1) {
        	Point2D p = csi.f(d);
        	x[i] = (int) Math.round((float)p.getX());
        	y[i] = (int) Math.round((float)p.getY());
        	if( i != 0) {
        		g.setColor( new Color(
                		(int) (Math.random()*255) , (int) (Math.random()*255), (int) (Math.random()*255)));
        		g.drawLine(x[i], y[i], x[i-1], y[i-1]);
        		
        		System.out.println((x[i] - x[i-1]) + ":" + (y[i]-y[i-1]) + ":" + MUtil.distance(x[i], y[i],x[i-1],y[i-1]));
        	}
        	++i;
        }*/
	
	public static class CubicSplineInterpolator2D
		implements Interpolator2D
	{
		private double kx[];
		private double ky[];
		private double x_[];
		private double y_[];
		private int length;
		
		public CubicSplineInterpolator2D(List<Point2D> points, boolean fast) {
			length = points.size();
			kx = new double[length];
			ky = new double[length];
			x_ = new double[length];
			y_ = new double[length];
			for( int i=0; i<length; ++i) {
				x_[i] = points.get(i).getX();
				y_[i] = points.get(i).getY();
			}
			
			fastCalculateSlopes();
		}
		
		public void addPoint(Point2D point, boolean fast) {
			if( length >= kx.length) {
				// Expand the internal arrays as needed
				int l = (kx.length*3 + 1)/2;
				double t[] = new double[l];
				System.arraycopy(kx, 0, t, 0, length);
				kx = t;
				t = new double[l];
				System.arraycopy(ky, 0, t, 0, length);
				ky = t;
				t = new double[l];
				System.arraycopy(x_, 0, t, 0, length);
				x_ = t;
				t = new double[l];
				System.arraycopy(y_, 0, t, 0, length);
				y_ = t;
			}
			
			x_[length] = point.getX();
			y_[length] = point.getY();

			
			if( length >= 2) {
				kx[length-1] = (x_[length] - x_[length-2])/2;
				ky[length-1] = (y_[length] - y_[length-2])/2;
			}
			kx[length] = (x_[length]-x_[length-1])/4;
			ky[length] = (y_[length]-y_[length-1])/4;
			
			length++;
		}
		
		public int getLength() {return this.length;}
		
		private void fastCalculateSlopes() {
			if( length <= 1) return;
			
			kx[0] = (x_[1] - x_[0])/4;
			ky[0] = (y_[1] - y_[0])/4;
			
			int i = 0;
			for( i = 1; i < length-1; ++i) {
				kx[i] = (x_[i+1]-x_[i-1])/2;
				ky[i] = (y_[i+1]-y_[i-1])/2;
			}
			kx[i] = (x_[i] - x_[i-1])/4;
			ky[i] = (y_[i] - y_[i-1])/4;
		}

		@Override
		public Point2D f(double t) {
			if( kx.length == 0) return new Point2D.Double(0,0);
			
			if( t <= 0) return new Point2D.Double(x_[0], y_[0]);
			if( t >= length-1) return new Point2D.Double(x_[kx.length-1], y_[kx.length-1]);
			
			int i = (int)t;
			
			
			double n = t - i;
			double a_x = kx[i] - (x_[i+1] - x_[i]);
			double b_x =-kx[i+1] + x_[i+1] - x_[i];
			
			double a_y = ky[i] - (y_[i+1] - y_[i]);
			double b_y =-ky[i+1] + y_[i+1] - y_[i];
			double qx = (1-n)*x_[i] + n*x_[i+1] + n*(1-n)*(a_x*(1-n)+b_x*n);
			double qy = (1-n)*y_[i] + n*y_[i+1] + n*(1-n)*(a_y*(1-n)+b_y*n);
			
			return new Point2D.Double(qx, qy);
		}
	
	}
	
	/** Constructs a Polygon of degree N-1 which goes through the given N
	 * points and uses that Polygon to interpolate the data. */
	public static class LagrangeInterpolator 
		implements Interpolator
	{
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
