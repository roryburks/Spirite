package spirite.base.util.linear

import rb.vectrix.linear.ITransformF
import rb.vectrix.linear.Mat4f
import rb.glow.gl.IFloat32Source
import rb.glow.gl.IGL
import rb.vectrix.linear.ITransform
import rb.vectrix.mathUtil.f

// TODO: Figure out where this belongs (later).  Possibly in Glow

fun Mat4f.toIFloat32Source(gl: IGL): IFloat32Source {
    val source = gl.makeFloat32Source(16)
    source[0] = this.m00f; source[1] = this.m01f; source[2] = this.m02f; source[3] = this.m03f
    source[4] = this.m10f; source[5] = this.m11f; source[6] = this.m12f; source[7] = this.m13f
    source[8] = this.m20f; source[9] = this.m21f; source[10] = this.m22f; source[11] = this.m23f
    source[12] = this.m30f; source[13] = this.m31f; source[14] = this.m32f; source[15] = this.m33f
    return source
}