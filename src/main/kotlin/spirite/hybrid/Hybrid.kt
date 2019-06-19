package spirite.hybrid

import rb.glow.gl.IGL
import sgui.components.IComponentProvider
import sgui.systems.IMouseSystem
import sguiSwing.SwingComponentProvider
import sguiSwing.mouseSystem.SwMouseSystem
import rb.glow.gle.IGLEngine
import rbJvm.glow.awt.AwtImageConverter
import spirite.hybrid.Transferables.IClipboard
import spirite.hybrid.Transferables.SwClipboard
import sgui.systems.IKeypressSystem
import sgui.systems.KeypressSystem
import sgui.systems.MKeypressSystem
import rbJvm.glow.jogl.JOGLProvider

/** Hybrid is a collection of components and interfaces that are platform-specific (things like Timers, GUI libraries,
 * OpenGL implementations, etc).  It wraps them such that they can code can be as portable as possible.
 */
interface IHybrid {
    val imageCreator : IImageCreator
    val imageConverter : AwtImageConverter
    val timing : ITimerEngine
    val ui : IComponentProvider
    val imageIO : IImageIO
    val clipboard : IClipboard

    val mouseSystem : IMouseSystem
    val keypressSystem : IKeypressSystem

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
    override val imageConverter: AwtImageConverter get() = AwtImageConverter{EngineLaunchpoint.gle}
    override val imageIO: IImageIO get() = JImageIO
    override val clipboard: IClipboard get() = SwClipboard

    override val mouseSystem: IMouseSystem get() = SwMouseSystem
    override val keypressSystem: MKeypressSystem = KeypressSystem

    override fun LockFrom(o: Any): ILock = JLock(o)
    override fun beep() {
        java.awt.Toolkit.getDefaultToolkit().beep()
    }
}