package spirite.base.util.interpolation;

import spirite.base.util.linear.Vec2;

import java.util.List;

/** Constructs a Polygon of degree N-1 which goes through the given N
 * points and uses that Polygon to interpolate the data.
 *
 * Lagrange Interpolations are a straightforward form of interpolation
 * that results in an infinitely differentiable curve (meaning it is
 * extremely smooth), but creates a curve which can have a lot of hills
 * and valleys, resulting in a very erratic-looking curve. */
public class LagrangeInterpolator
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
                    pi_coef2[k] = pi_coef[k] *(-p_j.getX());
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