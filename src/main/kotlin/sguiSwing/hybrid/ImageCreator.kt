package sguiSwing.hybrid

import rb.glow.img.RawImage
import rb.glow.gl.GLImage

interface IImageCreator {
    fun createImage(width: Int, height: Int): RawImage
}

object SwImageCreator : IImageCreator{
    override fun createImage(width: Int, height: Int): RawImage = GLImage(width, height, EngineLaunchpoint.gle)
}