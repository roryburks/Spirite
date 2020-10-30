package spirite.specialRendering

import rb.glow.gl.shader.GlShaderLoadContract
import rb.glow.gl.shader.programs.BasicCall
import rb.glow.gl.shader.programs.LineRenderCall
import rb.glow.gl.shader.programs.PolyRenderCall
import rb.glow.gl.shader.programs.RenderCall

private val root = "shaders/330"

object ShaderMapping {
    val Map330 get() = mapOf(
            BasicCall.Key to GlShaderLoadContract(
                    "${root}/brushes/stroke_basic.vert",
                    "${root}/brushes/stroke_basic.geom",
                    "${root}/brushes/stroke_basic.frag"),
            StrokeV2LinePass.Key to GlShaderLoadContract(
                    "${root}/brushes/stroke_pixel.vert",
                    null,
                    "${root}/brushes/stroke_pixel.frag"),
            StrokePixelCall.Key to GlShaderLoadContract(
                    "${root}/brushes/stroke_pixel.vert",
                    null,
                    "${root}/brushes/stroke_pixel.frag"),
            StrokeV2ApplyCall.Key to GlShaderLoadContract(
                    "${root}/pass.vert",
                    null,
                    "${root}/brushes/stroke_intensify.frag"),
            StrokeApplyCall.Key to GlShaderLoadContract(
                    "${root}/pass.vert",
                    null,
                    "${root}/brushes/stroke_apply.frag"),
            StrokeV3LinePass.Key to GlShaderLoadContract(
                    "${root}/brushes/stroke_v3_line.vert",
                    null,
                    "${root}/brushes/stroke_v3_line.frag"),

            // Constructions
            SquareGradientCall.Key to GlShaderLoadContract(
                    "${root}/pass.vert",
                    null,
                    "${root}/constructions/square_grad.frag"),
            GridCall.Key to GlShaderLoadContract(
                    "${root}/pass.vert",
                    null,
                    "${root}/constructions/pass_grid.frag"),

            // Shapes
            PolyRenderCall.Key to GlShaderLoadContract(
                    "${root}/shapes/poly_render.vert",
                    null,
                    "${root}/shapes/shape_render.frag"),
            LineRenderCall.Key to GlShaderLoadContract(
                    "${root}/shapes/line_render.vert",
                    "${root}/shapes/line_render.geom",
                    "${root}/shapes/shape_render.frag"),

            // Filters
            ChangeColorCall.Key to GlShaderLoadContract(
                    "${root}/pass.vert",
                    null,
                    "${root}/filters/pass_change_color.frag"),
            InvertCall.Key to GlShaderLoadContract(
                            "${root}/pass.vert",
                            null,
                            "${root}/filters/pass_invert.frag"),

            // Special
            FillAfterpassCall.Key to GlShaderLoadContract(
                    "${root}/pass.vert",
                    null,
                    "${root}/special/pass_fill.frag"),
            BorderCall.Key to GlShaderLoadContract(
                    "${root}/pass.vert",
                    null,
                    "${root}/special/pass_border.frag"),

            // Render
            RenderCall.Key to GlShaderLoadContract(
                    "${root}/pass.vert",
                    null,
                    "${root}/render/pass_render.frag"),
            BasicCall.Key to GlShaderLoadContract(
                    "${root}/pass.vert",
                    null,
                    "${root}/render/pass_basic.frag")

    )
}