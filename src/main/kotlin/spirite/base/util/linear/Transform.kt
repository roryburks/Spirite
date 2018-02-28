package spirite.base.util.linear

import com.hackoeur.jglm.support.FastMath
import kotlin.math.cos
import kotlin.math.sin

/**
 *
 * NOTE: When a MutableTransform is passed as a plain Transform it is essentially immutable.  Naughty coders can violate
 * this by manually casting the Transform as a MutableTransform, but calling toImmutable will create a new MutableTransform
 * even if the base is Mutable.  This allows a contract-based conversion from Mutable to Immutable without having to
 * push data.
 *
 * For example, internally you might want something to be a MutableTransform since you might be altering it frequently,
 * but it passes it to a class as a Transform.  This is essentially a contract that they should not edit it (and can't
 * without manually casting it).
 */
abstract class Transform()
{
    abstract val m00:Float
    abstract val m01:Float
    abstract val m02:Float
    abstract val m10:Float
    abstract val m11:Float
    abstract val m12:Float

    val determinant : Float get() {return m00 * m11 - m01 * m10}

    operator fun times(tx: Transform) : Transform {
        return ImmutableTransform(
                m00* tx.m00 + m01* tx.m10,
                m00* tx.m01 + m01* tx.m11,
                m00 * tx.m02 + m01 * tx.m12 + m02,
                m10 * tx.m00 + m11 * tx.m10,
                m10 * tx.m01 + m11 * tx.m11,
                m10 * tx.m02 + m11 * tx.m12 + m12
        )
    }

    fun apply(v: Vec2) : Vec2 {
        return Vec2(
                m00*v.x + m01*v.y + m02,
                m10*v.x + m11*v.y + m12)
    }

    fun invert(): ImmutableTransform {
        val det = determinant

        val im00 = m11 / det
        val im10 = -m10 / det
        val im01 = -m01 / det
        val im11 = m00 / det
        val im02 = (m01 * m12 - m02 * m11) / det
        val im12 = (-m00 * m12 + m10 * m02) / det

        return ImmutableTransform(im00, im01, im02, im10, im11, im12)
    }

    // Not extremely necessary because Transform is taken/treated as a contract to not change it (by casting it).
    fun toImmutable() : ImmutableTransform {
        return ImmutableTransform(m00,m01,m02, m10,m11,m12)
    }
    fun toMutable() : MutableTransform {
        return MutableTransform(m00,m01,m02, m10,m11,m12)
    }

    override fun toString(): String {
        return "$m00\t$m01\t$m02\n$m10\r$m11\t$m12\n0\t0\t1"
    }

    companion object {
        fun TranslationMatrix( transX: Float, transY: Float) : Transform{
            return ImmutableTransform(
                    1f, 0f, transX,
                    0f, 1f, transY)
        }
        fun ScaleMatrix( scaleX: Float, scaleY: Float): Transform{
            return ImmutableTransform(
                    scaleX, 0f, 0f,
                    0f, scaleY, 0f)
        }
        fun RotationMatrix( theta: Float): Transform {
            val c = cos(theta)
            val s = sin(theta)
            return ImmutableTransform(
                    c, -s, 0f,
                    s, c, 0f)
        }
        val IdentityMatrix = ImmutableTransform(1f, 0f, 0f, 0f, 1f, 0f)

        //    public static MatTrans ConvertTri(
        //    		float fx1, float fy1, float fx2, float fy2, float fx3, float fy3,
        //    		float tx1, float ty1, float tx2, float ty2, float tx3, float ty3) throws NoninvertableException
        //    {
        //    	MatTrans from = new MatTrans( fx3-fx2, fx2-fx1, fx1, fy3-fy2, fy2-fy1, fy1);
        //    	MatTrans to = new MatTrans( tx3-tx2, tx2-tx1, tx1, ty3-ty2, ty2-ty1, ty1);
        //
        //    	MatTrans result = from.createInverse();
        //    	result.preConcatenate(to);
        //
        //    	return result;
        //    }
    }

}

data class ImmutableTransform(
        override val m00: Float = 1f, override val m01: Float = 0f, override val m02: Float = 0f,
        override val m10: Float = 0f, override val m11: Float = 1f, override val m12: Float = 0f
) :Transform()
{
}

