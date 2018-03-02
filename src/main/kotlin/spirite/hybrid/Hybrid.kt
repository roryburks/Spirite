package spirite.hybrid

import spirite.base.graphics.gl.IGL
import spirite.pc.JOGL.JOGLProvider


interface IHybrid {
    val system : ISystemUtils
    val imageCreator : IImageCreator

    val gl : IGL
}

object Hybrid : IHybrid {
    override val gl: IGL get() = JOGLProvider.getGL()
    override val system: ISystemUtils get() = JSystemUtils
    override val imageCreator: IImageCreator get() = EngineLaunchpoint

}