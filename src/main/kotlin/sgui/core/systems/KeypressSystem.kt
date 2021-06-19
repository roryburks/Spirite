package sgui.core.systems

interface IKeypressSystem
{
    val holdingSpace: Boolean
    val hotkeysEnabled: Boolean
    val lastAlphaNumPressed: Char
}

interface MKeypressSystem : IKeypressSystem
{
    override var holdingSpace: Boolean
    override var hotkeysEnabled: Boolean
    override var lastAlphaNumPressed: Char
}

object KeypressSystem : MKeypressSystem {
    override var holdingSpace: Boolean = false
    override var hotkeysEnabled: Boolean = true
    override var lastAlphaNumPressed: Char = '0'
}