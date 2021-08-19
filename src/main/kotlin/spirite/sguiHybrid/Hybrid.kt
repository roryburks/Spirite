package spirite.sguiHybrid

import rb.glow.IImageConverter
import rb.glow.gl.IGL
import rb.glow.gle.IGLEngine
import rbJvm.glow.awt.AwtImageConverter
import rbJvm.glow.jogl.JOGLProvider
import sgui.core.components.IComponentProvider
import sgui.core.systems.*
import sgui.swing.SwTimerEngine
import sgui.swing.SwingComponentProvider
import sgui.swing.systems.JImageIO
import sgui.swing.systems.JLock
import sgui.swing.systems.SwImageCreator
import sgui.swing.systems.mouseSystem.SwMouseSystem
import spirite.sguiHybrid.transferables.IClipboard
import spirite.sguiHybrid.transferables.SwClipboard

/** Hybrid is a collection of components and interfaces that are platform-specific (things like Timers, GUI libraries,
 * OpenGL implementations, etc).  It wraps them such that they can code can be as portable as possible.
 */
interface IHybrid {
    val imageConverter : IImageConverter
    val timing : ITimerEngine
    val ui : IComponentProvider
    val imageIO : IImageIO
    val clipboard : IClipboard

    val mouseSystem : IMouseSystem
    val keypressSystem : IKeypressSystem

    val gl : IGL
    val gle : IGLEngine

    fun LockFrom( o: Any) : ILock
}

val Hybrid : IHybrid get() = SwHybrid

object SwHybrid : IHybrid {

    override val ui: IComponentProvider get() = SwingComponentProvider
    override val timing: ITimerEngine get() = SwTimerEngine
    override val gle: IGLEngine = EngineLaunchpoint.gle
    override val gl: IGL get() = JOGLProvider.gl
    override val imageConverter: AwtImageConverter get() = AwtImageConverter{EngineLaunchpoint.gle}
    override val imageIO: IImageIO get() = JImageIO(imageConverter)
    override val clipboard: IClipboard get() = SwClipboard

    override val mouseSystem: IMouseSystem get() = SwMouseSystem
    override val keypressSystem: MKeypressSystem = KeypressSystem

    override fun LockFrom(o: Any): ILock = JLock(o)
}