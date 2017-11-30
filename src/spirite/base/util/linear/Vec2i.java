package spirite.base.util.linear;

/**
 * Created by Rory Burks on 4/28/2017.
 */

public class Vec2i {
    public final int x;
	public final int y;

    public Vec2i( int x, int y) {
        this.x = x;
        this.y = y;
    }
    public Vec2i( Vec2i Vec2i) {
        this.x = Vec2i.x;
        this.y = Vec2i.y;
    }

    public Vec2i sub( Vec2i rhs) {
        return new Vec2i( x - rhs.x, y - rhs.y);
    }

    public Vec2i add( Vec2i rhs) {
        return new Vec2i( x + rhs.x, y + rhs.y);
    }

    public int dot( Vec2i rhs) {
        return this.x *rhs.x + this.y * rhs.y;
    }

    @Override
    public boolean equals(Object obj) {
        if( obj == null) return false;
        if( obj instanceof  Vec2i) {
            Vec2i other = (Vec2i)obj;
            if( other.x == x && other.y == y)
                return true;
        }
        return false;
    }
}
