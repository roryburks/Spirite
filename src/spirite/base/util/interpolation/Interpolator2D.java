package spirite.base.util.interpolation;

import spirite.base.util.glmath.Vec2;

public interface Interpolator2D{
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
    
    public float getCurveLength();
    public void addPoint(float x, float y);
    public Vec2 eval(float t);
    public Interpolator2D.InterpolatedPoint evalExt(float t);
}