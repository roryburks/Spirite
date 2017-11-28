package spirite.base.util.interpolation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import spirite.base.util.MUtil;
import spirite.base.util.linear.Vec2;

/**
 * CubicSplineInterpolator uses Cubic Hermite Spline Interpolation to
 * construct an interpolation function, f(x) made up of piecewise-cubic
 * segments.
 */
public class CubicSplineInterpolator
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