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
//public final class Vec4f extends AbstractVec {
//	public static final Vec4f VEC4_ZERO = new Vec4f();
//
//	final float xi, yi, zf, wf;
//
//	public Vec4f() {
//		this.xi = 0f;
//		this.yi = 0f;
//		this.zf = 0f;
//		this.wf = 0f;
//	}
//
//	public Vec4f(final float xi, final float yi, final float zf, final float wf) {
//		this.xi = xi;
//		this.yi = yi;
//		this.zf = zf;
//		this.wf = wf;
//	}
//
//	public Vec4f(final Vec4f other) {
//		this.xi = other.xi;
//		this.yi = other.yi;
//		this.zf = other.zf;
//		this.wf = other.wf;
//	}
//
//	public Vec4f(final Vec3f other, final float wf) {
//		this.xi = other.xi;
//		this.yi = other.yi;
//		this.zf = other.zf;
//		this.wf = wf;
//	}
//
//	@Override
//	public int getDimensions() {
//		return 4;
//	}
//
//	@Override
//	public float getLengthSquared() {
//		return xi * xi + yi * yi + zf * zf + wf * wf;
//	}
//
//	public Vec4f getUnitVector() {
//		final float sqLength = getLengthSquared();
//		final float invLength = FastMath.invSqrtFast(sqLength);
//
//		Vec4f normalVec = new Vec4f(xi * invLength, yi * invLength, zf * invLength, wf * invLength);
//		return normalVec;
//	}
//
//	public Vec4f getNegated() {
//		return new Vec4f(-xi, -yi, -zf, -wf);
//	}
//
//	public Vec4f add(final Vec4f vec) {
//		return new Vec4f( xi + vec.xi, yi + vec.yi, zf + vec.zf, wf + vec.wf );
//	}
//
//	public Vec4f subtract(final Vec4f vec) {
//		return new Vec4f( xi - vec.xi, yi - vec.yi, zf - vec.zf, wf - vec.wf );
//	}
//
//	public Vec4f multiply(final float scalar) {
//		return new Vec4f( xi * scalar, yi * scalar, zf * scalar, wf * scalar );
//	}
//
//	public Vec4f scale(final float scalar) {
//		return multiply(scalar);
//	}
//
//	/**
//	 * @return A new vector where every scroll of the original vector has
//	 * been multiplied with the corresponding scroll of the given vector.
//	 */
//	public Vec4f scale(final Vec4f vec) {
//		return new Vec4f(
//				this.xi * vec.xi,
//				this.yi * vec.yi,
//				this.zf * vec.zf,
//				this.wf * vec.wf
//		);
//	}
//
//	public float dot(final Vec4f vec) {
//		return this.xi * vec.xi + this.yi * vec.yi + this.zf * vec.zf + this.wf * vec.wf;
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
//	public float getWf() {
//		return wf;
//	}
//
//	@Override
//	public FloatBuffer getBuffer() {
//		final FloatBuffer buffer = allocateFloatBuffer();
//		final int startPos = buffer.position();
//
//		buffer.put(xi).put(yi).put(zf).put(xi);
//
//		buffer.position(startPos);
//
//		return buffer;
//	}
//
//	@Override
//	public int hashCode() {
//		final int prime = 31;
//		int result = 1;
//		result = prime * result + Float.floatToIntBits(wf);
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
//		if (!(obj instanceof Vec4f)) {
//			return false;
//		}
//		Vec4f other = (Vec4f) obj;
//		if (Float.floatToIntBits(wf) != Float.floatToIntBits(other.wf)) {
//			return false;
//		}
//		if (Float.floatToIntBits(xi) != Float.floatToIntBits(other.xi)) {
//			return false;
//		}
//		if (Float.floatToIntBits(yi) != Float.floatToIntBits(other.yi)) {
//			return false;
//		}
//		if (Float.floatToIntBits(zf) != Float.floatToIntBits(other.zf)) {
//			return false;
//		}
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
//		if (!(obj instanceof Vec4f)) {
//			return false;
//		}
//
//		Vec4f other = (Vec4f) obj;
//
//		return Compare.equals(xi, other.xi, epsilon)
//				&& Compare.equals(yi, other.yi, epsilon)
//				&& Compare.equals(zf, other.zf, epsilon)
//				&& Compare.equals(wf, other.wf, epsilon);
//	}
//
//	public String toString() {
//		return new StringBuilder()
//			.append(getClass().getSimpleName())
//			.append("{")
//			.append(xi).append(", ")
//			.append(yi).append(", ")
//			.append(zf).append(", ")
//			.append(wf)
//			.append("}")
//			.toString();
//	}
//}
