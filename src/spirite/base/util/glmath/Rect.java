package spirite.base.util.glmath;

public class Rect {
	public int x, y, width, height;

	public Rect( int width, int height) {
		x = y = 0;
		this.width = width;
		this.height = height;
	}
	public Rect( int x, int y, int width, int height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	public Rect(Rect other) {
		this.x = other.x;
		this.y = other.y;
		this.width = other.width;
		this.height = other.height;
	}
	public Rect(Vec2i vec2i) {
		x = y = 0;
		this.width = vec2i.x;
		this.height = vec2i.y;
	}
	public boolean isEmpty() {
		return width <= 0 || height <= 0;
	}

	public Rect intersection(Rect other) {
		int x1 = Math.max( x,  other.x);
		int y1 = Math.max( y, other.y);
		int x2 = Math.min( x+width, other.x+other.width);
		int y2 = Math.min( y+height, other.y+other.height);
		return new Rect( x1, y1, x2-x1, y2-y1);
	}
	public boolean contains(int x2, int y2) {
		if( width <= 0 || height <= 0) return false;
		
		return !( x2 < x || y2 < y || x2 > x+width || y2 > y+height);
	}
	public Rect union(Rect rect) {
		if( rect == null || rect.isEmpty()) return new Rect(this);
		if( isEmpty()) return new Rect(rect);
		
		return new Rect(
				Math.min( x, rect.x),
				Math.min( y, rect.y),
				Math.max( x + width, rect.x + rect.width),
				Math.max( y + height, rect.y + rect.height));
	}
}
