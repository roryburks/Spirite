package spirite.base.util.interpolation;

import java.util.List;

import spirite.base.util.MUtil;
import spirite.base.util.glmath.Vec2;

/**
 * CubicSplineInterpolator2D is a two-dimensional curve interpolator which
 * uses Cubic Hermite Spline interpolation from a given number of points
 * to interpolate a 2D curve.  It maintains two separate Hermite Splines:
 * one for the X-axis, and another for the Y-axis, which are both traversed
 * along a common axis (t).
 *
 * The range of t is determined by the length of the line segments which
 * make up the key-points.
 */
public class CubicSplineInterpolator2D
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
    private boolean extrapolate = false;

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

    public CubicSplineInterpolator2D(float[] xs, float[] ys, boolean fast) {
        length = Math.min(xs == null ? 0 : xs.length, ys == null ? 0 : ys.length);

        int l = Math.max(length, 10);
        kx = new float[l];
        ky = new float[l];
        x_ = new float[l];
        y_ = new float[l];
        t_ = new float[l];
        distance = 0;

        if( length != 0) {
            x_[0] = xs[0];
            y_[0] = ys[0];
            t_[0] = 0;

        }

        for( int i=1; i<length; ++i) {
            x_[i] = xs[i];
            y_[i] = ys[i];
            t_[i] = (float) (distance + MUtil.distance(x_[i-1], y_[i-1], x_[i], y_[i]));
            distance = t_[i];
        }

        this.fast = fast;

//            if( !fast)
//                MDebug.handleWarning(MDebug.WarningType.UNSUPPORTED, null, "Precise smoothing not implemented.");

        fastCalculateSlopes();
	}
    

    public boolean isExtrapolating() { return extrapolate;}
    public void setExtrapolating( boolean extrapolate) {this.extrapolate = extrapolate;}
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
        if( length == 0) return new Vec2(0,0);

        if( t <= 0 && !extrapolate)return new Vec2(x_[0], y_[0]);
        if( t >= distance && !extrapolate) return new Vec2(x_[length-1], y_[length-1]);

        int i = 0;
        while( t > t_[i] &&  ++i < length);
        if( i == length && !extrapolate) return new Vec2(x_[length-1], y_[length-1]);

        if( i == 0) {
        	Vec2 p1 = _eval(0.075f, 0, 1);
        	
        	float dt = (float)MUtil.distance(p1.x, p1.y, x_[0], y_[0]);
        	float d = t / dt;
        	
        	return new Vec2( x_[0] + d * (p1.x - x_[0]),y_[0] + d * (p1.y - y_[0]));
        }
        if( i == length) {

        	Vec2 p1 = _eval(0.925f, length-2, length-1);
        	
        	float dt = (float)MUtil.distance(p1.x, p1.y, x_[length-1], y_[length-1]);
        	float d = (t - distance) / dt + 1;
        	
        	return new Vec2( x_[length-1] + d * (x_[length-1]-p1.x ),y_[length-1] + d * ( y_[length-1]- p1.y));
        }
        
        return _eval((t - t_[i-1])/(t_[i]-t_[i-1]), i-1, i);
    }
    private Vec2 _eval(float n, int i1, int i2) {
        float dt = t_[i2]-t_[i1];
        float a_x = kx[i1]*dt - (x_[i2] - x_[i1]);
        float b_x =-kx[i2]*dt + x_[i2] - x_[i1];
        float a_y = ky[i1]*dt - (y_[i2] - y_[i1]);
        float b_y =-ky[i2]*dt + y_[i2] - y_[i1];

        float qx = (1-n)*x_[i1] + n*x_[i2] + n*(1-n)*(a_x*(1-n)+b_x*n);
        float qy = (1-n)*y_[i1] + n*y_[i2] + n*(1-n)*(a_y*(1-n)+b_y*n);

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