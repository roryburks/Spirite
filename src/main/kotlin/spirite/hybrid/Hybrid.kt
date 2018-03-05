package spirite.hybrid

import spirite.base.graphics.gl.GLEngine
import spirite.base.graphics.gl.IGL
import spirite.pc.JOGL.JOGLProvider


interface IHybrid {
    val system : ISystemUtils
    val imageCreator : IImageCreator
    val imageConverter : ImageConverter

    val gl : IGL
    val gle : GLEngine
}

object Hybrid : IHybrid {
    override val gle: GLEngine = EngineLaunchpoint.gle
    override val gl: IGL get() = JOGLProvider.getGL()
    override val system: ISystemUtils get() = JSystemUtils
    override val imageCreator: IImageCreator get() = EngineLaunchpoint
    override val imageConverter: ImageConverter get() = ImageConverter(EngineLaunchpoint.gle)

}