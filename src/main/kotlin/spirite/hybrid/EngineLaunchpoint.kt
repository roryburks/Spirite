package spirite.hybrid

import spirite.base.graphics.NillImage
import spirite.base.graphics.RawImage
import spirite.base.graphics.RawImage.InvalidImageDimensionsExeption
import spirite.base.graphics.gl.GLEngine
import spirite.base.graphics.gl.GLImage
import spirite.pc.JOGL.JOGLProvider
import spirite.pc.resources.JClassScriptService

object EngineLaunchpoint {
    val gle = GLEngine(JOGLProvider.getGL(), JClassScriptService())

    fun createImage(width: Int, height: Int): RawImage {
        return when {
            width <= 0 || height <= 0 -> NillImage()
            else -> GLImage(width, height, gle)
        }
    }
}