data class MutableTransform(
        private var _m00:Float = 1f, private var _m01:Float = 0f, private var _m02:Float = 0f,
        private var _m10:Float = 0f, private var _m11:Float = 1f, private var _m12:Float = 0f
):Transform()
{
    constructor():this( 1f,0f,0f,0f,1f,0f){}

    override val m00: Float get() = _m00
    override val m01: Float get() = _m01
    override val m02: Float get() = _m02
    override val m10: Float get() = _m10
    override val m11: Float get() = _m11
    override val m12: Float get() = _m12

    fun translate(ox: Float, oy: Float) {
        _m02 += ox * m00 + oy * m01
        _m12 += ox * m10 + oy * m11
    }

    fun preTranslate(ox: Float, oy: Float) {
        _m02 += ox
        _m12 += oy
    }

    // THIS = THIS * tx
    fun concatenate(tx: Transform) {
        val n00 = m00 * tx.m00 + m01 * tx.m10
        val n01 = m00 * tx.m01 + m01 * tx.m11
        val n02 = m00 * tx.m02 + m01 * tx.m12 + m02
        val n10 = m10 * tx.m00 + m11 * tx.m10
        val n11 = m10 * tx.m01 + m11 * tx.m11
        val n12 = m10 * tx.m02 + m11 * tx.m12 + m12
        _m00 = n00
        _m01 = n01
        _m02 = n02
        _m10 = n10
        _m11 = n11
        _m12 = n12
    }

    // THIS = tx * THIS
    fun preConcatenate(tx: Transform) {
        val n00 = tx.m00 * m00 + tx.m01 * m10
        val n01 = tx.m00 * m01 + tx.m01 * m11
        val n02 = tx.m00 * m02 + tx.m01 * m12 + tx.m02
        val n10 = tx.m10 * m00 + tx.m11 * m10
        val n11 = tx.m10 * m01 + tx.m11 * m11
        val n12 = tx.m10 * m02 + tx.m11 * m12 + tx.m12
        _m00 = n00
        _m01 = n01
        _m02 = n02
        _m10 = n10
        _m11 = n11
        _m12 = n12
    }

    fun setToIdentity() {
        _m00 = 1f
        _m11 = 1f
        _m12 = 0f
        _m10 = 0f
        _m01 = 0f
        _m02 = 0f
    }

    fun rotate(theta: Float) {
        val c = Math.cos(theta.toDouble()).toFloat()
        val s = FastMath.sin(theta.toDouble()).toFloat()
        val n00 = m00 * c + m01 * s
        val n01 = m00 * -s + m01 * c
        val n10 = m10 * c + m11 * s
        val n11 = m10 * -s + m11 * c
        _m00 = n00
        _m01 = n01
        _m10 = n10
        _m11 = n11
    }

    fun preRotate(theta: Float) {
        val c = FastMath.cos(theta.toDouble()).toFloat()
        val s = FastMath.sin(theta.toDouble()).toFloat()
        val n00 = c * m00 - s * m10
        val n01 = c * m01 - s * m11
        val n02 = c * m02 - s * m12
        val n10 = s * m00 + c * m10
        val n11 = s * m01 + c * m11
        val n12 = s * m02 + c * m12
        _m00 = n00
        _m01 = n01
        _m02 = n02
        _m10 = n10
        _m11 = n11
        _m12 = n12
    }

    fun scale(sx: Float, sy: Float) {
        _m00 *= sx
        _m01 *= sy
        _m10 *= sx
        _m11 *= sy
    }

    fun preScale(sx: Float, sy: Float) {
        _m00 *= sx
        _m01 *= sx
        _m02 *= sx
        _m10 *= sy
        _m11 *= sy
        _m12 *= sy
    }

    fun getTranslateX() = m02
    fun getTranslateY() = m12

    fun inverseTransform(from: Vec2): Vec2 {
        return invert().apply(from)
    }

    fun invertM(): MutableTransform {
        val det = determinant

        if( det == 0f) throw NoninvertableException()

        val im00 = m11 / det
        val im10 = -m10 / det
        val im01 = -m01 / det
        val im11 = m00 / det
        val im02 = (m01 * m12 - m02 * m11) / det
        val im12 = (-m00 * m12 + m10 * m02) / det

        return MutableTransform(im00, im01, im02, im10, im11, im12)
    }

    class NoninvertableException internal constructor() : Exception("Matrix is Not Invertable")


    companion object {
        fun TranslationMatrix( transX: Float, transY: Float) : MutableTransform{
            return MutableTransform(
                    1f, 0f, transX,
                    0f, 1f, transY)
        }
        fun ScaleMatrix( scaleX: Float, scaleY: Float): MutableTransform{
            return MutableTransform(
                    scaleX, 0f, 0f,
                    scaleY, 0f, 0f)
        }
        fun RotationMatrix( theta: Float): MutableTransform {
            val c = cos(theta)
            val s = sin(theta)
            return MutableTransform(
                    c, -s, 0f,
                    s, c, 0f)
        }
        fun IdentityMatrix() = MutableTransform(1f, 0f, 0f, 0f, 1f, 0f)
    }
}