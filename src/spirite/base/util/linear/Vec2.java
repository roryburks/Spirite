package spirite.base.util.linear;

import com.hackoeur.jglm.support.FastMath;

/**
 * Created by Rory Burks on 4/28/2017.
 */

public class Vec2 {
    public float x;
	public float y;
	
	public Vec2() {}
    public Vec2( float x, float y) {
        this.x = x;
        this.y = y;
    }
    public Vec2( Vec2 vec2) {
        this.x = vec2.x;
        this.y = vec2.y;
    }

    public Vec2 sub( Vec2 rhs) {
        return new Vec2( x - rhs.x, y - rhs.y);
    }

    public Vec2 add( Vec2 rhs) {
        return new Vec2( x + rhs.x, y + rhs.y);
    }

    public Vec2 normalize() {
        float isr = FastMath.invSqrtFast( x*x + y*y);
        return new Vec2( this.x * isr, this.y * isr);
    }

    public float dot( Vec2 rhs) {
        return this.x *rhs.x + this.y * rhs.y;
    }
	public float cross(Vec2 rhs) {
		return x*rhs.y - y*rhs.x;
	}

    @Override
    public boolean equals(Object obj) {
        if( obj == null) return false;
        if( obj instanceof  Vec2) {
            Vec2 other = (Vec2)obj;
            if( other.x == x && other.y == y)
                return true;
        }
        return false;
    }
	public float getMag() {
		return FastMath.sqrtFast(x*x + y*y);
	}
	public Vec2 scalar(float f) {
		return new Vec2( x*f, y*f);
	}
}
