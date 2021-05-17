package rb.animo.io

import rb.animo.animation.IAnimation
import rb.animo.animationSpace.IAnimationSpace


interface IAafScope {
    val animations: Map<String,IAnimation>
    val animationSpaces: List<IAnimationSpace>
}

object NilAafScope : IAafScope {
    override val animations: Map<String, IAnimation> get() = emptyMap()
    override val animationSpaces: List<IAnimationSpace> get() = emptyList()
}

class AafScope(
    override val animations: Map<String,IAnimation>,
    override val animationSpaces: List<IAnimationSpace>)
    : IAafScope

object AafFileNameChooser {
    private val regex by lazy { Regex("""\.([^.\\\/]+)${'$'}""")}
    fun getFilenames(filename: String) : Pair<String,String>
    {
        val extension = regex.find(filename)?.groupValues?.getOrNull(1)

        return when(extension) {
            "png" -> Pair(filename, filename.substring(0,filename.length - 3) + "aaf")
            "aaf" ->  Pair(filename.substring(0,filename.length - 3) + "png", filename)
            null -> Pair("$filename.png", "$filename.aaf")
            else -> Pair(filename.substring(0,filename.length - extension.length) + "png", filename)
        }
    }
}
