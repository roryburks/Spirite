package rbJvm.animo

import rb.animo.animation.AafAnimation
import rb.animo.io.*
import rb.animo.io.aafReader.AafReaderFactory
import rb.glow.gle.IGLEngine
import rbJvm.glow.awt.ImageBI
import java.awt.image.BufferedImage
import java.io.*
import javax.imageio.ImageIO

class JvmAafLoader(private val _gle: IGLEngine) : ILoader<IAafScope> {
    override fun load(string: String, onLoad: (IAafScope) -> Unit, onFail: (Exception?) -> Unit) {
        val (pngFile, aafFile) = AafFileNameChooser.getFilenames(string)

        try {
            lateinit var img: BufferedImage
            val loader = JvmAafLoader::class.java.classLoader
            loader.getResource(pngFile).openStream().use {
                img = ImageIO.read(it)
            }

            loader.getResource(aafFile).openStream().use { inputStream ->
                val dis = DataInputStream(inputStream)
                val reader = JvmDataInputStreamReader(dis)
                val version = reader.readInt()
                val aaf = AafReaderFactory.getReader(version).read(reader)

                val glimg = _gle.converter.convertToGL(ImageBI(img),_gle)

                val animations = aaf.animations
                    .map { Pair(it.name, AafAnimation(it, glimg)) }
                    .toMap()
                onLoad(AafScope(animations, emptyList()))
            }
        }catch (e: Exception) {
            onFail(e)
        }
    }

}