package sgui.swing.systems

import rb.glow.gl.GLImage
import rb.glow.img.RawImage
import sgui.core.systems.IImageCreator
import sgui.hybrid.EngineLaunchpoint

object SwImageCreator : IImageCreator {
    override fun createImage(width: Int, height: Int): RawImage = GLImage(width, height, EngineLaunchpoint.gle)
}