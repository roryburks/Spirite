package spirite.base.graphics.gl

import rb.vectrix.linear.Vec3f
import rb.vectrix.linear.Vec4f
import rb.vectrix.mathUtil.f
import spirite.base.brains.toolset.ColorChangeMode
import spirite.base.brains.toolset.ColorChangeMode.*
import spirite.base.graphics.JoinMethod
import spirite.base.graphics.JoinMethod.*
import spirite.base.graphics.gl.IGLEngine.BlendMethod
import spirite.base.graphics.gl.IGLEngine.BlendMethod.*
import spirite.base.graphics.gl.ProgramType.*
import spirite.base.graphics.gl.SquareGradientCall.Companion

//STROKE_SPORE(MAX),
//STROKE_BASIC(SRC_OVER),
internal enum class ProgramType(
        internal val method: BlendMethod
) {
}

interface IGlProgramCall {
    val uniforms: List<GLUniform>?
    val programKey: String
    val method: BlendMethod get() = SRC_OVER
    val lineSmoothing : Boolean get() = false
}

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
            GLUniform1i("u_optionMask", when( changeMethod) {
                CHECK_ALL -> 0
                IGNORE_ALPHA -> 1
                AUTO -> 2
            }))

    override val method: BlendMethod get() = MAX
    override val programKey: String get() = SquareGradientCall.Key
    companion object { const val Key = "CHANGE_COLOR"}
}

class GridCall(
        color1: Vec3f,
        color2: Vec3f,
        size: Int)
    : IGlProgramCall
{
    override val uniforms: List<GLUniform>? = listOf(
            GLUniform3f( "u_Color1", color1),
            GLUniform3f( "u_Color2", color2),
            GLUniform1i( "u_Size", size)
    )

    override val programKey: String get() = SquareGradientCall.Key
    companion object { const val Key = "GRID"}
}

class BasicCall() : IGlProgramCall {
    override val uniforms: List<GLUniform>? = null

    override val programKey: String get() = SquareGradientCall.Key
    companion object { const val Key = "PASS_BASIC"}
}

class BorderCall(met : Int)
    : IGlProgramCall
{
    override val uniforms: List<GLUniform>? = listOf(GLUniform1i("u_cycle", met))

    override val method: BlendMethod get() = DEST_OVER
    override val programKey: String get() = SquareGradientCall.Key
    companion object { const val Key = "PASS_BORDER"}
}

class InvertCall() : IGlProgramCall {
    override val uniforms: List<GLUniform>? get() = null

    override val method: BlendMethod get() = MAX
    override val programKey: String get() = SquareGradientCall.Key
    companion object { const val Key = "PASS_INVERT"}
}

class RenderCall(
        alpha: Float,
        calls: List<Pair<RenderAlgorithm, Int>>)
    : IGlProgramCall
{
    val MAX_CALLS = 10

    enum class RenderAlgorithm( val progId: Int) {
        //STRAIGHT_PASS(0), // Adding this would be redundant
        AS_COLOR(1),
        AS_COLOR_ALL(2),
        DISSOLVE(3)
    }

    override val uniforms: List<GLUniform>? = listOf(
            GLUniform1f("u_alpha", alpha),
            GLUniform1iv("u_values", IntArray(MAX_CALLS, {calls.getOrNull(it)?.second ?: 0})),
            GLUniform1iv( "u_composites", IntArray(MAX_CALLS, {calls.getOrNull(it)?.first?.progId ?: 0}))
    )

    override val programKey: String get() = SquareGradientCall.Key
    companion object { const val Key = "PASS_RENDER"}
}

class StrokeV2LinePass(color: Vec3f)
    : IGlProgramCall
{

    override val uniforms: List<GLUniform>? = listOf(
            GLUniform3f("u_color", color))

    override val lineSmoothing: Boolean get() = true
    override val programKey: String get() = SquareGradientCall.Key
    companion object { const val Key = "STROKE_V2_LINE_PASS"}
}
class StrokeV3LinePass : IGlProgramCall
{
    override val uniforms: List<GLUniform>? = null
    override val lineSmoothing: Boolean get() = true
    override val programKey: String get() = SquareGradientCall.Key
    companion object { const val Key = "STROKE_V3_LINE_PASS"}
    
}

class StrokeV2ApplyCall(
        color: Vec3f,
        alpha: Float,
        intensifyMethod: IntensifyMethod)
    :IGlProgramCall
{

    enum class IntensifyMethod(val code: Int) {
        DEFAULT(0),
        HARD_EDGED(1),
    }
    override val uniforms: List<GLUniform>? = listOf(
            GLUniform3f( "u_color", color),
            GLUniform1f("u_alpha", alpha),
            GLUniform1i("u_intensifyMode", intensifyMethod.code))

    override val programKey: String get() = SquareGradientCall.Key
    companion object { const val Key = "STROKE_INTENSIFY"}
}

class StrokeApplyCall(
        color: Vec3f,
        alpha: Float)
    :IGlProgramCall
{
    override val uniforms: List<GLUniform>? = listOf(
            GLUniform3f( "u_color", color),
            GLUniform1f("u_alpha", alpha))

    override val programKey: String get() = SquareGradientCall.Key
    companion object { const val Key = "STROKE_APPLY"}
}


class PolyRenderCall(
        color: Vec3f,
        alpha: Float)
    :IGlProgramCall
{
    override val uniforms: List<GLUniform>? = listOf(
            GLUniform3f( "u_color", color),
            GLUniform1f("u_alpha", alpha))

    override val programKey: String get() = SquareGradientCall.Key
    companion object { const val Key = "POLY_RENDER"}
}

class LineRenderCall(
        joinMethod: JoinMethod,
        lineWidth: Float,
        color: Vec3f,
        alpha: Float)
    : IGlProgramCall
{
    override val uniforms: List<GLUniform>? = listOf(
            GLUniform1i("u_join", when( joinMethod) {
                BEVEL -> 1 // 2
                MITER -> 1
                ROUNDED -> 1 // 0
            }),
            GLUniform1f("u_width", lineWidth/2f),
            GLUniform3f( "u_color", color),
            GLUniform1f("u_alpha", alpha))

    override val programKey: String get() = SquareGradientCall.Key
    companion object { const val Key = "LINE_RENDER"}
}


class StrokePixelCall( color: Vec3f)
    :IGlProgramCall
{
    override val uniforms: List<GLUniform>? = listOf(
            GLUniform3f( "u_color", color))

    override val programKey: String get() = SquareGradientCall.Key
    companion object { const val Key = "STROKE_PIXEL"}
}

class FillAfterpassCall(color: Vec4f, width: Int, height: Int)
    :IGlProgramCall
{
    override val uniforms: List<GLUniform>? = listOf(
            GLUniform4f( "u_color", color),
            GLUniform1i("u_width", width),
            GLUniform1i("u_height", height),
            GLUniform1f("u_wratio", width / (((width-1)/8+1)*8).f),
            GLUniform1f("u_hratio", height / (((height-1)/4+1)*4).f))

    override val programKey: String get() = SquareGradientCall.Key
    companion object { const val Key = "FILL_AFTERPASS"}
}