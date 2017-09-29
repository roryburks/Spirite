package spirite.base.util.glmath;

import com.hackoeur.jglm.support.FastMath;

/**
 *
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

	public void preConcatenate(MatTrans tx) {
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
	
	public void setToIdentity() {
		m00 = m11 = 1;
		m02 = m01 = m10 = m12 = 0;
	}
	public void rotate(float theta) {
        float c = (float) FastMath.cos(theta);
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
	public void scale(float sx, float sy) {
        m00 *= sx;
        m01 *= sy;
        m10 *= sx;
        m11 *= sy;
	}
	public float getTranslateX() {return m02;}
	public float getTranslateY() {return m12;}
	
	public Vec2 transform(Vec2 from, Vec2 to) {
        float x = from.x;
        float y = from.y;
        to.x = m00 * x + m01 * y + m02;
        to.y = m10 * x + m11 * y + m12;
        return to;
	}
	public Vec2 inverseTransform(Vec2 from, Vec2 to) throws NoninvertableException 
	{
		return createInverse().transform(from, to);
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