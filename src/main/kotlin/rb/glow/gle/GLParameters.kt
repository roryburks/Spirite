package rb.glow.gle

import rb.glow.gl.GLImage
import rb.vectrix.shapes.RectI

data class GLParameters(
        var width : Int,
        var heigth: Int,
        var flip: Boolean = false,
        var clipRect : RectI? = null,
        var premultiplied: Boolean = true,

        var texture1 : GLImage? = null,
        var texture2 : GLImage? = null,

        var useBlendMode: Boolean = true,
        var useDefaultBlendMode: Boolean = true,
        var bm_sfc: Int = 0,
        var bm_sfa: Int = 0,
        var bm_dfc: Int = 0,
        var bm_dfa: Int = 0,
        var bm_fc: Int = 0,
        var bm_fa: Int = 0
) {
    fun setBlendMode(src_factor: Int, dst_factor: Int, formula: Int) {
        useDefaultBlendMode = false
        bm_sfa = src_factor
        bm_sfc = bm_sfa
        bm_dfa = dst_factor
        bm_dfc = bm_dfa
        bm_fa = formula
        bm_fc = bm_fa
    }

    fun setBlendModeExt(
            src_factor_color: Int, dst_factor_color: Int, formula_color: Int,
            src_factor_alpha: Int, dst_factor_alpha: Int, formula_alpha: Int) {
        useDefaultBlendMode = false
        bm_sfc = src_factor_color
        bm_dfc = dst_factor_color
        bm_fc = formula_color

        bm_sfa = src_factor_alpha
        bm_dfa = dst_factor_alpha
        bm_fa = formula_alpha

    }
}