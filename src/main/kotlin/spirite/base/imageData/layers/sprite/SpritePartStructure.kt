package spirite.base.imageData.layers.sprite

import spirite.base.imageData.layers.sprite.SpriteLayer.SpritePart

data class SpritePartStructure(
        val depth: Int,
        val partName: String,
        val visible: Boolean = true,
        val alpha: Float = 1f,
        val transX: Float = 0f,
        val transY: Float = 0f,
        val scaleX: Float = 1f,
        val scaleY: Float = 1f,
        val rot: Float = 0f,
        val centerX: Int? = null,
        val centerY: Int? = null)
{
    constructor(part : SpritePart) :
            this(part.depth, part.partName, part.visible, part.alpha, part.transX, part.transY, part.scaleX, part.scaleY, part.rot, part.centerX, part.centerY)
}