package rbJvm.animo

import rb.animo.AnimoDependencySet
import rb.animo.io.ILoader
import rb.glow.gle.IGLEngine
import rb.glow.img.IImage
import rbJvm.glow.awt.ImageBI
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

class JvmImageLoader( private val _gle: IGLEngine) : ILoader<IImage> {
    val loader = JvmImageLoader::class.java.classLoader

    override fun load(string: String, onLoad: (IImage) -> Unit, onFail: (Exception?) -> Unit) {
        try {
            lateinit var img : BufferedImage
            loader.getResource(string).openStream().use {
                img = ImageIO.read(it)
            }
            val glImg = _gle.converter.convertToGL(ImageBI(img), _gle)
            onLoad(glImg)
        }
        catch (e : Exception) {
            onFail(e)
        }
    }

}


object JvmImageLoaderProvider {
    val Loader by lazy { JvmImageLoader(AnimoDependencySet.Gle.value) }
}

