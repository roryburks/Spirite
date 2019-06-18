package rb.glow.gle

import rb.glow.gl.IGL

interface IGLContext {
    val glGetter: () -> IGL
    fun runOnGLThread( run: ()->Unit)
    fun runInGLContext(run: ()->Unit)
}