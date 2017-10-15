package spirite.base.util.interpolation;

public class CubicSplineInterpolatorND {
    private float dx[][];	// x-differentials
    private float x_[][];	// x-values
    private float t_[];	// t-values
    private int N;
    private int length = 0;

    private boolean fast;	// UNIMPLEMENTED

    public CubicSplineInterpolatorND(float[][] points, int N, int length, boolean fast)
    {
    	this.N = N;
    	this.length = length;

        int l = Math.max(length, 10);
    	dx = new float[N][];
    	x_ = new float[N][];
    	t_ = new float[l];
    	for( int i=0; i<N; ++i) {
    		dx[i] = new float[l];
    		x_[i] = new float[l];
    	}
    	if( length != 0) {
    	}
    	
    	for( int w=0; w<length; ++w) {
    		for( int i=0; i<N; ++i)
    			x_[i][w] = points[w][i];
    		t_[w] = points[w][N];
    		
    	}

    	System.out.println(t_[0] +"," + t_[1] + "," + t_[2]);
    	
    	this.fast = fast;
    	calculateDifferentials();
    }
    
    private void calculateDifferentials() {
    	if( length <= 1) return;
    	
    	float dt = t_[1]-t_[0];
    	for( int i=0; i<N; ++i)
    		dx[i][0] = 0.25f*(x_[i][1]-x_[i][0])/dt;
    	
    	int w;
    	for( w=1; w<length-1; ++w) {
    		float dt1 = t_[w+1] - t_[w];
    		float dt2 = t_[w] - t_[w-1];
    		for( int i=0; i<N; ++i)
    			dx[i][w] = 0.5f * ((x_[i][w+1]-x_[i][w])/dt1 + (x_[i][w]-x_[i][w-1])/dt2);
    	}
    	dt = t_[w] - t_[w-1];
		for( int i=0; i<N; ++i)
			dx[i][w] = 0.25f*(x_[i][w] - x_[i][w-1])/dt;
    }
    
    public void addPoint(float[] p) {
        if( x_.length <= length) expand(length+1);

        // Code could be made less verbose in by combining parts of the
        //	different cases, but would be less readable
        for( int i=0; i<N; ++i)
        	x_[i][length] = p[i];
        t_[length] = p[N];
        
        if( length == 0) {
            t_[0] = 0;
        }
        else if( length == 1) {
            float dt = t_[1] - t_[0];
            for( int i=0; i<N; ++i) {
            	dx[i][1] = 0.25f*(x_[i][1]-x_[i][0])/dt;
            	dx[i][0] = dx[i][1];
            }
        }
        else {
            float dt1 = t_[length] - t_[length-1];
            float dt2 = t_[length-1] - t_[length-2];
            
    		for( int i=0; i<N; ++i) {
    			dx[i][length-1] = 0.5f * ((x_[i][length]-x_[i][length-1])/dt1 + (x_[i][length-1]-x_[i][length-2])/dt2);
    			dx[i][length] = 0.25f * (x_[i][length] - x_[i][length-1])/dt1;
    		}
        }

        length++;
    }
    
    /** Expands the internal arrays in order to accommodate the new length. */
    private void expand(int new_length) {
        if( x_.length >= new_length) return;

        int l = (length==0)?new_length:x_.length;

        // Expand by 50% at a time (similar to ArrayList)
        // TODO: Not using ArrayList to avoid boxing/unboxing bloat, but
        //	should still encapsulate this stuff in a custom primitive list
        //	object
        while( l < new_length)
            l = (l*3+1)/2;

        float buff[];
        for( int i=0; i<N; ++i) {
        	buff = new float[l];
            System.arraycopy(x_[i], 0, buff, 0, length);
        	x_[i] = buff;
        	buff = new float[l];
            System.arraycopy(dx[i], 0, buff, 0, length);
        	dx[i] = buff;
        }
        buff = new float[l];
        System.arraycopy(t_, 0, buff, 0, length);
        t_ = buff;
    }
    
    public float[] eval(float t) {
    	float[] ret = new float[N];
    	if( length == 0) return ret;
    	
    	if( t <= 0) {
    		for( int i=0; i<N; ++i)
    			ret[i] = x_[i][0];
    		return ret;
    	}
    	
    	int w=1;
    	while( t > t_[w] && ++w < length) ;
    	if( w == length) {
    		for( int i=0; i<N; ++i)
    			ret[i] = x_[i][length-1];
    		return ret;
    	}
    	
    	float dt = t_[w]-t_[w-1];
    	float n = (t - t_[w-1])/dt;
    	
    	for( int i=0; i<N; ++i) {
    		float a = dx[i][w-1]*dt - (x_[i][w] - x_[i][w-1]);
    		float b =-dx[i][w]*dt + x_[i][w] - x_[i][w-1];
    		ret[i] = (1-n)*x_[i][w-1] + n*x_[i][w] + n*(n-1)*(a*(1-n) + b*n);
    	}
    	
    	return ret;
    }
}
