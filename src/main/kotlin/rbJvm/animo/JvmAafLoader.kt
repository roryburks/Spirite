package rbJvm.animo

import rb.animo.animation.AafAnimation
import rb.animo.animation.AafStructure
import rb.animo.io.*
import rb.animo.io.aaf.reader.AafReaderFactory
import rb.file.BufferedFileReader
import rb.glow.gle.IGLEngine
import rbJvm.file.JvmInputStreamFileReader
import rbJvm.file.JvmRandomAccessFileBinaryReadStream
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
                val reader = BufferedFileReader(JvmInputStreamFileReader(inputStream))

                val aafReader = AafReaderFactory.readVersionAndGetReader(reader)
                val aaf = aafReader.read(reader)

                val glimg = _gle.converter.convertToGL(ImageBI(img),_gle)

                val asStructure = AafStructure.fromFile(aaf)
                val animations = asStructure.animations
                    .map { Pair(it.name, AafAnimation(it, glimg)) }
                    .toMap()
                onLoad(AafScope(animations, emptyList()))
            }
        }catch (e: Exception) {
            onFail(e)
        }
    }

}