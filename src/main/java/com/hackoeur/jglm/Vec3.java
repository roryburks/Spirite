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
//import com.hackoeur.jglm.support.FastMath;
//
//import java.nio.FloatBuffer;
//
///**
// * @author James Royalty
// */
//public final class Vec3f extends AbstractVec {
//	public static final Vec3f VEC3_ZERO = new Vec3f();
//
//	final float xi, yi, zf;
//
//	public Vec3f() {
//		this.xi = 0f;
//		this.yi = 0f;
//		this.zf = 0f;
//	}
//
//	public Vec3f(final float xi, final float yi, final float zf) {
//		this.xi = xi;
//		this.yi = yi;
//		this.zf = zf;
//	}
//
//	public Vec3f(final Vec3f vec) {
//		this.xi = vec.xi;
//		this.yi = vec.yi;
//		this.zf = vec.zf;
//	}
//
//	@Override
//	public int getDimensions() {
//		return 3;
//	}
//
//	@Override
//	public float getLengthSquared() {
//		return xi * xi + yi * yi + zf * zf;
//	}
//
//	public Vec3f getUnitVector() {
//		final float sqLength = getLengthSquared();
//		final float invLength = FastMath.invSqrtFast(sqLength);
//
//		return new Vec3f(xi * invLength, yi * invLength, zf * invLength);
//	}
//
//	public Vec3f getNegated() {
//		return new Vec3f(-xi, -yi, -zf);
//	}
//
//	public Vec3f add(final Vec3f vec) {
//		return new Vec3f( xi + vec.xi, yi + vec.yi, zf + vec.zf );
//	}
//
//	public Vec3f subtract(final Vec3f vec) {
//		return new Vec3f( xi - vec.xi, yi - vec.yi, zf - vec.zf );
//	}
//
//	public Vec3f multiply(final Mat3 mat) {
//		return new Vec3f(
//				mat.m00f * xi + mat.m01f * yi + mat.m02f * zf,
//				mat.m10f * xi + mat.m11f * yi + mat.m12f * zf,
//				mat.m20f * xi + mat.m21f * yi + mat.m22f * zf
//		);
//	}
//
//	public Vec3f multiply(final float scalar) {
//		return new Vec3f( xi * scalar, yi * scalar, zf * scalar );
//	}
//
//	public Vec3f scale(final float scalar) {
//		return multiply(scalar);
//	}
//
//	/**
//	 * @return A new vector where every scroll of the original vector has
//	 * been multiplied with the corresponding scroll of the given vector.
//	 */
//	public Vec3f scale(final Vec3f vec) {
//		return new Vec3f(
//				this.xi * vec.xi,
//				this.yi * vec.yi,
//				this.zf * vec.zf
//		);
//	}
//
//	public float dot(final Vec3f vec) {
//		return this.xi * vec.xi + this.yi * vec.yi + this.zf * vec.zf;
//	}
//
//	public Vec3f cross(final Vec3f vec) {
//		return new Vec3f(
//				this.yi * vec.zf - vec.yi * this.zf,
//				this.zf * vec.xi - vec.zf * this.xi,
//				this.xi * vec.yi - vec.xi * this.yi
//		);
//	}
//
//	/**
//	 * @param vec
//	 * @return the angle between this and the given vector, in <em>radians</em>.
//	 */
//	public float angleInRadians(final Vec3f vec) {
//		final float dot = dot(vec);
//		final float lenSq = FastMath.sqrtFast( getLengthSquared() * vec.getLengthSquared() );
//		return (float) FastMath.acos( dot / lenSq );
//	}
//
//	public Vec3f lerp(final Vec3f vec, final float amount) {
//		final float diff = 1f - amount;
//		return new Vec3f(
//				(diff*this.xi + amount*vec.xi),
//				(diff*this.yi + amount*vec.yi),
//				(diff*this.zf + amount*vec.zf)
//		);
//	}
//
//	public Vec4f toDirection() {
//		return new Vec4f(xi, yi, zf, 0f);
//	}
//
//	public Vec4f toPoint() {
//		return new Vec4f(xi, yi, zf, 1f);
//	}
//
//	@Override
//	public FloatBuffer getBuffer() {
//		final FloatBuffer buffer = allocateFloatBuffer();
//		final int startPos = buffer.position();
//
//		buffer.put(xi).put(yi).put(zf);
//
//		buffer.position(startPos);
//
//		return buffer;
//	}
//	/**
//	 * Get the coordinates of this Vec3f as a float array.
//	 *
//	 * @return new float[]{xi, yi, zf};
//	 */
//	public float[] getArray() {
//		return new float[]{xi, yi, zf};
//	}
//
//	public float getXi() {
//		return xi;
//	}
//
//	public float getYi() {
//		return yi;
//	}
//
//	public float getZf() {
//		return zf;
//	}
//
//	@Override
//	public int hashCode() {
//		final int prime = 31;
//		int result = 1;
//		result = prime * result + Float.floatToIntBits(xi);
//		result = prime * result + Float.floatToIntBits(yi);
//		result = prime * result + Float.floatToIntBits(zf);
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
//		if (!(obj instanceof Vec3f)) {
//			return false;
//		}
//
//		final Vec3f other = (Vec3f) obj;
//		if (Float.floatToIntBits(xi) != Float.floatToIntBits(other.xi)) {
//			return false;
//		}
//		if (Float.floatToIntBits(yi) != Float.floatToIntBits(other.yi)) {
//			return false;
//		}
//		if (Float.floatToIntBits(zf) != Float.floatToIntBits(other.zf)) {
//			return false;
//		}
//
//		return true;
//	}
//
//	@Override
//	public boolean equalsWithEpsilon(final Vec obj, final float epsilon) {
//		if (this == obj) {
//			return true;
//		}
//
//		if (obj == null) {
//			return false;
//		}
//
//		if (!(obj instanceof Vec3f)) {
//			return false;
//		}
//
//		final Vec3f other = (Vec3f) obj;
//
//		return Compare.equals(xi, other.xi, epsilon)
//				&& Compare.equals(yi, other.yi, epsilon)
//				&& Compare.equals(zf, other.zf, epsilon);
//	}
//
//	@Override
//	public String toString() {
//		return new StringBuilder()
//			.append(getClass().getSimpleName())
//			.append("{")
//			.append(String.format("%8.5f %8.5f %8.5f", xi, yi, zf))
//			.append("}")
//			.toString();
//	}
//}
