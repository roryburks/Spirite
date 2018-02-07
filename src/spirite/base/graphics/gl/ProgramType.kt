package spirite.base.graphics.gl

import spirite.base.graphics.JoinMethod
import spirite.base.graphics.JoinMethod.*
import spirite.base.graphics.gl.GLEngine.BlendMethod
import spirite.base.graphics.gl.GLEngine.BlendMethod.*
import spirite.base.graphics.gl.ProgramType.*
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
    PASS_ESCALATE(SRC_OVER),

    STROKE_SPORE(MAX),
    STROKE_BASIC(SRC_OVER),
    STROKE_PIXEL(SRC_OVER),
    STROKE_V2_LINE_PASS(SRC_OVER),
    STROKE_AFTERPASS_INTENSIFY(SOURCE),

    POLY_RENDER(SRC_OVER),
    LINE_RENDER(SRC_OVER),
}

sealed abstract class ProgramCall {
    abstract val uniforms: List<GLUniform>?

    internal abstract val programType: ProgramType
}


class SquareGradientCall(
        fixedAmount: Float,
        gradientType: GradientType
): ProgramCall() {
    override val uniforms: List<GLUniform>? = listOf(
            GLUniform1f("u_fixedAmmount", fixedAmount),
            GLUniform1i("u_typeCode", gradientType.ordinal))

    enum class GradientType { R, G, B, H, S, V}

    override val programType: ProgramType = SQARE_GRADIENT
}

class ChangeColorCall(
        fromColor: Vec4,
        toColor: Vec4
) : ProgramCall() {
    override val uniforms: List<GLUniform>? = listOf(
            GLUniform4f("u_fromColor", fromColor),
            GLUniform4f("u_toColor", fromColor))

    override val programType: ProgramType = CHANGE_COLOR
}

class GridCall(
        color1: Vec3,
        color2: Vec3
) : ProgramCall() {
    override val uniforms: List<GLUniform>? = listOf(
            GLUniform3f( "u_Color1", color1),
            GLUniform3f( "u_Color2", color2)
    )
    override val programType: ProgramType = GRID
}

class BasicCall() : ProgramCall() {
    override val uniforms: List<GLUniform>? = null
    override val programType: ProgramType get() = PASS_BASIC
}

class BorderCall(
        met : Int
): ProgramCall() {
    override val uniforms: List<GLUniform>? = listOf(GLUniform1i("u_cycle", met))
    override val programType: ProgramType get() = PASS_BORDER
}


class PolyRenderCall(
        color: Vec3,
        alpha: Float
):ProgramCall() {
    override val uniforms: List<GLUniform>? = listOf(
            GLUniform3f( "u_color", color),
            GLUniform1f("u_alpha", alpha))
    override val programType: ProgramType get() = POLY_RENDER
}

class LineRenderCall(
        joinMethod: JoinMethod,
        lineWidth: Float,
        color: Vec3,
        alpha: Float
) : ProgramCall() {
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