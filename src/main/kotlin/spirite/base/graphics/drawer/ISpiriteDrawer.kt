package spirite.base.graphics.drawer

import spirite.base.graphics.GraphicsContext
import spirite.base.graphics.gl.GLGraphicsContext

class SpiriteDrawer( private val gc: GraphicsContext) {
    fun drawTransparencyBg(x: Int, y: Int, w: Int, h: Int, squareSize: Int) {
        if( gc is GLGraphicsContext) {
            gc.drawTransparencyBG(x, y, w, h, squareSize)
        }
    }
}