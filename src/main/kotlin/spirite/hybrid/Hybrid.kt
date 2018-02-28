package spirite.hybrid


interface IHybrid {
    val system : ISystemUtils
    val imageCreator : IImageCreator
}

object Hybrid : IHybrid {
    override val system: ISystemUtils get() = JSystemUtils
    override val imageCreator: IImageCreator get() = EngineLaunchpoint

}