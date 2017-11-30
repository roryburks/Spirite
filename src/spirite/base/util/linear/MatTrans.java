package spirite.base.util.linear;

import com.hackoeur.jglm.support.FastMath;

/**
 * MatTrans is a library-independent implementation of a 2D translation matrix.  Much of it
 * mimicks the behavior of AWT's AffineTransform
 *
 * @author Rory Burks
 */

public class MatTrans {
    // [ m00 m01 m02 ]
    // [ m10 m11 m12 ]
    // [   0   0   1 ]
    private float m00, m01, m02;
    private float m10, m11, m12;

    public MatTrans() {
        m00 = m11 = 1;
    }
    public MatTrans(float m00, float m01, float m02, float m10, float m11, float m12) {
    	this.m00 = m00;
    	this.m01 = m01;
    	this.m02 = m02;
    	this.m10 = m10;
    	this.m11 = m11;
    	this.m12 = m12;
    }
    public MatTrans( MatTrans other) {
    	m00 = other.m00;
    	m01 = other.m01;
    	m02 = other.m02;
    	m10 = other.m10;
    	m11 = other.m11;
    	m12 = other.m12;
    }
    public static MatTrans TranslationMatrix( float transX, float transY) {
    	return new MatTrans( 
    			1, 0, transX,
    			0, 1, transY);
    }
    public static MatTrans ScaleMatrix( float scaleX, float scaleY) {
    	return new MatTrans( 
    			scaleX, 0, 0,
    			0, scaleY, 0);
    }
    public static MatTrans RotationMatrix( float theta) {
        float c = (float) Math.cos(theta);
        float s = (float) FastMath.sin(theta);
    	return new MatTrans( 
    			c, -s, 0,
    			s, c, 0);
    }
    public static MatTrans ConvertTri(
    		float fx1, float fy1, float fx2, float fy2, float fx3, float fy3,
    		float tx1, float ty1, float tx2, float ty2, float tx3, float ty3) throws NoninvertableException
    {
    	MatTrans from = new MatTrans( fx3-fx2, fx2-fx1, fx1, fy3-fy2, fy2-fy1, fy1);
    	MatTrans to = new MatTrans( tx3-tx2, tx2-tx1, tx1, ty3-ty2, ty2-ty1, ty1);

    	MatTrans result = from.createInverse();
    	result.preConcatenate(to);
    	
    	return result;
    }
    
    public float getM00() {return m00;}
    public float getM01() {return m01;}
    public float getM02() {return m02;}
    public float getM10() {return m10;}
    public float getM11() {return m11;}
    public float getM12() {return m12;}

	public void translate(float ox, float oy) {
		m02 += ox * m00 + oy * m01;
		m12 += ox * m10 + oy * m11;
	}
	public void preTranslate( float ox, float oy) {
		m02 += ox;
		m12 += oy;
	}
	
	// THIS = THIS * tx
	public void concatenate(MatTrans tx) {
		float n00 = m00 * tx.m00 + m01 * tx.m10;
		float n01 = m00 * tx.m01 + m01 * tx.m11;
		float n02 = m00 * tx.m02 + m01 * tx.m12 + m02;
		float n10 = m10 * tx.m00 + m11 * tx.m10;
		float n11 = m10 * tx.m01 + m11 * tx.m11;
		float n12 = m10 * tx.m02 + m11 * tx.m12 + m12;
		m00 = n00;
		m01 = n01;
		m02 = n02;
		m10 = n10;
		m11 = n11;
		m12 = n12;
	}

	// THIS = tx * THIS
	public void preConcatenate(MatTrans tx) {
		float n00 = tx.m00 * m00 + tx.m01 * m10;
		float n01 = tx.m00 * m01 + tx.m01 * m11;
		float n02 = tx.m00 * m02 + tx.m01 * m12 + tx.m02;
		float n10 = tx.m10 * m00 + tx.m11 * m10;
		float n11 = tx.m10 * m01 + tx.m11 * m11;
		float n12 = tx.m10 * m02 + tx.m11 * m12 + tx.m12;
		m00 = n00;
		m01 = n01;
		m02 = n02;
		m10 = n10;
		m11 = n11;
		m12 = n12;
	}
	
	public void setToIdentity() {
		m00 = m11 = 1;
		m02 = m01 = m10 = m12 = 0;
	}
	public void rotate(float theta) {
        float c = (float) Math.cos(theta);
        float s = (float) FastMath.sin(theta);
        float n00 = m00 *  c + m01 * s;
        float n01 = m00 * -s + m01 * c;
        float n10 = m10 *  c + m11 * s;
        float n11 = m10 * -s + m11 * c;
        m00 = n00;
        m01 = n01;
        m10 = n10;
        m11 = n11;
	}
	public void preRotate(float theta) {
        float c = (float) FastMath.cos(theta);
        float s = (float) FastMath.sin(theta);
        float n00 = c*m00 - s*m10;
        float n01 = c*m01 - s*m11;
        float n02 = c*m02 - s*m12;
        float n10 = s*m00 + c*m10;
        float n11 = s*m01 + c*m11;
        float n12 = s*m02 + c*m12;
		m00 = n00;
		m01 = n01;
		m02 = n02;
		m10 = n10;
		m11 = n11;
		m12 = n12;
	}
	public void scale(float sx, float sy) {
        m00 *= sx;
        m01 *= sy;
        m10 *= sx;
        m11 *= sy;
	}
	public void preScale( float sx, float sy) {
		m00 *= sx;
		m01 *= sx;
		m02 *= sx;
		m10 *= sy;
		m11 *= sy;
		m12 *= sy;
	}
	public float getTranslateX() {return m02;}
	public float getTranslateY() {return m12;}
	
	public Vec2 transform(Vec2 from) {
        float x = from.x;
        float y = from.y;
        return new Vec2(
        		m00 * x + m01 * y + m02,
        		m10 * x + m11 * y + m12);
	}
	public Vec2 inverseTransform(Vec2 from) throws NoninvertableException 
	{
		return createInverse().transform(from);
	}
	
    public MatTrans createInverse()
            throws NoninvertableException
    {
    	float det = getDeterminant();
        if (det == 0)
            throw new NoninvertableException("can't invert transform");

        float im00 = m11 / det;
        float im10 = -m10 / det;
        float im01 = -m01 / det;
        float im11 = m00 / det;
        float im02 = (m01 * m12 - m02 * m11) / det;
        float im12 = (-m00 * m12 + m10 * m02) / det;

        return new MatTrans (im00, im01, im02, im10, im11, im12);
    }
    
    public static class NoninvertableException extends Exception {
    	private NoninvertableException( String message){super(message);}
    }

    public float getDeterminant()
    {
        return m00 * m11 - m01 * m10;
    }
    
    @Override
    public String toString() {
    	return m00 + "\t" + m01 + "\t" + m02 + "\n" + m10 + "\t" + m11 + "\t" + m12 + "\n1\t0\t1";
    }
}