package rb.vectrix.linear

import rb.vectrix.mathUtil.d
import kotlin.math.cos
import kotlin.math.sin



interface ITransform
{
    val m00 : Double
    val m01 : Double
    val m02 : Double
    val m10 : Double
    val m11 : Double
    val m12 : Double

    operator fun times(tx: ITransform) = ImmutableTransformD(
            m00 * tx.m00 + m01 * tx.m10,
            m00 * tx.m01 + m01 * tx.m11,
            m00 * tx.m02 + m01 * tx.m12 + m02,
            m10 * tx.m00 + m11 * tx.m10,
            m10 * tx.m01 + m11 * tx.m11,
            m10 * tx.m02 + m11 * tx.m12 + m12
    )

    fun apply( v : Vec2) : Vec2 = Vec2d(
            m00 * v.x + m01 * v.y + m02,
            m10 * v.x + m11 * v.y + m12
    )

    val determinant get() = m00*m11 - m01*m10

    fun invert() : ITransform?

    fun toImmutable() : ImmutableTrasform
    fun toMutable() : MutableTransform
}

fun ITransformF.invertN() : ImmutableTransformF = invert() ?: ImmutableTransformF.Identity

sealed class ImmutableTrasform : ITransform
sealed class MutableTransform : ITransform
{

}

interface ITransformD : ITransform
{
    override val determinant get() = m00*m11 - m01 * m10

    override fun invert(): ImmutableTransformD? {
        val det = m00*m11 - m01*m10
        if( det == 0.0) return null

        val im00 = m11 / det
        val im10 = -m10 / det
        val im01 = -m01 / det
        val im11 = m00 / det
        val im02 = (m01 * m12 - m02 * m11) / det
        val im12 = (-m00 * m12 + m10 * m02) / det

        return ImmutableTransformD(im00, im01, im02, im10, im11, im12)
    }

    override fun toImmutable() : ImmutableTransformD
    override fun toMutable(): MutableTransformD
}

data class ImmutableTransformD(
        override val m00 : Double,
        override val m01 : Double,
        override val m02 : Double,
        override val m10 : Double,
        override val m11 : Double,
        override val m12 : Double )
    : ImmutableTrasform(), ITransformD
{
    override fun toImmutable() = this
    override fun toMutable() = MutableTransformD(m00, m01, m02, m10, m11, m12)

    companion object {
        val Identity = ImmutableTransformD(1.0, 0.0, 0.0, 0.0, 1.0, 0.0)
        fun Translation( transX : Double, transY: Double) = ImmutableTransformD(
                1.0, 0.0, transX,
                0.0, 1.0, transY
        )

        fun Scale( scaleX : Double, scaleY: Double) = ImmutableTransformD(
                scaleX, 0.0, 0.0,
                0.0, scaleY, 0.0
        )

        fun Rotation( theta: Double) : ImmutableTransformD {
            val c = cos(theta)
            val s = sin(theta)
            return ImmutableTransformD(
                    c, -s, 0.0,
                    s, c, 0.0
            )
        }
    }
}


