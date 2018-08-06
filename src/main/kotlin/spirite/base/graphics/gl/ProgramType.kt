package spirite.base.graphics.gl

import spirite.base.brains.toolset.ColorChangeMode
import spirite.base.brains.toolset.ColorChangeMode.*
import spirite.base.graphics.JoinMethod
import spirite.base.graphics.JoinMethod.*
import spirite.base.graphics.gl.IGLEngine.BlendMethod
import spirite.base.graphics.gl.IGLEngine.BlendMethod.*
import spirite.base.graphics.gl.ProgramType.*
import spirite.base.util.f
import spirite.base.util.linear.Vec3
import spirite.base.util.linear.Vec4

internal enum class ProgramType(
        internal val method: BlendMethod
) {
    SQARE_GRADIENT(MAX),
    CHANGE_COLOR(MAX),
    GRID(SRC_OVER),

    PASS_BASIC(SRC_OVER),
    PASS_BORDER(DEST_OVER),
    PASS_INVERT(MAX),
    PASS_RENDER(SRC_OVER),

    STROKE_SPORE(MAX),
    STROKE_BASIC(SRC_OVER),
    STROKE_PIXEL(SRC_OVER),
    STROKE_V2_LINE_PASS(SRC_OVER),
    STROKE_INTENSIFY(SRC_OVER),
    STROKE_APPLY(SRC_OVER),
    STROKE_V3_LINE_PASS(SRC_OVER),

    POLY_RENDER(SRC_OVER),
    LINE_RENDER(SRC_OVER),

    FILL_AFTERPASS(SRC_OVER),
}

sealed abstract class ProgramCall {
    abstract val uniforms: List<GLUniform>?

    internal abstract val programType: ProgramType
}


class SquareGradientCall(
        fixedAmount: Float,
        gradientType: GradientType)
    : ProgramCall()
{
    override val uniforms: List<GLUniform>? = listOf(
            GLUniform1f("u_fixedAmmount", fixedAmount),
            GLUniform1i("u_typeCode", gradientType.ordinal))

    enum class GradientType { R, G, B, H, S, V}

    override val programType: ProgramType = SQARE_GRADIENT
}

class ChangeColorCall(
        fromColor: Vec4,
        toColor: Vec4,
        changeMethod: ColorChangeMode)
    : ProgramCall()
{
    override val uniforms: List<GLUniform>? = listOf(
            GLUniform4f("u_fromColor", fromColor),
            GLUniform4f("u_toColor", toColor),
            GLUniform1i("u_optionMask", when( changeMethod) {
                CHECK_ALL -> 0
                IGNORE_ALPHA -> 1
                AUTO -> 2
            }))

    override val programType: ProgramType = CHANGE_COLOR
}

class GridCall(
        color1: Vec3,
        color2: Vec3,
        size: Int)
    : ProgramCall()
{
    override val uniforms: List<GLUniform>? = listOf(
            GLUniform3f( "u_Color1", color1),
            GLUniform3f( "u_Color2", color2),
            GLUniform1i( "u_Size", size)
    )
    override val programType: ProgramType = GRID
}

class BasicCall() : ProgramCall() {
    override val uniforms: List<GLUniform>? = null
    override val programType: ProgramType get() = PASS_BASIC
}

class BorderCall(met : Int)
    : ProgramCall()
{
    override val uniforms: List<GLUniform>? = listOf(GLUniform1i("u_cycle", met))
    override val programType: ProgramType get() = PASS_BORDER
}

class InvertCall() : ProgramCall() {
    override val uniforms: List<GLUniform>? get() = null
    override val programType: ProgramType get() = PASS_INVERT
}

class RenderCall(
        alpha: Float,
        calls: List<Pair<RenderAlgorithm, Int>>)
    : ProgramCall()
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

    override val programType: ProgramType get() = PASS_RENDER
}

class StrokeV2LinePass(color: Vec3)
    : ProgramCall()
{

    override val uniforms: List<GLUniform>? = listOf(
            GLUniform3f("u_color", color))

    override val programType: ProgramType get() = STROKE_V2_LINE_PASS
}
class StrokeV3LinePass : ProgramCall()
{
    override val uniforms: List<GLUniform>? = null
    override val programType: ProgramType get() = STROKE_V3_LINE_PASS
}

class StrokeV2ApplyCall(
        color: Vec3,
        alpha: Float,
        intensifyMethod: IntensifyMethod)
    :ProgramCall()
{

    enum class IntensifyMethod(val code: Int) {
        DEFAULT(0),
        HARD_EDGED(1),
    }
    override val uniforms: List<GLUniform>? = listOf(
            GLUniform3f( "u_color", color),
            GLUniform1f("u_alpha", alpha),
            GLUniform1i("u_intensifyMode", intensifyMethod.code))
    override val programType: ProgramType get() = STROKE_INTENSIFY
}

class StrokeApplyCall(
        color: Vec3,
        alpha: Float)
    :ProgramCall()
{
    override val uniforms: List<GLUniform>? = listOf(
            GLUniform3f( "u_color", color),
            GLUniform1f("u_alpha", alpha))
    override val programType: ProgramType get() = STROKE_APPLY
}


class PolyRenderCall(
        color: Vec3,
        alpha: Float)
    :ProgramCall()
{
    override val uniforms: List<GLUniform>? = listOf(
            GLUniform3f( "u_color", color),
            GLUniform1f("u_alpha", alpha))
    override val programType: ProgramType get() = POLY_RENDER
}

class LineRenderCall(
        joinMethod: JoinMethod,
        lineWidth: Float,
        color: Vec3,
        alpha: Float)
    : ProgramCall()
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

    override val programType: ProgramType get() = LINE_RENDER
}


class StrokePixelCall( color: Vec3)
    :ProgramCall()
{
    override val uniforms: List<GLUniform>? = listOf(
            GLUniform3f( "u_color", color))
    override val programType: ProgramType get() = STROKE_PIXEL
}

class FillAfterpassCall( color: Vec4, width: Int, height: Int)
    :ProgramCall()
{
    override val uniforms: List<GLUniform>? = listOf(
            GLUniform4f( "u_color", color),
            GLUniform1i("u_width", width),
            GLUniform1i("u_height", height),
            GLUniform1f("u_wratio", width / (((width-1)/8+1)*8).f),
            GLUniform1f("u_hratio", height / (((height-1)/4+1)*4).f))
    override val programType: ProgramType get() = FILL_AFTERPASS
}