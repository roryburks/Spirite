package rbJvm.glow.jogl

import rb.glow.gl.IGL
import rb.glow.gle.IGLContext
import javax.swing.SwingUtilities

class JOGLContext  : IGLContext {
    override val glGetter: () -> IGL get() = { JOGLProvider.gl }

    override fun runOnGLThread(run: () -> Unit) {
        SwingUtilities.invokeLater{
            JOGLProvider.context.makeCurrent()
            run()
            JOGLProvider.context.release()
        }
    }

    override fun runInGLContext(run: () -> Unit) {
        JOGLProvider.context.makeCurrent()
        run()
        JOGLProvider.context.release()
    }
}