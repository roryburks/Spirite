package spirite.gui.menus

import spirite.base.brains.commands.SpriteCommands
import spirite.base.imageData.layers.sprite.SpriteLayer
import spirite.base.imageData.layers.sprite.SpriteLayer.SpritePart

object SpriteLayerContextMenus {
    fun schemeForSprite(part: SpritePart)  : List<MenuItem>{
        val menuItems = mutableListOf<MenuItem>()

        menuItems.add(MenuItem("Split into new Sprite", SpriteCommands.SplitParts))

        return menuItems
    }
}