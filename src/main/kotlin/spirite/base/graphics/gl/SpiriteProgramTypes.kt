package spirite.base.graphics.gl

import rb.glow.gl.*
import rb.vectrix.linear.Vec3f
import rb.vectrix.linear.Vec4f
import rb.vectrix.mathUtil.f
import spirite.base.brains.toolset.ColorChangeMode
import spirite.base.brains.toolset.ColorChangeMode.*
import rb.glow.gle.IGLEngine.BlendMethod
import rb.glow.gle.IGLEngine.BlendMethod.*
import rb.glow.gle.IGlProgramCall

//STROKE_SPORE(MAX),
//STROKE_BASIC(SRC_OVER),

class SquareGradientCall(
        fixedAmount: Float,
        gradientType: GradientType)
    : IGlProgramCall
{
    override val uniforms: List<GLUniform>? = listOf(
            GLUniform1f("u_fixedAmmount", fixedAmount),
            GLUniform1i("u_typeCode", gradientType.ordinal))

    enum class GradientType { R, G, B, H, S, V}

    override val method: BlendMethod get() = MAX
    override val programKey: String get() = Key
    companion object { const val Key = "SQARE_GRADIENT"}
}

class ChangeColorCall(
        fromColor: Vec4f,
        toColor: Vec4f,
        changeMethod: ColorChangeMode)
    : IGlProgramCall
{
    override val uniforms: List<GLUniform>? = listOf(
            GLUniform4f("u_fromColor", fromColor),
            GLUniform4f("u_toColor", toColor),
            GLUniform1i("u_optionMask", when (changeMethod) {
                CHECK_ALL -> 0
                IGNORE_ALPHA -> 1
                AUTO -> 2
            }))

    override val method: BlendMethod get() = MAX
    override val programKey: String get() = Key
    companion object { const val Key = "CHANGE_COLOR"}
}

class GridCall(
        color1: Vec3f,
        color2: Vec3f,
        size: Int)
    : IGlProgramCall
{
    override val uniforms: List<GLUniform>? = listOf(
            GLUniform3f("u_Color1", color1),
            GLUniform3f("u_Color2", color2),
            GLUniform1i("u_Size", size)
    )

    override val programKey: String get() = Key
    companion object { const val Key = "GRID"}
}

class BorderCall(met : Int)
    : IGlProgramCall
{
    override val uniforms: List<GLUniform>? = listOf(GLUniform1i("u_cycle", met))

    override val method: BlendMethod get() = DEST_OVER
    override val programKey: String get() = Key
    companion object { const val Key = "PASS_BORDER"}
}

class InvertCall() : IGlProgramCall {
    override val uniforms: List<GLUniform>? get() = null

    override val method: BlendMethod get() = MAX
    override val programKey: String get() = Key
    companion object { const val Key = "PASS_INVERT"}
}

class StrokeV2LinePass(color: Vec3f)
    : IGlProgramCall
{

    override val uniforms: List<GLUniform>? = listOf(
            GLUniform3f("u_color", color))

    override val lineSmoothing: Boolean get() = true
    override val programKey: String get() = Key
    companion object { const val Key = "STROKE_V2_LINE_PASS"}
}
class StrokeV3LinePass : IGlProgramCall
{
    override val uniforms: List<GLUniform>? = null
    override val lineSmoothing: Boolean get() = true
    override val programKey: String get() = Key
    companion object { const val Key = "STROKE_V3_LINE_PASS"}
    
}

class StrokeV2ApplyCall(
        color: Vec3f,
        alpha: Float,
        intensifyMethod: IntensifyMethod)
    : IGlProgramCall
{

    enum class IntensifyMethod(val code: Int) {
        DEFAULT(0),
        HARD_EDGED(1),
    }
    override val uniforms: List<GLUniform>? = listOf(
            GLUniform3f("u_color", color),
            GLUniform1f("u_alpha", alpha),
            GLUniform1i("u_intensifyMode", intensifyMethod.code))

    override val programKey: String get() = Key
    companion object { const val Key = "STROKE_INTENSIFY"}
}

class StrokeApplyCall(
        color: Vec3f,
        alpha: Float)
    : IGlProgramCall
{
    override val uniforms: List<GLUniform>? = listOf(
            GLUniform3f("u_color", color),
            GLUniform1f("u_alpha", alpha))

    override val programKey: String get() = Key
    companion object { const val Key = "STROKE_APPLY"}
}


class StrokePixelCall( color: Vec3f)
    : IGlProgramCall
{
    override val uniforms: List<GLUniform>? = listOf(
            GLUniform3f("u_color", color))

    override val programKey: String get() = Key
    companion object { const val Key = "STROKE_PIXEL"}
}

class FillAfterpassCall(color: Vec4f, width: Int, height: Int)
    : IGlProgramCall
{
    override val uniforms: List<GLUniform>? = listOf(
            GLUniform4f("u_color", color),
            GLUniform1i("u_width", width),
            GLUniform1i("u_height", height),
            GLUniform1f("u_wratio", width / (((width - 1) / 8 + 1) * 8).f),
            GLUniform1f("u_hratio", height / (((height - 1) / 4 + 1) * 4).f))

    override val programKey: String get() = Key
    companion object { const val Key = "FILL_AFTERPASS"}
}