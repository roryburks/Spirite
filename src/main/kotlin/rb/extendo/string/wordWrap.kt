package rb.extendo.string

fun String.wordWrap(perLine: Int, splittingChars :Set<Char> = setOf(' ','-')) : List<String> {
    val input = this
    val splitOn = splittingChars.toHashSet()
    var caret = 0
    val result = mutableListOf<String>()
    var currentLine = StringBuilder()

    fun nextWord() : String {
        when(val char = input[caret]) {
            in splitOn -> {
                caret++
                return char.toString()
            }
            else -> {
                val start = caret
                while( caret < input.length && input[++caret] !in splitOn) {}
                return input.substring(start, caret)
            }
        }
    }

    while (caret < input.length) {
        val word = nextWord()
        if( word.length > perLine) {
            word.forEach {
                if( currentLine.length >= perLine) {
                    result.add(currentLine.toString())
                    currentLine = StringBuilder()
                }
                currentLine.append(it)
            }
        }
        else {
            if( currentLine.length + word.length >= perLine) {
                result.add(currentLine.toString())
                currentLine = StringBuilder()
            }
            currentLine.append(word)
        }
    }
    if( currentLine.any()) {
        result.add(currentLine.toString())
    }

    return result
}