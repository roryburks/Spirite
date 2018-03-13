package spirite.hybrid

import spirite.base.graphics.gl.GLEngine
import spirite.base.graphics.gl.IGL
import spirite.pc.JOGL.JOGLProvider


interface IHybrid {
    val system : ISystemUtils
    val imageCreator : IImageCreator
    val imageConverter : ImageConverter
    val timing : ITimerEngine

    val gl : IGL
    val gle : GLEngine
}

object Hybrid : IHybrid {
    override val timing: ITimerEngine get() = STimerEngine
    override val gle: GLEngine = EngineLaunchpoint.gle
    override val gl: IGL get() = JOGLProvider.gl
    override val system: ISystemUtils get() = JSystemUtils
    override val imageCreator: IImageCreator get() = EngineLaunchpoint
    override val imageConverter: ImageConverter get() = ImageConverter(EngineLaunchpoint.gle)


}