package spirite.base.graphics.gl

class GLEngine(
        internal val gl: IGL) {


    var target: IGLTexture? = null

    fun runOnGLThread( run: () -> Unit) {

    }
}