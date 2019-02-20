package spirite.hybrid.inputSystems

interface IKeypressSystem
{
    val holdingSpace: Boolean
}

interface MKeypressSystem : IKeypressSystem
{
    override var holdingSpace: Boolean
}

class KeypressSystem : MKeypressSystem {
    override var holdingSpace: Boolean = false
}