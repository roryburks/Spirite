package spirite.base.util;

/**
 * Created by Guy on 4/29/2017.
 */

public class Colors {
    public static final int BLACK = 0xFF000000;
    public static final int DARK_GRAY = 0xFF404040;
    public static final int GRAY = 0xFF808080;
    public static final int LIGHT_GRAY = 0xFFC0C0C0;
    public static final int WHITE = 0xFFFFFFFF;
    public static final int RED = 0xFFFF0000;
    public static final int BLUE = 0xFF00FF00;
    public static final int GREEN = 0xFF0000FF;
    public static final int CYAN = 0xFF0000FF;
    public static final int MAGENTA = 0xFFFF00FF;
    public static final int YELLOW = 0xFFFFFF00;
    public static final int ORANGE = 0xFFFFC800;
    public static final int PINK = 0xFFFFAFAF;

    public static int getAlpha( int argb) {
        return (argb >>> 24) & 0xFF;
    }
    public static int getRed( int argb) {
        return (argb >>> 16) & 0xFF;
    }
    public static int getGreen( int argb) {
        return (argb >>> 8) & 0xFF;
    }
    public static int getBlue( int argb) {
        return (argb) & 0xFF;
    }
    public static int toColor( int a, int r, int g, int b) {
        return ((a&0xFF) << 24) | ((r&0xFF) << 16) | ((g&0xFF) << 8) | ((b&0xFF));
    }
    public static int toColor(  int r, int g, int b) {
        return (0xFF << 24) | ((r&0xFF) << 16) | ((g&0xFF) << 8) | ((b&0xFF));
    }
}
