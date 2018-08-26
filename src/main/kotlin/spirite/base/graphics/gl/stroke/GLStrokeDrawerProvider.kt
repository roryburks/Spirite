package spirite.base.graphics.gl.stroke

import spirite.base.graphics.gl.IGLEngine
import spirite.base.pen.stroke.IStrokeDrawerProvider
import spirite.base.pen.stroke.StrokeParams
import spirite.base.pen.stroke.StrokeParams.Method.PIXEL

class GLStrokeDrawerProvider( val gle: IGLEngine) : IStrokeDrawerProvider {
    override fun getStrokeDrawer(strokeParams: StrokeParams) = when(strokeParams.method) {
        PIXEL -> GLStrikeDrawerPixel(gle)
        else -> GLStrokeDrawerV3_b(gle)
    }
}