data class MutableTransformD(
        override var m00 : Double,
        override var m01 : Double,
        override var m02 : Double,
        override var m10 : Double,
        override var m11 : Double,
        override var m12 : Double )
    : MutableTransform(), ITransformD
{
    // = M * Translation(ox,oy)
    fun translate( ox: Double, oy: Double) {
        m02 += ox * m00 + oy * m01
        m12 += ox * m10 + oy * m11
    }

    // = Translation(ox,oy) * M
    fun preTranslate( ox: Double, oy: Double) {
        m02 += ox
        m12 += oy
    }

    // = M * Scale(sx, sy)
    fun scale( sx:Double, sy: Double) {
        m00 *= sx
        m01 *= sy
        m10 *= sx
        m11 *= sy
    }

    // = Scale(sx,sy) * M
    fun preScale(sx: Double, sy: Double) {
        m00 *= sx
        m01 *= sx
        m02 *= sx
        m10 *= sy
        m11 *= sy
        m12 *= sy
    }

    // = M * Rotate(theta)
    fun rotate( theta: Double) {
        val c = cos(theta)
        val s = sin(theta)
        val n00 = m00 * c + m01 * s
        val n01 = m00 * -s + m01 * c
        val n10 = m10 * c + m11 * s
        val n11 = m10 * -s + m11 * c
        m00 = n00
        m01 = n01
        m10 = n10
        m11 = n11
    }

    // = Rotate(theta) * M
    fun preRotate( theta: Double) {
        val c = cos(theta)
        val s = sin(theta)
        val n00 = c * m00 - s * m10
        val n01 = c * m01 - s * m11
        val n02 = c * m02 - s * m12
        val n10 = s * m00 + c * m10
        val n11 = s * m01 + c * m11
        val n12 = s * m02 + c * m12
        m00 = n00
        m01 = n01
        m02 = n02
        m10 = n10
        m11 = n11
        m12 = n12
    }

    // = M * R
    fun concatenate(R : ITransform) {
        val n00 = m00 * R.m00 + m01 * R.m10
        val n01 = m00 * R.m01 + m01 * R.m11
        val n02 = m00 * R.m02 + m01 * R.m12 + m02
        val n10 = m10 * R.m00 + m11 * R.m10
        val n11 = m10 * R.m01 + m11 * R.m11
        val n12 = m10 * R.m02 + m11 * R.m12 + m12
        m00 = n00
        m01 = n01
        m02 = n02
        m10 = n10
        m11 = n11
        m12 = n12
    }

    // = L * M
    fun preConcatenate(L : ITransform) {
        val n00 = L.m00 * m00 + L.m01 * m10
        val n01 = L.m00 * m01 + L.m01 * m11
        val n02 = L.m00 * m02 + L.m01 * m12 + L.m02
        val n10 = L.m10 * m00 + L.m11 * m10
        val n11 = L.m10 * m01 + L.m11 * m11
        val n12 = L.m10 * m02 + L.m11 * m12 + L.m12
        m00 = n00
        m01 = n01
        m02 = n02
        m10 = n10
        m11 = n11
        m12 = n12
    }

    fun set(other: ITransform) {
        m00 = other.m00
        m01 = other.m01
        m02 = other.m02
        m10 = other.m10
        m11 = other.m11
        m12 = other.m12
    }

    fun reset() {
        m00 = 1.0
        m01 = 0.0
        m02 = 0.0
        m10 = 0.0
        m11 = 1.0
        m12 = 0.0
    }

    override fun toImmutable() = ImmutableTransformD(m00, m01, m02, m10, m11, m12)
    override fun toMutable() = MutableTransformD(m00, m01, m02, m10, m11, m12)

    companion object {
        fun Translation( transX: Double, transY: Double) = MutableTransformD(
                1.0, 0.0, transX,
                0.0, 1.0, transY
        )
        fun Scale( scaleX: Double, scaleY: Double) = MutableTransformD(
                scaleX, 0.0, 0.0,
                0.0, scaleY, 0.0
        )
        fun RotationMatrix( theta: Double): MutableTransformD {
            val c = cos(theta)
            val s = sin(theta)
            return MutableTransformD(
                    c, -s, 0.0,
                    s, c, 0.0
            )
        }
        val Identity get() = MutableTransformD(1.0, 0.0, 0.0, 0.0, 1.0, 0.0)
    }
}

interface ITransformF : ITransform
{
    val m00f : Float
    val m01f : Float
    val m02f : Float
    val m10f : Float
    val m11f : Float
    val m12f : Float

    override val m00 : Double get() = m00f.d
    override val m01 : Double get() = m01f.d
    override val m02 : Double get() = m02f.d
    override val m10 : Double get() = m10f.d
    override val m11 : Double get() = m11f.d
    override val m12 : Double get() = m12f.d

    val determinantF get() = m00f*m11f - m01f*m10f

    operator fun times( tx : ITransformF) = ImmutableTransformF(
            m00f * tx.m00f + m01f * tx.m10f,
            m00f * tx.m01f + m01f * tx.m11f,
            m00f * tx.m02f + m01f * tx.m12f + m02f,
            m10f * tx.m00f + m11f * tx.m10f,
            m10f * tx.m01f + m11f * tx.m11f,
            m10f * tx.m02f + m11f * tx.m12f + m12f
    )

    override fun invert(): ImmutableTransformF? {
        val det = m00f*m11f - m01f*m10f
        if( det == 0f) return null

        val im00 = m11f / det
        val im10 = -m10f / det
        val im01 = -m01f / det
        val im11 = m00f / det
        val im02 = (m01f * m12f - m02f * m11f) / det
        val im12 = (-m00f * m12f + m10f * m02f) / det

        return ImmutableTransformF(im00, im01, im02, im10, im11, im12)
    }

    fun apply( v : Vec2f) = Vec2f(
            m00f * v.xf + m01f * v.yf + m02f,
            m10f * v.xf + m11f * v.yf + m12f
    )

    override fun toImmutable() : ImmutableTransformF
    override fun toMutable(): MutableTransformF

    companion object{
        val Identity get() = ImmutableTransformF.Identity
    }
}

