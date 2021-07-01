package spirite.base.imageData.animation

object DefaultAnimCharMap {
    fun getCharList() : List<Char> {
        return (0..51).map { getCharForIndex(it)!! }
    }

    fun getCharForIndex( index: Int) : Char? {
        return when(index) {
            in 0..25 -> 'A' + index
            in 26..51 -> 'a' + (index - 26)
            else -> null
        }
    }

    fun getIndexFromChar(char: Char?) : Int? {
        return when( char) {
            null -> null
            in 'A'..'Z' -> char - 'A'
            in 'a'..'z' -> (char - 'a') + 26
            else -> null
        }
    }
}