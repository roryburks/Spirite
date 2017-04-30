package spirite.base.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import spirite.base.graphics.GraphicsContext;
import spirite.base.util.glmath.Vec2;

/**
 * A Package which contains a set of classes for interpolating data.
 *
 * TODO: Implement Bezier interpolation
 *
 * @author Rory Burks
 */
public class Interpolation {
    public static interface Interpolator{
        public float eval(float t);
    }


    public static class InterpolatedPoint {
        public final float x, y, lerp;
        public final int left, right;
        public InterpolatedPoint( float x, float y, float lerp, int left, int right) {
            this.x = x;
            this.y = y;
            this.lerp = lerp;
            this.left = left;
            this.right = right;
        }
    }
    public static interface Interpolator2D{
        public float getCurveLength();
        public void addPoint(float x, float y);
        public Vec2 eval(float t);
        public InterpolatedPoint evalExt(float t);
    }

    /**
     * CubicSplineInterpolator uses Cubic Hermite Spline Interpolation to
     * construct an interpolation function, f(x) made up of piecewise-cubic
     * segments.
     */
    public static class CubicSplineInterpolator
            implements Interpolator
    {
        private final float k[];
        private final float x_[];
        private final float y_[];

        private final boolean spatial;

        /**
         *
         * @param points_
         * @param fast
         * @param spatial spatial weighting weights the point slopes by
         * the total distance between two points, not just the X-distance.
         * Produces a result very similar (though not identical) to a 2D
         * Cubic Spline that only has points with strictly increasing X values.
         */
        public CubicSplineInterpolator(List<Vec2> points_, boolean fast, boolean spatial) {
            this.spatial = spatial;

            // Sorts the points by X
            List<Vec2> points = new ArrayList<>(points_);

            Collections.sort(
                    points,
                    new Comparator<Vec2>() {
                        @Override
                        public int compare(Vec2 o1, Vec2 o2) {
                            float d = o1.x - o2.x;
                            return (int) Math.signum(d);
                        }
                    }
            );

            k = new float[points.size()];
            x_ = new float[points.size()];
            y_ = new float[points.size()];

            for( int i=0; i< points.size(); ++i){
                Vec2 p = points.get(i);
                x_[i] = p.x;
                y_[i] = p.y;
            }

            fastCalculateSlopes();
        }

        private void fastCalculateSlopes(){
            if( k.length <= 1) return;

            // Note: Enpoint weighting is suppressed a little to avoid wonky
            //	start/end curves

            k[0] = (y_[1] - y_[0])/(x_[1]-x_[0]);

            int i = 0;
            for( i = 1; i < k.length-1; ++i) {
                if( spatial) {
                    float d1 = (float) MUtil.distance(x_[i], y_[i], x_[i+1], y_[i+1]);
                    float d2 = (float) MUtil.distance(x_[i-1], y_[i-1], x_[i], y_[i]);

                    k[i] = ((y_[i+1]-y_[i])/d1 + (y_[i]-y_[i-1])/d2) /
                            ((x_[i+1]-x_[i])/d1 + (x_[i]-x_[i-1])/d2);
                }
                else {

                    k[i] = 0.5f*((y_[i+1]-y_[i])/(x_[i+1]-x_[i]) +
                            (y_[i]-y_[i-1])/(x_[i]-x_[i-1]));
                }
            }
            k[i] = (y_[i] - y_[i-1])/(x_[i]-x_[i-1]);
        }

        public int getNumPoints() {return k.length;}
        public float getX(int n) {return x_[n];}
        public float getY(int n) {return y_[n];}

        @Override
        public float eval(float t) {
            if( k.length == 0) return 0;


            if( t <= x_[0]) return y_[0];
            if( t >= x_[k.length-1]) return y_[k.length-1];

            int i = 0;
            while( t > x_[i] &&  ++i < k.length);
            if( i == k.length) return y_[k.length-1];


            float dx = x_[i]-x_[i-1];
            float n = (t - x_[i-1])/dx;

            float a = k[i-1]*dx - (y_[i] - y_[i-1]);
            float b =-k[i]*dx + (y_[i] - y_[i-1]);

            return  (1-n)*y_[i-1] + n*y_[i] + n*(1-n)*(a*(1-n)+b*n);
        }

    }

    // !!!!  DEBUG
    public static void _DEBUG_drawCurve(GraphicsContext gc) {
        // Demonstrates Cubic Spline Interpolation
        List<Vec2> points = Arrays.asList(new Vec2[]{});
        CubicSplineInterpolator2D csi = new CubicSplineInterpolator2D(points, true);

        csi.addPoint(0, 0);
        csi.addPoint(200, 20);
        csi.addPoint(260, 80);
        csi.addPoint(200, 200);
        csi.addPoint(100, 80);
        csi.addPoint(50,50);
        csi.addPoint(0,500);

        for( int i=0; i < csi.getNumPoints(); ++i) {
            int dx = (int)Math.round(csi.getX(i));
            int dy = (int)Math.round(csi.getY(i));
            gc.fillOval(dx-3, dy-3, 6, 6);
        }

        int ox = -999;
        int oy = -999;
        for( float d = 0; d < csi.getCurveLength(); d += 1) {
            Vec2 p = csi.eval(d);
            int nx = (int) Math.round(p.x);
            int ny = (int) Math.round(p.y);

            if( ox != -999) {
//                gc.setColor( Colors.toColor( 255, (int) (Math.random()*255) , (int) (Math.random()*255), (int) (Math.random()*255)));
                gc.drawLine(ox, oy, nx, ny);
            }
            ox = nx;
            oy = ny;
        }
    }


