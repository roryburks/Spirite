package rb.glow

interface ITextPlacer {
    fun placeText( text: String, x: Int, y: Int)
}
interface MTextPlacer : ITextPlacer {
    fun reset()
}