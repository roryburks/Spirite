package spirite.hybrid

import spirite.base.graphics.gl.GLEngine
import spirite.base.graphics.gl.IGL
import spirite.gui.components.basic.IComponentProvider
import spirite.pc.gui.SwingComponentProvider
import spirite.pc.JOGL.JOGLProvider


interface IHybrid {
    val system : ISystemUtils
    val imageCreator : IImageCreator
    val imageConverter : ImageConverter
    val timing : ITimerEngine
    val ui : IComponentProvider

    val gl : IGL
    val gle : GLEngine

    fun beep()
}

object Hybrid : IHybrid {
    override val ui: IComponentProvider get() = SwingComponentProvider
    override val timing: ITimerEngine get() = STimerEngine
    override val gle: GLEngine = EngineLaunchpoint.gle
    override val gl: IGL get() = JOGLProvider.gl
    override val system: ISystemUtils get() = JSystemUtils
    override val imageCreator: IImageCreator get() = EngineLaunchpoint
    override val imageConverter: ImageConverter get() = ImageConverter(EngineLaunchpoint.gle)

    override fun beep() {
        java.awt.Toolkit.getDefaultToolkit().beep()
    }
}