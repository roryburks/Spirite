package mutil;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.List;

import mutil.Interpolation.CubicSplineInterpolator2D;
import spirite.MDebug;
import spirite.MUtil;
import spirite.MDebug.WarningType;

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
		public Point2D eval(double t);
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
	
	// !!!!  DEBUG
	public static void _DEBUG_drawCurve(java.awt.Graphics g) {
        // Demonstrates Cubic Spline Interpolation
        List<Point2D> points = Arrays.asList(new Point2D[]{
        });
        CubicSplineInterpolator2D csi = new CubicSplineInterpolator2D(points, true);

        csi.addPoint(new Point2D.Double(0, 0));
    	csi.addPoint(new Point2D.Double(200, 20));
    	csi.addPoint(new Point2D.Double(260, 80));
    	csi.addPoint(new Point2D.Double(200, 200));
    	csi.addPoint(new Point2D.Double(100, 80));
        csi.addPoint(new Point2D.Double(50,50));
        csi.addPoint(new Point2D.Double(0,500));

        for( int i=0; i < csi.getNumPoints(); ++i) {
        	int dx = (int)Math.round(csi.getX(i));
        	int dy = (int)Math.round(csi.getY(i));
        	g.fillOval(dx-3, dy-3, 6, 6);
        }
        
        int ox = -999;
        int oy = -999;
        for( double d = 0; d < csi.getCurveLength(); d += 1) {
        	Point2D p = csi.eval(d);
        	int nx = (int) Math.round((float)p.getX());
        	int ny = (int) Math.round((float)p.getY());
    	
        	if( ox != -999) {
	    		g.setColor( new Color(
	            		(int) (Math.random()*255) , (int) (Math.random()*255), (int) (Math.random()*255)));
	    		g.drawLine(ox, oy, nx, ny);
        	}
        	ox = nx;
        	oy = ny;
        }
	}
	
	
	public static class CubicSplineInterpolator2D 
		implements Interpolator2D
	{
		private double kx[];	// x-differentials
		private double ky[];	// y-differentials
		private double x_[];	// x-values
		private double y_[];	// y-values
		private double t_[];	// t-values
		private int length = 0;
		private double distance = 0;
		
		private final boolean fast;

		/**
		 * 
		 * @param points
		 * @param fast	
		 * <li>If true, use Finite difference to get the slope vectors
		 * 	(i.e. takes the average of the two distance vectors to/from the points).
		 * <li>If false, it finds the slopes such that the second-degree derivative
		 * of the curve is 0 at each point (by solving the tridiagonal linear equation
		 * system)
		 * !!!!CURRENTLY UNIMPLEMENTED!!!!
		 */
		public CubicSplineInterpolator2D(List<Point2D> points, boolean fast) {
			length = (points == null)?0:points.size();
			
			int l = Math.max(length, 10);
			kx = new double[l];
			ky = new double[l];
			x_ = new double[l];
			y_ = new double[l];
			t_ = new double[l];
			distance = 0;
			
			if( length != 0) {
				x_[0] = points.get(0).getX();
				y_[0] = points.get(0).getY();
				t_[0] = 0;
				
			}
			
			for( int i=1; i<length; ++i) {
				x_[i] = points.get(i).getX();
				y_[i] = points.get(i).getY();
				t_[i] = distance + MUtil.distance(x_[i-1], y_[i-1], x_[i], y_[i]);
				distance = t_[i];
			}
			
			this.fast = fast;
			
			if( !fast)
				MDebug.handleWarning(WarningType.UNSUPPORTED, null, "Precise smoothing not implemented.");
				
			fastCalculateSlopes();
		}

		public double getCurveLength() {return this.distance;}
		public int getNumPoints() {return this.length;}
		public double getX(int n) {return x_[n];}
		public double getY(int n) {return y_[n];}

		public void addPoint(Point2D point) {
			if( kx.length <= length) expand(length+1);

			// Code could be made less verbose in by combining parts of the 
			//	different cases, but would be less readable
			x_[length] = point.getX();
			y_[length] = point.getY();
			if( length == 0) {
				t_[0] = 0;
			}
			else if( length == 1) {
				t_[1] = MUtil.distance(x_[0], y_[0], x_[1], y_[1]);
				double dt = t_[1] - t_[0];
				kx[1] = 0.25*(x_[1]-x_[0])/dt;
				ky[1] = 0.25*(y_[1]-y_[0])/dt;
				kx[0] = kx[1];
				ky[0] = ky[1];
			}
			else {
				x_[length] = point.getX();
				y_[length] = point.getY();
				t_[length] = t_[length-1] +
						MUtil.distance(x_[length-1], y_[length-1], x_[length], y_[length]);
	

				double dt1 = t_[length] - t_[length-1];
				double dt2 = t_[length-1] - t_[length-2];
				kx[length-1] = 0.5*((x_[length]-x_[length-1])/dt1 + (x_[length-1]-x_[length-2])/dt2);
				ky[length-1] = 0.5*((y_[length]-y_[length-1])/dt1 + (y_[length-1]-y_[length-2])/dt2);
					
				kx[length] = 0.25*(x_[length]-x_[length-1])/dt1;
				ky[length] = 0.25*(y_[length]-y_[length-1])/dt1;
			}

			distance = t_[length];
			length++;
		}
		
		/** Expands the internal arrays in order to accomidate the new length. */
		private void expand(int new_length) {
			if( kx.length >= new_length) return;
			
			int l = (length==0)?new_length:kx.length;
			
			// Expand by 50% at a time (similar to ArrayList)
			// TODO: Not using ArrayList to avoid boxing/unboxing bloar, but
			//	should still encapsulate this stuff in a custom primitive list
			//	object
			while( l < new_length)
				l = (l*3+1)/2;

			double buff[] = new double[l];
			kx = buff;
			buff = new double[l];
			System.arraycopy(ky, 0, buff, 0, length);
			ky = buff;
			buff = new double[l];
			System.arraycopy(x_, 0, buff, 0, length);
			x_ = buff;
			buff = new double[l];
			System.arraycopy(y_, 0, buff, 0, length);
			y_ = buff;
			buff = new double[l];
			System.arraycopy(t_, 0, buff, 0, length);
			t_ = buff;
		}
		
		/** Calculates the slopes using the simple Finite-Distance method which
		 * just takes the average of the vector to and away from a middle point
		 */
		private void fastCalculateSlopes() {
			if( length <= 1) return;

			// Note: Enpoint weighting is suppressed a little to avoid wonky
			//	start/end curves
			double dt = t_[1] - t_[0];
			kx[0] = 0.25*(x_[1] - x_[0])/dt;
			ky[0] = 0.25*(y_[1] - y_[0])/dt;
			
			int i = 0;
			for( i = 1; i < length-1; ++i) {
				double dt1 = t_[i+1] - t_[i];
				double dt2 = t_[i] - t_[i-1];
				kx[i] = 0.5*((x_[i+1]-x_[i])/dt1 + (x_[i]-x_[i-1])/dt2);
				ky[i] = 0.5*((y_[i+1]-y_[i])/dt1 + (y_[i]-y_[i-1])/dt2);
			}
			dt = t_[i] - t_[i-1];
			kx[i] = 0.25*(x_[i] - x_[i-1])/dt;
			ky[i] = 0.25*(y_[i] - y_[i-1])/dt;
		}
		

		@Override
		public Point2D eval(double t) {
			if( kx.length == 0) return new Point2D.Double(0,0);
			
			if( t <= 0) return new Point2D.Double(x_[0], y_[0]);
			if( t >= distance) return new Point2D.Double(x_[kx.length-1], y_[kx.length-1]);
			
			int i = 0;
			while( t > t_[i] &&  ++i < kx.length);
			if( i == kx.length) return new Point2D.Double(x_[kx.length-1], y_[kx.length-1]);
			
			
			double dt = t_[i]-t_[i-1];
			double n = (t - t_[i-1])/dt;
			
			double a_x = kx[i-1]*dt - (x_[i] - x_[i-1]);
			double b_x =-kx[i]*dt + x_[i] - x_[i-1];
			double a_y = ky[i-1]*dt - (y_[i] - y_[i-1]);
			double b_y =-ky[i]*dt + y_[i] - y_[i-1];
			
			double qx = (1-n)*x_[i-1] + n*x_[i] + n*(1-n)*(a_x*(1-n)+b_x*n);
			double qy = (1-n)*y_[i-1] + n*y_[i] + n*(1-n)*(a_y*(1-n)+b_y*n);
			
			return new Point2D.Double(qx, qy);
		}
		
	}
	
	/**
	 * CubicSplineInterpolator2DFixed differs from CubicSplineInterpolator2D
	 * in that each point represents a fixed t-length of 1 whereas in 
	 * CubicSplineInterpolator2D, the t-length is determined by distancew
	 */
	public static class CubicSplineInterpolator2DFixed
		implements Interpolator2D
	{
		private double kx[];
		private double ky[];
		private double x_[];
		private double y_[];
		private int length;
		
		public CubicSplineInterpolator2DFixed(List<Point2D> points, boolean fast) {
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
		public Point2D eval(double t) {
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
