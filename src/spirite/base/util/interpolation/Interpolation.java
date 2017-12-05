package spirite.base.util.interpolation;

import spirite.base.graphics.GraphicsContext;
import spirite.base.util.linear.Vec2;

import java.util.Arrays;
import java.util.List;

/**
 * A Package which contains a set of classes for interpolating data.
 *
 * TODO: Implement Bezier interpolation
 *
 * @author Rory Burks
 */
public class Interpolation {
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
}