data class ImmutableTransformF(
        override val m00f : Float,
        override val m01f : Float,
        override val m02f : Float,
        override val m10f : Float,
        override val m11f : Float,
        override val m12f : Float)
    : ITransformF, ImmutableTrasform()
{
    override fun toImmutable() = this
    override fun toMutable() = MutableTransformF(m00f, m01f, m02f, m10f, m11f, m12f)


    companion object {
        val Identity = ImmutableTransformF(1f, 0f, 0f, 0f, 1f, 0f)
        fun Translation( transX : Float, transY: Float) = ImmutableTransformF(
                1f, 0f, transX,
                0f, 1f, transY
        )

        fun Scale( scaleX : Float, scaleY: Float) = ImmutableTransformF(
                scaleX, 0f, 0f,
                0f, scaleY, 0f
        )

        fun Rotation( theta: Float) : ImmutableTransformF {
            val c = cos(theta)
            val s = sin(theta)
            return ImmutableTransformF(
                    c, -s, 0f,
                    s, c, 0f
            )
        }
    }
}


data class MutableTransformF(
        override var m00f : Float,
        override var m01f : Float,
        override var m02f : Float,
        override var m10f : Float,
        override var m11f : Float,
        override var m12f : Float)
    : ITransformF, MutableTransform()
{
    override fun toImmutable() = ImmutableTransformF(m00f, m01f, m02f, m10f, m11f, m12f)
    override fun toMutable() = copy()


    // = M * Translation(ox,oy)
    fun translate( ox: Float, oy: Float) {
        m02f += ox * m00f + oy * m01f
        m12f += ox * m10f + oy * m11f
    }

    // = Translation(ox,oy) * M
    fun preTranslate( ox: Float, oy: Float) {
        m02f += ox
        m12f += oy
    }

    // = M * Scale(sx, sy)
    fun scale( sx:Float, sy: Float) {
        m00f *= sx
        m01f *= sy
        m10f *= sx
        m11f *= sy
    }

    // = Scale(sx,sy) * M
    fun preScale(sx: Float, sy: Float) {
        m00f *= sx
        m01f *= sx
        m02f *= sx
        m10f *= sy
        m11f *= sy
        m12f *= sy
    }

    // = M * Rotate(theta)
    fun rotate( theta: Float) {
        val c = cos(theta)
        val s = sin(theta)
        val n00 = m00f * c + m01f * s
        val n01 = m00f * -s + m01f * c
        val n10 = m10f * c + m11f * s
        val n11 = m10f * -s + m11f * c
        m00f = n00
        m01f = n01
        m10f = n10
        m11f = n11
    }

    // = Rotate(theta) * M
    fun preRotate( theta: Float) {
        val c = cos(theta)
        val s = sin(theta)
        val n00 = c * m00f - s * m10f
        val n01 = c * m01f - s * m11f
        val n02 = c * m02f - s * m12f
        val n10 = s * m00f + c * m10f
        val n11 = s * m01f + c * m11f
        val n12 = s * m02f + c * m12f
        m00f = n00
        m01f = n01
        m02f = n02
        m10f = n10
        m11f = n11
        m12f = n12
    }

    // = M * R
    fun concatenate(R : ITransformF) {
        val n00 = m00f * R.m00f + m01f * R.m10f
        val n01 = m00f * R.m01f + m01f * R.m11f
        val n02 = m00f * R.m02f + m01f * R.m12f + m02f
        val n10 = m10f * R.m00f + m11f * R.m10f
        val n11 = m10f * R.m01f + m11f * R.m11f
        val n12 = m10f * R.m02f + m11f * R.m12f + m12f
        m00f = n00
        m01f = n01
        m02f = n02
        m10f = n10
        m11f = n11
        m12f = n12
    }

    // = L * M
    fun preConcatenate(L : ITransformF) {
        val n00 = L.m00f * m00f + L.m01f * m10f
        val n01 = L.m00f * m01f + L.m01f * m11f
        val n02 = L.m00f * m02f + L.m01f * m12f + L.m02f
        val n10 = L.m10f * m00f + L.m11f * m10f
        val n11 = L.m10f * m01f + L.m11f * m11f
        val n12 = L.m10f * m02f + L.m11f * m12f + L.m12f
        m00f = n00
        m01f = n01
        m02f = n02
        m10f = n10
        m11f = n11
        m12f = n12
    }

    fun reset() {
        m00f = 1f
        m01f = 0f
        m02f = 0f
        m10f = 0f
        m11f = 1f
        m12f = 0f
    }


    companion object {
        val Identity get() = MutableTransformF(1f, 0f, 0f, 0f, 1f, 0f)
        fun Translation( transX : Float, transY: Float) = MutableTransformF(
                1f, 0f, transX,
                0f, 1f, transY
        )

        fun Scale( scaleX : Float, scaleY: Float) = MutableTransformF(
                scaleX, 0f, 0f,
                0f, scaleY, 0f
        )

        fun Rotation( theta: Float) : MutableTransformF {
            val c = cos(theta)
            val s = sin(theta)
            return MutableTransformF(
                    c, -s, 0f,
                    s, c, 0f
            )
        }
    }
}