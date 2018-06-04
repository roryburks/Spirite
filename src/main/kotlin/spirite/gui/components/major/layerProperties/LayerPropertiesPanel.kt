package spirite.gui.components.major.layerProperties

import spirite.base.brains.IMasterControl
import spirite.base.imageData.groupTree.GroupTree.LayerNode
import spirite.base.imageData.groupTree.GroupTree.Node
import spirite.base.imageData.layers.SimpleLayer
import spirite.base.imageData.layers.sprite.SpriteLayer
import spirite.gui.components.advanced.omniContainer.IOmniComponent
import spirite.gui.components.basic.IComponent
import spirite.gui.components.basic.ICrossPanel
import spirite.gui.resources.IIcon
import spirite.hybrid.Hybrid
import javax.swing.JLabel

class LayerPropertiesPanel( val master: IMasterControl) : IOmniComponent
{
    override val component: IComponent get() = imp
    override val icon: IIcon? get() = null

    val imp = Hybrid.ui.CrossPanel()


    val spriteProperties by lazy { SpriteLayerPanel(master) }

    val listener = { new : Node?, old : Node? ->
        spriteProperties.linkedSprite = null

        imp.setLayout {
            rows.add(when(new){
                null-> Hybrid.ui.Label("Null")
                is LayerNode -> when( new.layer) {
                    is SimpleLayer -> Hybrid.ui.Label("Simple Layer")
                    is SpriteLayer -> spriteProperties.also { it.linkedSprite = new.layer }
                    else -> Hybrid.ui.Label("Unknown")
                }
                else -> Hybrid.ui.Label("Unknown Node Type")
            })
        }
    }.also { master.centralObservatory.selectedNode.addWeakListener(it) }

}