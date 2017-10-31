package spirite.base.util;

import java.awt.Color;

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
    public static final int BLUE = 0xFF0000FF;
    public static final int GREEN = 0xFF00FF00;
    public static final int CYAN = 0xFF00FFFF;
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
    
    public static Color darken( Color color) {
    	float[] hsv = new float[3];
    	Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), hsv);
    	hsv[2] = Math.max(0, hsv[2]-0.1f);
    	return new Color(Color.HSBtoRGB(hsv[0], hsv[1], hsv[2]));
    }
    
    public static double colorDistance( int color1, int color2) {
    	int dr = getRed(color1) - getRed(color2);
    	int dg = getGreen(color1) - getGreen(color2);
    	int db = getBlue(color1) - getBlue(color2);
    	int da = getAlpha(color1) - getAlpha(color2);
    	return Math.sqrt(dr*dr + dg*dg + db*db + da*da);
    }
}
