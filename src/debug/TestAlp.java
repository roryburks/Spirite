package debug;

import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUtessellator;

import spirite.base.util.glu.PolygonTesselater.GLUTCB;
import spirite.base.util.linear.MatTrans;
import spirite.base.util.linear.Vec2;
import spirite.base.util.linear.MatTrans.NoninvertableException;

public class TestAlp {

    public static void main(String[] args) {
    	(new d005_BinarySearch())._do();
    }
    
    public static class d005_BinarySearch
    {
    	public void _do() {
    		System.out.println(indexedBinSearch(45));
    		System.out.println(indexedBinSearch(35));
    		System.out.println(indexedBinSearch(25));
    		System.out.println(indexedBinSearch(16));
    		System.out.println(indexedBinSearch(12));
    		System.out.println(indexedBinSearch(10));
    		System.out.println(indexedBinSearch(8));
    		System.out.println(indexedBinSearch(5));
    		System.out.println(indexedBinSearch(3.5f));
    		System.out.println(indexedBinSearch(2));
    	}
    	
    	public float indexedBinSearch(float met) {
        	float[] t= new float[]{3,4,7,9,11,15,20,30,40};	
        	int length = t.length;
        	
			if( met < 0) return 0;
			
			int min=0;
			int max=length-1;
			int mid = 0;
			
			while( min <= max) {
				mid = min + ((max-min)/2);
				
				if( t[mid] > met)
					max = mid-1;
				else if( t[mid] < met)
					min = mid+1;
				else
					return mid;
			}
			
			if( min >= length)
				return length-1;
			if( min == 0)
				return 0;
			
			float lerp = (met-t[min-1])/(float)(t[min]-t[min-1]);
			
			if( lerp < 0) return min-1;
			if( lerp > 1) return min+1;
			return (min-1) + lerp;
    	}
    }
    
    public static class d004_crossProduct
    {
    	public void _do() {
    		Vec2 b = new Vec2(100,100);
    		Vec2 a = new Vec2(0, 100);
			
			float scale_b = b.getMag();
			
			float t =  a.dot(b) / scale_b;
			float m = a.cross(b) / scale_b;
			
			System.out.println((t / scale_b) + ":" + m);
    	}
    }
    
    public static class d003_synchronizedLocks
    {
    	Object lock = new Object();
    	
    	public void _do() {
    		new Thread(() -> {
        		synchronized( lock) {
    				synchronized(lock) {
	        			(new Thread( () ->  {
		        				_do2(1);
								try {
									Thread.sleep(1000);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
	        			})).start();
    				}
    				synchronized(lock) {
	        			(new Thread( () ->  {
		        				_do2(2);
								try {
									Thread.sleep(1000);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
	        			})).start();
    				}
        			_do2(0);
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
        		}
    		}).start();
    		new Thread(() -> {
        		synchronized( lock) {
        			_do2(10);
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
        		}
    		}).start();
    		
    		// Output: 
    		//>Do2 Start: 0
    		//>Do2 End: 0
    		//>Do2 Start: 2
    		//>Do2 End: 2
    		//>Do2 Start: 1
    		//>Do2 End: 1
    		//>Do2 Start: 10
    		//>Do2 End: 10
    		// Conclusion:
    		//	I have no earthly idea why 2 happens before 1, but other than that, the results
    		//	show how Nested locks work as expected, maintaining a stack of locks when locking
    		//	on the same object.  It also shows that locks do what you expect.
    	}
    	private void _do2(int i) {
    		synchronized( lock) {
    			System.out.println("Do2 Start: "+ i);
    			try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    			System.out.println("Do2 End: "+ i);
    		}
    	}
    }
    
    public static void d002_tesselationWithGLU() {
		GLUtessellator tess = GLU.gluNewTess();
		GLUTCB callback = new GLUTCB();

		GLU.gluTessCallback(tess, GLU.GLU_TESS_VERTEX, callback);// vertexCallback);
		GLU.gluTessCallback(tess, GLU.GLU_TESS_BEGIN, callback);// beginCallback);
		GLU.gluTessCallback(tess, GLU.GLU_TESS_END, callback);// endCallback);
		GLU.gluTessCallback(tess, GLU.GLU_TESS_ERROR, callback);// errorCallback);
		GLU.gluTessCallback(tess, GLU.GLU_TESS_COMBINE, callback);// combineCallback);

		double[][] data = { 
				{0, 0, 0},
				{25, 25, 0},
				{60, 0, 0},
				{70, 70, 0},
				{50, 70, 0},
				{50, 50, 0},
				{100, 50, 0},
				{100, 100, 0},
				{-25, 100, 0},
				{-25, 125, 0},
				{50, 125, 0},
				{50, 70, 0},
				{0, 70, 0},
			};
		
	    GLU.gluTessProperty(tess,
	        GLU.GLU_TESS_WINDING_RULE, 
	        GLU.GLU_TESS_WINDING_ODD);
	    GLU.gluTessBeginPolygon(tess, null);
	    GLU.gluTessBeginContour(tess);
	    for( int i=0; i < data.length; ++i) 
	    	GLU.gluTessVertex(tess, data[i], 0, data[i]);
	    GLU.gluTessEndContour(tess);
	    GLU.gluTessEndPolygon(tess);
	    GLU.gluDeleteTess(tess);
    }
    public static void d001_triangleConvertingTransform() {
    	float x1 = 14;
    	float y1 = 7;
    	float x2 = 8;
    	float y2 = 46;
    	float x3 = 74;
    	float y3 = 34;
    	float tx1 = 10;
    	float ty1 = 76;
    	float tx2 = -13;
    	float ty2 = 99;
    	float tx3 = 29;
    	float ty3 = 47;
    	
    	{
	    	MatTrans trans = new MatTrans( x3-x2, x2-x1, x1, y3-y2, y2-y1, y1);
	    	Vec2 p = trans.transform(new Vec2(0,0), new Vec2(0,0));
	    	System.out.println( p.x + "," + p.y);
	    	Vec2 p2 = trans.transform(new Vec2(0,1), new Vec2(0,0));
	    	System.out.println( p2.x + "," + p2.y);
	    	Vec2 p3 = trans.transform(new Vec2(1,1), new Vec2(0,0));
	    	System.out.println( p3.x + "," + p3.y);
    	}
    	
    	try {
			MatTrans trans = MatTrans.ConvertTri(x1, y1, x2, y2, x3, y3, tx1, ty1, tx2, ty2, tx3, ty3);

	    	Vec2 p = trans.transform(new Vec2(14,7), new Vec2(0,0));
	    	System.out.println( p.x + "," + p.y);
	    	Vec2 p2 = trans.transform(new Vec2(8,46), new Vec2(0,0));
	    	System.out.println( p2.x + "," + p2.y);
	    	Vec2 p3 = trans.transform(new Vec2(74,34), new Vec2(0,0));
	    	System.out.println( p3.x + "," + p3.y);
		} catch (NoninvertableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}