    /**
     * CubicSplineInterpolator2D is a two-dimensional curve interpolator which
     * uses Cubic Hermite Spline interpolation from a given number of points
     * to interpolate a 2D curve.  It maintains two seperate Hermite Splines:
     * one for the X-axis, and another for the Y-axis, which are both traversed
     * along a common axis (t).
     *
     * The range of t is determined by the length of the line segments which
     * make up the key-points.
     */
    public static class CubicSplineInterpolator2D
            implements Interpolator2D
    {
        private float kx[];	// x-differentials
        private float ky[];	// y-differentials
        private float x_[];	// x-values
        private float y_[];	// y-values
        private float t_[];	// t-values
        private int length = 0;
        private float distance = 0;

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
        public CubicSplineInterpolator2D(List<Vec2> points, boolean fast)
        {
            length = (points == null)?0:points.size();

            int l = Math.max(length, 10);
            kx = new float[l];
            ky = new float[l];
            x_ = new float[l];
            y_ = new float[l];
            t_ = new float[l];
            distance = 0;

            if( length != 0) {
                x_[0] = points.get(0).x;
                y_[0] = points.get(0).y;
                t_[0] = 0;

            }

            for( int i=1; i<length; ++i) {
                x_[i] = points.get(i).x;
                y_[i] = points.get(i).y;
                t_[i] = (float) (distance + MUtil.distance(x_[i-1], y_[i-1], x_[i], y_[i]));
                distance = t_[i];
            }

            this.fast = fast;

//            if( !fast)
//                MDebug.handleWarning(MDebug.WarningType.UNSUPPORTED, null, "Precise smoothing not implemented.");

            fastCalculateSlopes();
        }

        public float getCurveLength() {return this.distance;}
        public int getNumPoints() {return this.length;}
        public float getX(int n) {return x_[n];}
        public float getY(int n) {return y_[n];}

        public void addPoint(float px, float py) {
            if( kx.length <= length) expand(length+1);

            // Code could be made less verbose in by combining parts of the
            //	different cases, but would be less readable
            x_[length] = px;
            y_[length] = py;
            if( length == 0) {
                t_[0] = 0;
            }
            else if( length == 1) {
                t_[1] = (float) MUtil.distance(x_[0], y_[0], x_[1], y_[1]);
                float dt = t_[1] - t_[0];
                kx[1] = 0.25f*(x_[1]-x_[0])/dt;
                ky[1] = 0.25f*(y_[1]-y_[0])/dt;
                kx[0] = kx[1];
                ky[0] = ky[1];
            }
            else {
                x_[length] = px;
                y_[length] = py;
                t_[length] = t_[length-1] +
                        (float)MUtil.distance(x_[length-1], y_[length-1], x_[length], y_[length]);

                float dt1 = t_[length] - t_[length-1];
                float dt2 = t_[length-1] - t_[length-2];
                kx[length-1] = 0.5f*((x_[length]-x_[length-1])/dt1 + (x_[length-1]-x_[length-2])/dt2);
                ky[length-1] = 0.5f*((y_[length]-y_[length-1])/dt1 + (y_[length-1]-y_[length-2])/dt2);

                kx[length] = 0.25f*(x_[length]-x_[length-1])/dt1;
                ky[length] = 0.25f*(y_[length]-y_[length-1])/dt1;
            }

            distance = t_[length];
            length++;
        }

        /** Expands the internal arrays in order to accommodate the new length. */
        private void expand(int new_length) {
            if( kx.length >= new_length) return;

            int l = (length==0)?new_length:kx.length;

            // Expand by 50% at a time (similar to ArrayList)
            // TODO: Not using ArrayList to avoid boxing/unboxing bloar, but
            //	should still encapsulate this stuff in a custom primitive list
            //	object
            while( l < new_length)
                l = (l*3+1)/2;

            float buff[] = new float[l];
            System.arraycopy(kx, 0, buff, 0, length);
            kx = buff;
            buff = new float[l];
            System.arraycopy(ky, 0, buff, 0, length);
            ky = buff;
            buff = new float[l];
            System.arraycopy(x_, 0, buff, 0, length);
            x_ = buff;
            buff = new float[l];
            System.arraycopy(y_, 0, buff, 0, length);
            y_ = buff;
            buff = new float[l];
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
            float dt = t_[1] - t_[0];
            kx[0] = 0.25f*(x_[1] - x_[0])/dt;
            ky[0] = 0.25f*(y_[1] - y_[0])/dt;

            int i = 0;
            for( i = 1; i < length-1; ++i) {
                float dt1 = t_[i+1] - t_[i];
                float dt2 = t_[i] - t_[i-1];
                kx[i] = 0.5f*((x_[i+1]-x_[i])/dt1 + (x_[i]-x_[i-1])/dt2);
                ky[i] = 0.5f*((y_[i+1]-y_[i])/dt1 + (y_[i]-y_[i-1])/dt2);

            }
            dt = t_[i] - t_[i-1];
            kx[i] = 0.25f*(x_[i] - x_[i-1])/dt;
            ky[i] = 0.25f*(y_[i] - y_[i-1])/dt;
        }


        @Override
        public Vec2 eval(float t) {
            if( kx.length == 0) return new Vec2(0,0);

            if( t <= 0) return new Vec2(x_[0], y_[0]);
            if( t >= distance) return new Vec2(x_[kx.length-1], y_[kx.length-1]);

            int i = 0;
            while( t > t_[i] &&  ++i < kx.length);
            if( i == kx.length) return new Vec2(x_[kx.length-1], y_[kx.length-1]);


            float dt = t_[i]-t_[i-1];
            float n = (t - t_[i-1])/dt;

            float a_x = kx[i-1]*dt - (x_[i] - x_[i-1]);
            float b_x =-kx[i]*dt + x_[i] - x_[i-1];
            float a_y = ky[i-1]*dt - (y_[i] - y_[i-1]);
            float b_y =-ky[i]*dt + y_[i] - y_[i-1];

            float qx = (1-n)*x_[i-1] + n*x_[i] + n*(1-n)*(a_x*(1-n)+b_x*n);
            float qy = (1-n)*y_[i-1] + n*y_[i] + n*(1-n)*(a_y*(1-n)+b_y*n);

            return new Vec2(qx, qy);
        }
        @Override
        public InterpolatedPoint evalExt(float t) {
            if( kx.length == 0) return new InterpolatedPoint(0, 0, 0, 0, 0);

            if( t <= 0) return new InterpolatedPoint(x_[0], y_[0], 0, 0, 1);
            if( t >= distance) return new InterpolatedPoint(x_[kx.length-1], y_[kx.length-1], 1, kx.length-2, kx.length-1);

            int i = 0;
            while( t > t_[i] &&  ++i < kx.length);
            if( i == kx.length) return new InterpolatedPoint(x_[kx.length-1], y_[kx.length-1], 1, kx.length-2, kx.length-1);


            float dt = t_[i]-t_[i-1];
            float n = (t - t_[i-1])/dt;

            float a_x = kx[i-1]*dt - (x_[i] - x_[i-1]);
            float b_x =-kx[i]*dt + x_[i] - x_[i-1];
            float a_y = ky[i-1]*dt - (y_[i] - y_[i-1]);
            float b_y =-ky[i]*dt + y_[i] - y_[i-1];

            float qx = (1-n)*x_[i-1] + n*x_[i] + n*(1-n)*(a_x*(1-n)+b_x*n);
            float qy = (1-n)*y_[i-1] + n*y_[i] + n*(1-n)*(a_y*(1-n)+b_y*n);

            return new InterpolatedPoint(qx, qy, n, i-1, i);
        }

    }


