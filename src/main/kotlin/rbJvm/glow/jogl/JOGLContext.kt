package rbJvm.glow.jogl

import rb.glow.gl.IGL
import rb.glow.gle.IGLContext
import javax.swing.SwingUtilities

/// JOGLContext is a wrapper for determining whether or not the JPGL is currently in context or not and to avoid
/// double-releasing context.
class JOGLContext  : IGLContext {
    var inContext = false

    override val glGetter: () -> IGL get() = { JOGLProvider.gl }

    override fun runOnGLThread(run: () -> Unit) {
        SwingUtilities.invokeLater{ runInGLContext(run) }
    }

    override fun runInGLContext(run: () -> Unit) {
        if(!inContext) {
            JOGLProvider.context.makeCurrent()
            inContext = true
        }
        run()
        if( inContext) {
            JOGLProvider.context.release()
            inContext = false
        }
    }
}