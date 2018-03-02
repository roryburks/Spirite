package spirite.base.graphics.gl.stroke

import spirite.base.graphics.gl.GLEngine
import spirite.base.pen.stroke.IStrokeDrawer
import spirite.base.pen.stroke.IStrokeDrawerProvider
import spirite.base.pen.stroke.StrokeParams

class GLStrokeDrawerProvider( val gle: GLEngine) : IStrokeDrawerProvider {
    override fun getStrokeDrawer(strokeParams: StrokeParams) = GLStrokeDrawerV2(gle)
}