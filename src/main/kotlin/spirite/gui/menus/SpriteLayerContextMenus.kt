package spirite.gui.menus

import spirite.base.brains.commands.SpriteCommands
import spirite.base.imageData.layers.sprite.SpriteLayer.SpritePart
import spirite.base.imageData.mediums.magLev.MaglevMedium

object SpriteLayerContextMenus {
    fun schemeForSprite(part: SpritePart)  : List<MenuItem>{
        val menuItems = mutableListOf<MenuItem>()

        menuItems.add(MenuItem("Split into new Sprite", SpriteCommands.SplitParts))
        menuItems.add(MenuItem("Select All", SpriteCommands.SelectAll))
        menuItems.add(MenuItem("Move parts to other Sprite", SpriteCommands.MoveParts))
        menuItems.add(MenuItem("Fill in to other Sprites", SpriteCommands.FillInLinked))
        menuItems.add(MenuItem("Copy To Layer", SpriteCommands.CopySpriteParts))
        menuItems.add(MenuItem("Copy To All Linked Layer", SpriteCommands.CopySpritePartsAll))
        //menuItems.add(MenuItem("DEBUG: Copy Across Direct", SpriteCommands.CopyAcrossMirrored))

        if( part.context.parts.any { it.handle.medium is MaglevMedium }) {
            menuItems.add(MenuItem("Flatten Layer", SpriteCommands.FlattenMaglevs))
        }
        if( part.handle.medium is MaglevMedium) {
            menuItems.add(MenuItem("Describe Maglev Layer", SpriteCommands.Debug.DescribeMaglevThings))
        }

        return menuItems
    }
}