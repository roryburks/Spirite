package spirite.hybrid

interface ISystemUtils {
    val currentMilliseconds : Long
}

object JSystemUtils : ISystemUtils{
    override val currentMilliseconds: Long get() = System.currentTimeMillis()

}