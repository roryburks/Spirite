package com.hackoeur.jglm;///* Copyright (C) 2013 James L. Royalty
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package com.hackoeur.jglm;
//
//import com.hackoeur.jglm.support.Compare;
//
//import java.nio.FloatBuffer;
//import java.util.ArrayList;
//import java.util.List;
//
///**
// * A 3x3 matrix.
// *
// * @author James Royalty
// */
//public final class Mat3 extends AbstractMat {
//	public static final Mat3 MAT3_ZERO = new Mat3();
//	public static final Mat3 MAT3_IDENTITY = new Mat3(1.0f);
//
//	/* ::-------------------------------------------------------------------------::
//	 * COLUMN MAJOR LAYOUT: The first index indicates the COLUMN NUMBER.
//	 * The second is the ROW NUMBER.
//	 *
//	 * | A D G |   | m00f m10f m20f |
//	 * | B E H | = | m01f m11f m21f |
//	 * | C F I |   | m02f m12f m22f |
//	 */
//	final float m00f, m10f, m20f;
//	final float m01f, m11f, m21f;
//	final float m02f, m12f, m22f;
//
//	/**
//	 * Creates a matrix with all elements equal to ZERO.
//	 */
//	public Mat3() {
//		m00f = m10f = m20f = 0f;
//		m01f = m11f = m21f = 0f;
//		m02f = m12f = m22f = 0f;
//	}
//
//	/**
//	 * Creates a matrix with the given scroll along the diagonal.
//	 *
//	 * @param diagonalValue
//	 */
//	public Mat3(final float diagonalValue) {
//		m00f = m11f = m22f = diagonalValue;
//		m10f = m20f = 0f;
//		m01f = m21f = 0f;
//		m02f = m12f = 0f;
//	}
//
//	/**
//	 * Create a matrix using the given vectors as <em>columns</em>. For example,
//	 * <pre>
//	 * Mat3 m1 = new Mat3(
//	 * 	new Vec3f(1f, 2f, 3f), // first column
//	 * 	new Vec3f(4f, 5f, 6f), // second
//	 * 	new Vec3f(7f, 8f, 9f)  // third
//	 * );</pre>
//	 *
//	 * will create the following 3x3 matrix:
//	 * <pre>
//	 * | 1 4 7 |
//	 * | 2 5 8 |
//	 * | 3 6 9 |
//	 * </pre>
//	 *
//	 * @param col0 vector for the first column
//	 * @param col1 vector for the second column
//	 * @param col2 vector for the third column
//	 */
//	public Mat3(final Vec3f col0, final Vec3f col1, final Vec3f col2) {
//		this.m00f = col0.xi; this.m10f = col1.xi; this.m20f = col2.xi;
//		this.m01f = col0.yi; this.m11f = col1.yi; this.m21f = col2.yi;
//		this.m02f = col0.zf; this.m12f = col1.zf; this.m22f = col2.zf;
//	}
//
//	/**
//	 * Creates a matrix using successive triples as <em>columns</em>.  For example,
//	 * <pre>
//	 * Mat3 m1 = new Mat3(
//	 * 	1f, 2f, 3f, // first column
//	 * 	4f, 5f, 6f, // second
//	 * 	7f, 8f, 9f  // third
//	 * );</pre>
//	 *
//	 * will create the following 3x3 matrix:
//	 * <pre>
//	 * | 1 4 7 |
//	 * | 2 5 8 |
//	 * | 3 6 9 |
//	 * </pre>
//	 *
//	 * @param x00 first column, xi
//	 * @param x01 first column, yi
//	 * @param x02 first column, zf
//	 * @param x10 second column, xi
//	 * @param x11 second column, yi
//	 * @param x12 second column, zf
//	 * @param x20 third column, xi
//	 * @param x21 third column, yi
//	 * @param x22 third column, zf
//	 */
//	public Mat3(
//			final float x00, final float x01, final float x02,
//			final float x10, final float x11, final float x12,
//			final float x20, final float x21, final float x22) {
//		// Col 1
//		this.m00f = x00;
//		this.m01f = x01;
//		this.m02f = x02;
//
//		// Col 2
//		this.m10f = x10;
//		this.m11f = x11;
//		this.m12f = x12;
//
//		// Col 3
//		this.m20f = x20;
//		this.m21f = x21;
//		this.m22f = x22;
//	}
//
//	/**
//	 * Creates a matrix using successive triples as <em>columns</em>.  For example,
//	 * <pre>
//	 * Mat3 m1 = new Mat3(new float[] {
//	 * 	1f, 2f, 3f, // first column
//	 * 	4f, 5f, 6f, // second
//	 * 	7f, 8f, 9f  // third
//	 * });</pre>
//	 *
//	 * will create the following 3x3 matrix:
//	 * <pre>
//	 * | 1 4 7 |
//	 * | 2 5 8 |
//	 * | 3 6 9 |
//	 * </pre>
//	 *
//	 * @param mat array containing <em>at least</em> 9 elements.  It's okay if
//	 * the given array is larger than 9 elements; those elements will be ignored.
//	 */
//	public Mat3(final float[] mat) {
//		assert mat.length >= 9 : "Invalid matrix array length";
//
//		int i = 0;
//
//		// Col 1
//		m00f = mat[i++];
//		m01f = mat[i++];
//		m02f = mat[i++];
//
//		// Col 2
//		m10f = mat[i++];
//		m11f = mat[i++];
//		m12f = mat[i++];
//
//		// Col 3
//		m20f = mat[i++];
//		m21f = mat[i++];
//		m22f = mat[i++];
//	}
//
//	/**
//	 * Creates a matrix using successive triples as <em>columns</em>.  The semantics
//	 * are the same as the float array constructor.
//	 *
//	 * @param buffer
//	 */
//	public Mat3(final FloatBuffer buffer) {
//		assert buffer.capacity() >= 9 : "Invalid matrix buffer length";
//
//		final int startPos = buffer.position();
//
//		m00f = buffer.get();
//		m01f = buffer.get();
//		m02f = buffer.get();
//
//		m10f = buffer.get();
//		m11f = buffer.get();
//		m12f = buffer.get();
//
//		m20f = buffer.get();
//		m21f = buffer.get();
//		m22f = buffer.get();
//
//		buffer.position(startPos);
//	}
//
//	/**
//	 * Creates a matrix that is a copy of the given matrix.
//	 *
//	 * @param mat matrix to copy
//	 */
//	public Mat3(final Mat3 mat) {
//		this.m00f = mat.m00f;
//		this.m01f = mat.m01f;
//		this.m02f = mat.m02f;
//
//		this.m10f = mat.m10f;
//		this.m11f = mat.m11f;
//		this.m12f = mat.m12f;
//
//		this.m20f = mat.m20f;
//		this.m21f = mat.m21f;
//		this.m22f = mat.m22f;
//	}
//
//	@Override
//	public int getNumRows() {
//		return 3;
//	}
//
//	@Override
//	public int getNumColumns() {
//		return 3;
//	}
//
//	@SuppressWarnings("unchecked")
//	@Override
//	public <T extends Vec> T getColumn(final int columnIndex) {
//		assert columnIndex < 3 : "Invalid column index = " + columnIndex;
//
//		switch (columnIndex) {
//		case 0:
//			return (T) new Vec3f(m00f, m01f, m02f);
//		case 1:
//			return (T) new Vec3f(m10f, m11f, m12f);
//		case 2:
//			return (T) new Vec3f(m20f, m21f, m22f);
//		default:
//			throw new IllegalArgumentException("Invalid column index = " + columnIndex);
//		}
//	}
//
//	@SuppressWarnings("unchecked")
//	@Override
//	public <T extends Vec> Iterable<T> getColumns() {
//		List<Vec3f> cols = new ArrayList<Vec3f>(3);
//
//		cols.add(new Vec3f(m00f, m01f, m02f));
//		cols.add(new Vec3f(m10f, m11f, m12f));
//		cols.add(new Vec3f(m20f, m21f, m22f));
//
//		return (Iterable<T>) cols;
//	}
//
//	@Override
//	public FloatBuffer getBuffer() {
//		final FloatBuffer buffer = allocateFloatBuffer();
//		final int startPos = buffer.position();
//
//		// Col 1
//		buffer.put(m00f)
//			.put(m01f)
//			.put(m02f);
//
//		// Col 2
//		buffer.put(m10f)
//			.put(m11f)
//			.put(m12f);
//
//		// Col 3
//		buffer.put(m20f)
//			.put(m21f)
//			.put(m22f);
//
//		buffer.position(startPos);
//
//		return buffer;
//	}
//
//	@Override
//	public boolean isIdentity() {
//		return Compare.equals(m00f, 1f, Compare.MAT_EPSILON)
//				&& Compare.equals(m11f, 1f, Compare.MAT_EPSILON)
//				&& Compare.equals(m22f, 1f, Compare.MAT_EPSILON)
//
//				&& Compare.equalsZero(m01f)
//				&& Compare.equalsZero(m02f)
//
//				&& Compare.equalsZero(m10f)
//				&& Compare.equalsZero(m12f)
//
//				&& Compare.equalsZero(m20f)
//				&& Compare.equalsZero(m21f);
//	}
//
//	@Override
//	public boolean isZero() {
//		return Compare.equalsZero(m00f)
//				&& Compare.equalsZero(m01f)
//				&& Compare.equalsZero(m02f)
//
//				&& Compare.equalsZero(m10f)
//				&& Compare.equalsZero(m11f)
//				&& Compare.equalsZero(m12f)
//
//				&& Compare.equalsZero(m20f)
//				&& Compare.equalsZero(m21f)
//				&& Compare.equalsZero(m22f);
//	}
//
//	public Mat3 multiply(final float a) {
//		return new Mat3(
//				m00f*a, m01f*a, m02f*a,
//				m10f*a, m11f*a, m12f*a,
//				m20f*a, m21f*a, m22f*a
//		);
//	}
//
//	public Mat3 multiply(final Mat3 mat) {
//		return new Mat3(
//				this.m00f * mat.m00f + this.m10f * mat.m01f + this.m20f * mat.m02f, // m00f
//				this.m01f * mat.m00f + this.m11f * mat.m01f + this.m21f * mat.m02f, // m01f
//				this.m02f * mat.m00f + this.m12f * mat.m01f + this.m22f * mat.m02f, // m02f
//
//				this.m00f * mat.m10f + this.m10f * mat.m11f + this.m20f * mat.m12f, // m10f
//				this.m01f * mat.m10f + this.m11f * mat.m11f + this.m21f * mat.m12f, // m11f
//				this.m02f * mat.m10f + this.m12f * mat.m11f + this.m22f * mat.m12f, // m12f
//
//				this.m00f * mat.m20f + this.m10f * mat.m21f + this.m20f * mat.m22f, // m20f
//				this.m01f * mat.m20f + this.m11f * mat.m21f + this.m21f * mat.m22f, // m21f
//				this.m02f * mat.m20f + this.m12f * mat.m21f + this.m22f * mat.m22f  // m22f
//		);
//	}
//
//	/**
//	 * This is the equivalent of <strong>this * vector</strong> (if we had operator
//	 * overloading).  If you want <strong>vector * this</strong> then
//	 * see {@groupLink Vec3f#multiply(Mat3)}.
//	 *
//	 * @param vec
//	 * @return
//	 */
//	public Vec3f multiply(final Vec3f vec) {
//		return new Vec3f(
//				m00f * vec.xi + m10f * vec.yi + m20f * vec.zf,
//				m01f * vec.xi + m11f * vec.yi + m21f * vec.zf,
//				m02f * vec.xi + m12f * vec.yi + m22f * vec.zf
//		);
//	}
//
//	public Mat3 transpose() {
//		return new Mat3(
//				m00f, m10f, m20f,
//				m01f, m11f, m21f,
//				m02f, m12f, m22f
//		);
//	}
//
//	public float determinantF() {
//		return m00f * (m11f * m22f - m12f * m21f) - m01f * (m10f * m22f - m12f * m20f) + m02f * (m10f * m21f - m11f * m20f);
//	}
//
//	@Override
//	public int hashCode() {
//		final int prime = 31;
//		int result = 1;
//		result = prime * result + Float.floatToIntBits(m00f);
//		result = prime * result + Float.floatToIntBits(m01f);
//		result = prime * result + Float.floatToIntBits(m02f);
//		result = prime * result + Float.floatToIntBits(m10f);
//		result = prime * result + Float.floatToIntBits(m11f);
//		result = prime * result + Float.floatToIntBits(m12f);
//		result = prime * result + Float.floatToIntBits(m20f);
//		result = prime * result + Float.floatToIntBits(m21f);
//		result = prime * result + Float.floatToIntBits(m22f);
//		return result;
//	}
//
//	@Override
//	public boolean equals(Object obj) {
//		if (this == obj) {
//			return true;
//		}
//		if (obj == null) {
//			return false;
//		}
//		if (!(obj instanceof Mat3)) {
//			return false;
//		}
//		Mat3 other = (Mat3) obj;
//		if (Float.floatToIntBits(m00f) != Float.floatToIntBits(other.m00f)) {
//			return false;
//		}
//		if (Float.floatToIntBits(m01f) != Float.floatToIntBits(other.m01f)) {
//			return false;
//		}
//		if (Float.floatToIntBits(m02f) != Float.floatToIntBits(other.m02f)) {
//			return false;
//		}
//		if (Float.floatToIntBits(m10f) != Float.floatToIntBits(other.m10f)) {
//			return false;
//		}
//		if (Float.floatToIntBits(m11f) != Float.floatToIntBits(other.m11f)) {
//			return false;
//		}
//		if (Float.floatToIntBits(m12f) != Float.floatToIntBits(other.m12f)) {
//			return false;
//		}
//		if (Float.floatToIntBits(m20f) != Float.floatToIntBits(other.m20f)) {
//			return false;
//		}
//		if (Float.floatToIntBits(m21f) != Float.floatToIntBits(other.m21f)) {
//			return false;
//		}
//		if (Float.floatToIntBits(m22f) != Float.floatToIntBits(other.m22f)) {
//			return false;
//		}
//		return true;
//	}
//
//	@Override
//	public boolean equalsWithEpsilon(final Mat obj, final float epsilon) {
//		if (this == obj) {
//			return true;
//		}
//
//		if (obj == null) {
//			return false;
//		}
//
//		if (!(obj instanceof Mat3)) {
//			return false;
//		}
//
//		final Mat3 other = (Mat3) obj;
//
//		return Compare.equals(m00f, other.m00f, epsilon)
//				&& Compare.equals(m01f, other.m01f, epsilon)
//				&& Compare.equals(m02f, other.m02f, epsilon)
//
//				&& Compare.equals(m10f, other.m10f, epsilon)
//				&& Compare.equals(m11f, other.m11f, epsilon)
//				&& Compare.equals(m12f, other.m12f, epsilon)
//
//				&& Compare.equals(m20f, other.m20f, epsilon)
//				&& Compare.equals(m21f, other.m21f, epsilon)
//				&& Compare.equals(m22f, other.m22f, epsilon);
//	}
//
//	public String toString() {
//		return new StringBuilder()
//			.append(getClass().getSimpleName())
//			.append("{")
//			.append("\n ").append(String.format("%8.5f %8.5f %8.5f", m00f, m10f, m20f))
//			.append("\n ").append(String.format("%8.5f %8.5f %8.5f", m01f, m11f, m21f))
//			.append("\n ").append(String.format("%8.5f %8.5f %8.5f", m02f, m12f, m22f))
//			.append("\n}")
//			.toString();
//	}
//}
