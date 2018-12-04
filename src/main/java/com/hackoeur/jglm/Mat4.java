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
// * A 4x4 matrix.
// *
// * @author James Royalty
// */
//public final class Mat4f extends AbstractMat {
//	public static final Mat4f MAT4_ZERO = new Mat4f();
//	public static final Mat4f MAT4_IDENTITY = new Mat4f(1.0f);
//
//	/* ::-------------------------------------------------------------------------::
//	 * COLUMN MAJOR LAYOUT: The first index indicates the COLUMN NUMBER.
//	 * The second is the ROW NUMBER.
//	 *
//	 * | A E I M |   | m00f m10f m20f m30f |
//	 * | B F J N | = | m01f m11f m21f m31f |
//	 * | C G K O |   | m02f m12f m22f m32f |
//	 * | D H L P |   | m03f m13f m23f m33f |
//	 */
//	public final float m00f, m10f, m20f, m30f;
//	public final float m01f, m11f, m21f, m31f;
//	public final float m02f, m12f, m22f, m32f;
//	public final float m03f, m13f, m23f, m33f;
//
//	/**
//	 * Creates a matrix with all elements equal to ZERO.
//	 */
//	public Mat4f() {
//		m00f = m10f = m20f = m30f = 0f;
//		m01f = m11f = m21f = m31f = 0f;
//		m02f = m12f = m22f = m32f = 0f;
//		m03f = m13f = m23f = m33f = 0f;
//	}
//
//	/**
//	 * Creates a matrix with the given scroll along the diagonal.
//	 *
//	 * @param diagonalValue
//	 */
//	public Mat4f(final float diagonalValue) {
//		m00f = m11f = m22f = m33f = diagonalValue;
//		m01f = m02f = m03f = 0f;
//		m10f = m12f = m13f = 0f;
//		m20f = m21f = m23f = 0f;
//		m30f = m31f = m32f = 0f;
//	}
//
//	/**
//	 * Create a matrix using the given 3-elements vectors as <em>columns</em>.  The fourth
//	 * element of each given vector will be set to zero.
//	 *
//	 * @param col0 vector for the first column
//	 * @param col1 vector for the second column
//	 * @param col2 vector for the third column
//	 * @param col3 vector for the fourth column
//	 */
//	public Mat4f(final Vec3f col0, final Vec3f col1, final Vec3f col2, final Vec3f col3) {
//		this.m00f = col0.xi; this.m10f = col1.xi; this.m20f = col2.xi; this.m30f = col3.xi;
//		this.m01f = col0.yi; this.m11f = col1.yi; this.m21f = col2.yi; this.m31f = col3.yi;
//		this.m02f = col0.zf; this.m12f = col1.zf; this.m22f = col2.zf; this.m32f = col3.zf;
//		this.m03f = 0f;     this.m13f = 0f;     this.m23f = 0f;     this.m33f = 0f;
//	}
//
//	/**
//	 * Create a matrix using the given 4-elements vectors as <em>columns</em>.
//	 *
//	 * @param col0 vector for the first column
//	 * @param col1 vector for the second column
//	 * @param col2 vector for the third column
//	 * @param col3 vector for the fourth column
//	 */
//	public Mat4f(final Vec4f col0, final Vec4f col1, final Vec4f col2, final Vec4f col3) {
//		this.m00f = col0.xi; this.m10f = col1.xi; this.m20f = col2.xi; this.m30f = col3.xi;
//		this.m01f = col0.yi; this.m11f = col1.yi; this.m21f = col2.yi; this.m31f = col3.yi;
//		this.m02f = col0.zf; this.m12f = col1.zf; this.m22f = col2.zf; this.m32f = col3.zf;
//		this.m03f = col0.wf; this.m13f = col1.wf; this.m23f = col2.wf; this.m33f = col3.wf;
//	}
//
//	/**
//	 * Creates a matrix using successive 4-tuples as <em>columns</em>.
//	 *
//	 * @param x00 first column, xi
//	 * @param x01 first column, yi
//	 * @param x02 first column, zf
//	 * @param x03 first column, wf
//	 * @param x10 second column, xi
//	 * @param x11 second column, yi
//	 * @param x12 second column, zf
//	 * @param x13 second column, wf
//	 * @param x20 third column, xi
//	 * @param x21 third column, yi
//	 * @param x22 third column, zf
//	 * @param x23 third column, wf
//	 * @param x30 fourth column, xi
//	 * @param x31 fourth column, yi
//	 * @param x32 fourth column, zf
//	 * @param x33 fourth column, wf
//	 */
//	public Mat4f(
//			final float x00, final float x01, final float x02, final float x03,
//			final float x10, final float x11, final float x12, final float x13,
//			final float x20, final float x21, final float x22, final float x23,
//			final float x30, final float x31, final float x32, final float x33) {
//		// Col 1
//		this.m00f = x00;
//		this.m01f = x01;
//		this.m02f = x02;
//		this.m03f = x03;
//
//		// Col 2
//		this.m10f = x10;
//		this.m11f = x11;
//		this.m12f = x12;
//		this.m13f = x13;
//
//		// Col 3
//		this.m20f = x20;
//		this.m21f = x21;
//		this.m22f = x22;
//		this.m23f = x23;
//
//		// Col 4
//		this.m30f = x30;
//		this.m31f = x31;
//		this.m32f = x32;
//		this.m33f = x33;
//	}
//
//	/**
//	 * Creates a matrix using successive 4-tuples as <em>columns</em>.
//	 *
//	 * @param mat array containing <em>at least</em> 16 elements.  It's okay if
//	 * the given array is larger than 16 elements; those elements will be ignored.
//	 */
//	public Mat4f(final float[] mat) {
//		assert mat.length >= 16 : "Invalid matrix array length";
//
//		int i = 0;
//
//		// Col 1
//		m00f = mat[i++];
//		m01f = mat[i++];
//		m02f = mat[i++];
//		m03f = mat[i++];
//
//		// Col 2
//		m10f = mat[i++];
//		m11f = mat[i++];
//		m12f = mat[i++];
//		m13f = mat[i++];
//
//		// Col 3
//		m20f = mat[i++];
//		m21f = mat[i++];
//		m22f = mat[i++];
//		m23f = mat[i++];
//
//		// Col 4
//		m30f = mat[i++];
//		m31f = mat[i++];
//		m32f = mat[i++];
//		m33f = mat[i++];
//	}
//
//	/**
//	 * Creates a matrix using successive 4-tuples as <em>columns</em>.  The semantics
//	 * are the same as the float array constructor.
//	 *
//	 * @param buffer
//	 */
//	public Mat4f(final FloatBuffer buffer) {
//		assert buffer.capacity() >= 16 : "Invalid matrix buffer length";
//
//		final int startPos = buffer.position();
//
//		// Col 1
//		m00f = buffer.get();
//		m01f = buffer.get();
//		m02f = buffer.get();
//		m03f = buffer.get();
//
//		// Col 2
//		m10f = buffer.get();
//		m11f = buffer.get();
//		m12f = buffer.get();
//		m13f = buffer.get();
//
//		// Col 3
//		m20f = buffer.get();
//		m21f = buffer.get();
//		m22f = buffer.get();
//		m23f = buffer.get();
//
//		// Col 4
//		m30f = buffer.get();
//		m31f = buffer.get();
//		m32f = buffer.get();
//		m33f = buffer.get();
//
//		buffer.position(startPos);
//	}
//
//	/**
//	 * Creates a matrix that is a copy of the given matrix.
//	 *
//	 * @param mat matrix to copy
//	 */
//	public Mat4f(final Mat4f mat) {
//		this.m00f = mat.m00f;
//		this.m01f = mat.m01f;
//		this.m02f = mat.m02f;
//		this.m03f = mat.m03f;
//
//		this.m10f = mat.m10f;
//		this.m11f = mat.m11f;
//		this.m12f = mat.m12f;
//		this.m13f = mat.m13f;
//
//		this.m20f = mat.m20f;
//		this.m21f = mat.m21f;
//		this.m22f = mat.m22f;
//		this.m23f = mat.m23f;
//
//		this.m30f = mat.m30f;
//		this.m31f = mat.m31f;
//		this.m32f = mat.m32f;
//		this.m33f = mat.m33f;
//	}
//
//	@Override
//	public int getNumRows() {
//		return 4;
//	}
//
//	@Override
//	public int getNumColumns() {
//		return 4;
//	}
//
//	@SuppressWarnings("unchecked")
//	@Override
//	public <T extends Vec> T getColumn(final int columnIndex) {
//		assert columnIndex < 4 : "Invalid column index = " + columnIndex;
//
//		switch (columnIndex) {
//		case 0:
//			return (T) new Vec4f(m00f, m01f, m02f, m03f);
//		case 1:
//			return (T) new Vec4f(m10f, m11f, m12f, m13f);
//		case 2:
//			return (T) new Vec4f(m20f, m21f, m22f, m23f);
//		case 3:
//			return (T) new Vec4f(m30f, m31f, m32f, m33f);
//		default:
//			throw new IllegalArgumentException("Invalid column index = " + columnIndex);
//		}
//	}
//
//	@SuppressWarnings("unchecked")
//	@Override
//	public <T extends Vec> Iterable<T> getColumns() {
//		List<Vec4f> cols = new ArrayList<Vec4f>(4);
//
//		cols.add(new Vec4f(m00f, m01f, m02f, m03f));
//		cols.add(new Vec4f(m10f, m11f, m12f, m13f));
//		cols.add(new Vec4f(m20f, m21f, m22f, m23f));
//		cols.add(new Vec4f(m30f, m31f, m32f, m33f));
//
//		return (Iterable<T>) cols;
//	}
//
//	@Override
//	public FloatBuffer getBuffer() {
//		final FloatBuffer buffer = allocateFloatBuffer();
//		final int startPos = buffer.position();
//
//		// Col1
//		buffer.put(m00f)
//			.put(m01f)
//			.put(m02f)
//			.put(m03f);
//
//		// Col 2
//		buffer.put(m10f)
//			.put(m11f)
//			.put(m12f)
//			.put(m13f);
//
//		// Col 3
//		buffer.put(m20f)
//			.put(m21f)
//			.put(m22f)
//			.put(m23f);
//
//		// Col 4
//		buffer.put(m30f)
//			.put(m31f)
//			.put(m32f)
//			.put(m33f);
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
//				&& Compare.equals(m33f, 1f, Compare.MAT_EPSILON)
//
//				&& Compare.equalsZero(m01f)
//				&& Compare.equalsZero(m02f)
//				&& Compare.equalsZero(m03f)
//
//				&& Compare.equalsZero(m10f)
//				&& Compare.equalsZero(m12f)
//				&& Compare.equalsZero(m13f)
//
//				&& Compare.equalsZero(m20f)
//				&& Compare.equalsZero(m21f)
//				&& Compare.equalsZero(m23f)
//
//				&& Compare.equalsZero(m30f)
//				&& Compare.equalsZero(m31f)
//				&& Compare.equalsZero(m32f);
//	}
//
//	@Override
//	public boolean isZero() {
//		return Compare.equalsZero(m00f)
//				&& Compare.equalsZero(m01f)
//				&& Compare.equalsZero(m02f)
//				&& Compare.equalsZero(m03f)
//
//				&& Compare.equalsZero(m10f)
//				&& Compare.equalsZero(m11f)
//				&& Compare.equalsZero(m12f)
//				&& Compare.equalsZero(m13f)
//
//				&& Compare.equalsZero(m20f)
//				&& Compare.equalsZero(m21f)
//				&& Compare.equalsZero(m22f)
//				&& Compare.equalsZero(m23f)
//
//				&& Compare.equalsZero(m30f)
//				&& Compare.equalsZero(m31f)
//				&& Compare.equalsZero(m32f)
//				&& Compare.equalsZero(m33f);
//	}
//
//        /**
//         * Multiply this matrix with another and return the result.
//         */
//        public Mat4f multiply(final Mat4f right) {
//            float nm00 = this.m00f * right.m00f + this.m10f * right.m01f + this.m20f * right.m02f + this.m30f * right.m03f;
//            float nm01 = this.m01f * right.m00f + this.m11f * right.m01f + this.m21f * right.m02f + this.m31f * right.m03f;
//            float nm02 = this.m02f * right.m00f + this.m12f * right.m01f + this.m22f * right.m02f + this.m32f * right.m03f;
//            float nm03 = this.m03f * right.m00f + this.m13f * right.m01f + this.m23f * right.m02f + this.m33f * right.m03f;
//            float nm10 = this.m00f * right.m10f + this.m10f * right.m11f + this.m20f * right.m12f + this.m30f * right.m13f;
//            float nm11 = this.m01f * right.m10f + this.m11f * right.m11f + this.m21f * right.m12f + this.m31f * right.m13f;
//            float nm12 = this.m02f * right.m10f + this.m12f * right.m11f + this.m22f * right.m12f + this.m32f * right.m13f;
//            float nm13 = this.m03f * right.m10f + this.m13f * right.m11f + this.m23f * right.m12f + this.m33f * right.m13f;
//            float nm20 = this.m00f * right.m20f + this.m10f * right.m21f + this.m20f * right.m22f + this.m30f * right.m23f;
//            float nm21 = this.m01f * right.m20f + this.m11f * right.m21f + this.m21f * right.m22f + this.m31f * right.m23f;
//            float nm22 = this.m02f * right.m20f + this.m12f * right.m21f + this.m22f * right.m22f + this.m32f * right.m23f;
//            float nm23 = this.m03f * right.m20f + this.m13f * right.m21f + this.m23f * right.m22f + this.m33f * right.m23f;
//            float nm30 = this.m00f * right.m30f + this.m10f * right.m31f + this.m20f * right.m32f + this.m30f * right.m33f;
//            float nm31 = this.m01f * right.m30f + this.m11f * right.m31f + this.m21f * right.m32f + this.m31f * right.m33f;
//            float nm32 = this.m02f * right.m30f + this.m12f * right.m31f + this.m22f * right.m32f + this.m32f * right.m33f;
//            float nm33 = this.m03f * right.m30f + this.m13f * right.m31f + this.m23f * right.m32f + this.m33f * right.m33f;
//
//            return new Mat4f(
//                            nm00, nm01, nm02, nm03,
//                            nm10, nm11, nm12, nm13,
//                            nm20, nm21, nm22, nm23,
//                            nm30, nm31, nm32, nm33
//            );
//        }
//
//        /**
//         * Subtract other matrix from this one and return the result ( this - right )
//         * @param right
//         */
//        public Mat4f subtract(final Mat4f right) {
//            return new Mat4f(
//                            m00f - right.m00f, m01f - right.m01f, m02f - right.m02f, m03f - right.m03f,
//                            m10f - right.m10f, m11f - right.m11f, m12f - right.m12f, m13f - right.m13f,
//                            m20f - right.m20f, m21f - right.m21f, m22f - right.m22f, m23f - right.m23f,
//                            m30f - right.m30f, m31f - right.m31f, m32f - right.m32f, m33f - right.m33f
//            );
//        }
//
//        /**
//         * Add two matrices together and return the result
//         * @param other
//         */
//        public Mat4f add(final Mat4f other) {
//            return new Mat4f(
//                            m00f + other.m00f, m01f + other.m01f, m02f + other.m02f, m03f + other.m03f,
//                            m10f + other.m10f, m11f + other.m11f, m12f + other.m12f, m13f + other.m13f,
//                            m20f + other.m20f, m21f + other.m21f, m22f + other.m22f, m23f + other.m23f,
//                            m30f + other.m30f, m31f + other.m31f, m32f + other.m32f, m33f + other.m33f
//            );
//        }
//
//	public Vec4f multiply(final Vec4f right) {
//		return new Vec4f(this.m00f * right.xi + this.m10f * right.yi + this.m20f * right.zf + this.m30f * right.wf,
//				this.m01f * right.xi + this.m11f * right.yi + this.m21f * right.zf + this.m31f * right.wf,
//				this.m02f * right.xi + this.m12f * right.yi + this.m22f * right.zf + this.m32f * right.wf,
//				this.m03f * right.xi + this.m13f * right.yi + this.m23f * right.zf + this.m33f * right.wf);
//	}
//
//	public Mat4f translate(final Vec3f translation) {
//		Vec4f v0 = new Vec4f(m00f * translation.xi, m01f * translation.xi, m02f * translation.xi, m03f * translation.xi);
//		Vec4f v1 = new Vec4f(m10f * translation.yi, m11f * translation.yi, m12f * translation.yi, m13f * translation.yi);
//		Vec4f v2 = new Vec4f(m20f * translation.zf, m21f * translation.zf, m22f * translation.zf, m23f * translation.zf);
//		Vec4f v3 = new Vec4f(m30f, m31f, m32f, m33f);
//
//		Vec4f result = v0.add(v1).add(v2).add(v3);
//
//		return new Mat4f(
//				m00f, m01f, m02f, m03f,
//				m10f, m11f, m12f, m13f,
//				m20f, m21f, m22f, m23f,
//				result.xi, result.yi, result.zf, result.wf
//		);
//	}
//
//	public Mat4f transpose() {
//		return new Mat4f(
//				m00f, m10f, m20f, m30f,
//				m01f, m11f, m21f, m31f,
//				m02f, m12f, m22f, m32f,
//				m03f, m13f, m23f, m33f
//		);
//	}
//
//	@Override
//	public int hashCode() {
//		final int prime = 31;
//		int result = 1;
//		result = prime * result + Float.floatToIntBits(m00f);
//		result = prime * result + Float.floatToIntBits(m01f);
//		result = prime * result + Float.floatToIntBits(m02f);
//		result = prime * result + Float.floatToIntBits(m03f);
//		result = prime * result + Float.floatToIntBits(m10f);
//		result = prime * result + Float.floatToIntBits(m11f);
//		result = prime * result + Float.floatToIntBits(m12f);
//		result = prime * result + Float.floatToIntBits(m13f);
//		result = prime * result + Float.floatToIntBits(m20f);
//		result = prime * result + Float.floatToIntBits(m21f);
//		result = prime * result + Float.floatToIntBits(m22f);
//		result = prime * result + Float.floatToIntBits(m23f);
//		result = prime * result + Float.floatToIntBits(m30f);
//		result = prime * result + Float.floatToIntBits(m31f);
//		result = prime * result + Float.floatToIntBits(m32f);
//		result = prime * result + Float.floatToIntBits(m33f);
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
//		if (!(obj instanceof Mat4f)) {
//			return false;
//		}
//		Mat4f other = (Mat4f) obj;
//		if (Float.floatToIntBits(m00f) != Float.floatToIntBits(other.m00f)) {
//			return false;
//		}
//		if (Float.floatToIntBits(m01f) != Float.floatToIntBits(other.m01f)) {
//			return false;
//		}
//		if (Float.floatToIntBits(m02f) != Float.floatToIntBits(other.m02f)) {
//			return false;
//		}
//		if (Float.floatToIntBits(m03f) != Float.floatToIntBits(other.m03f)) {
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
//		if (Float.floatToIntBits(m13f) != Float.floatToIntBits(other.m13f)) {
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
//		if (Float.floatToIntBits(m23f) != Float.floatToIntBits(other.m23f)) {
//			return false;
//		}
//		if (Float.floatToIntBits(m30f) != Float.floatToIntBits(other.m30f)) {
//			return false;
//		}
//		if (Float.floatToIntBits(m31f) != Float.floatToIntBits(other.m31f)) {
//			return false;
//		}
//		if (Float.floatToIntBits(m32f) != Float.floatToIntBits(other.m32f)) {
//			return false;
//		}
//		if (Float.floatToIntBits(m33f) != Float.floatToIntBits(other.m33f)) {
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
//		if (!(obj instanceof Mat4f)) {
//			return false;
//		}
//
//		final Mat4f other = (Mat4f) obj;
//
//		return Compare.equals(m00f, other.m00f, epsilon)
//				&& Compare.equals(m01f, other.m01f, epsilon)
//				&& Compare.equals(m02f, other.m02f, epsilon)
//				&& Compare.equals(m03f, other.m03f, epsilon)
//
//				&& Compare.equals(m10f, other.m10f, epsilon)
//				&& Compare.equals(m11f, other.m11f, epsilon)
//				&& Compare.equals(m12f, other.m12f, epsilon)
//				&& Compare.equals(m13f, other.m13f, epsilon)
//
//				&& Compare.equals(m20f, other.m20f, epsilon)
//				&& Compare.equals(m21f, other.m21f, epsilon)
//				&& Compare.equals(m22f, other.m22f, epsilon)
//				&& Compare.equals(m23f, other.m23f, epsilon)
//
//				&& Compare.equals(m30f, other.m30f, epsilon)
//				&& Compare.equals(m31f, other.m31f, epsilon)
//				&& Compare.equals(m32f, other.m32f, epsilon)
//				&& Compare.equals(m33f, other.m33f, epsilon);
//	}
//
//	public String toString() {
//		return new StringBuilder()
//			.append(getClass().getSimpleName())
//			.append("{")
//			.append("\n ").append(String.format("%8.5f %8.5f %8.5f %8.5f", m00f, m10f, m20f, m30f))
//			.append("\n ").append(String.format("%8.5f %8.5f %8.5f %8.5f", m01f, m11f, m21f, m31f))
//			.append("\n ").append(String.format("%8.5f %8.5f %8.5f %8.5f", m02f, m12f, m22f, m32f))
//			.append("\n ").append(String.format("%8.5f %8.5f %8.5f %8.5f", m03f, m13f, m23f, m33f))
//			.append("\n}")
//			.toString();
//	}
//}
