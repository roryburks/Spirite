package spirite.hybrid

import sun.plugin2.util.SystemUtil


interface IHybrid {
    val system : ISystemUtils
    val imageCreator : IImageCreator
}

object Hybrid : IHybrid {
    override val system: ISystemUtils get() = JSystemUtils
    override val imageCreator: IImageCreator get() = EngineLaunchpoint

}