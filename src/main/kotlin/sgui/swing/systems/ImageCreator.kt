package sgui.swing.systems

import rb.glow.gl.GLImage
import rb.glow.gle.IGLEngine
import rb.glow.img.RawImage
import sgui.core.systems.IImageCreator

class SwImageCreator(private val _gle : IGLEngine) : IImageCreator {
    override fun createImage(width: Int, height: Int): RawImage = GLImage(width, height, _gle)
}