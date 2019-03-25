package spirite.hybrid.inputSystems

interface IKeypressSystem
{
    val holdingSpace: Boolean
    val hotkeysEnabled: Boolean
}

interface MKeypressSystem : IKeypressSystem
{
    override var holdingSpace: Boolean
    override var hotkeysEnabled: Boolean
}

class KeypressSystem : MKeypressSystem {
    override var holdingSpace: Boolean = false
    override var hotkeysEnabled: Boolean = true
}