    /** Constructs a Polygon of degree N-1 which goes through the given N
     * points and uses that Polygon to interpolate the data.
     *
     * Lagrange Interpolations are a straightforward form of interpolation
     * that results in an infinitely differentiable curve (meaning it is
     * extremely smooth), but creates a curve which can have a lot of hills
     * and valleys, resulting in a very erratic-looking curve. */
    public static class LagrangeInterpolator
            implements Interpolator
    {
        float[] coef;
        public LagrangeInterpolator( List<Vec2> points) {
            final int N = points.size();
            coef = new float[N];
            float[] pi_coef = new float[ N];
            float[] pi_coef2 = new float[ N];

            // Calculate the coefficience for the Lagrange polynomial
            for( int i=0; i<N; ++i) {
                float divisor = 1;

                // Calculate the divisor of the coefficient
                Vec2 p_i = points.get(i);
                Vec2 p_j;

                for( int j=0; j<N; ++j) {
                    if( j == i) continue;
                    p_j = points.get(j);
                    divisor *= (p_i.x - p_j.x);
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
                        pi_coef2[k] = pi_coef[k] *(- p_j.x);
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
                    coef[j] += (pi_coef[j] * p_i.y) / divisor;
                }
            }
        }

        public float eval( float x) {
            if( coef.length == 0) return 0;
            float ret = coef[0];
            float x_to_n = 1;

            for( int i=1; i<coef.length; ++i) {
                x_to_n *= x;
                ret += x_to_n * coef[i];
            }
            return ret;
        }
    }
}
