package spirite.hybrid

import spirite.base.graphics.gl.IGLEngine
import spirite.base.graphics.gl.IGL
import spirite.gui.components.basic.IComponentProvider
import spirite.pc.JOGL.JOGLProvider
import spirite.pc.gui.SwingComponentProvider

/** Hybrid is a collection of components and interfaces that are platform-specific (things like Timers, GUI libraries,
 * OpenGL implementations, etc).  It wraps them such that they can code can be as portable as possible.
 */
interface IHybrid {
    val imageCreator : IImageCreator
    val imageConverter : ImageConverter
    val timing : ITimerEngine
    val ui : IComponentProvider
    val imageIO : IImageIO

    val gl : IGL
    val gle : IGLEngine

    fun LockFrom( o: Any) : ILock
    fun beep()
}

val Hybrid : IHybrid get() = SwHybrid

object SwHybrid : IHybrid {

    override val ui: IComponentProvider get() = SwingComponentProvider
    override val timing: ITimerEngine get() = SwTimerEngine
    override val gle: IGLEngine = EngineLaunchpoint.gle
    override val gl: IGL get() = JOGLProvider.gl
    override val imageCreator: IImageCreator get() = SwImageCreator
    override val imageConverter: ImageConverter get() = ImageConverter(EngineLaunchpoint.gle)
    override val imageIO: IImageIO get() = JImageIO

    override fun LockFrom(o: Any): ILock = JLock(o)
    override fun beep() {
        java.awt.Toolkit.getDefaultToolkit().beep()
    }
}