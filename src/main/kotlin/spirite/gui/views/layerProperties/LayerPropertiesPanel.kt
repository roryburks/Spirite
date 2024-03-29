package spirite.gui.views.layerProperties

import rbJvm.owl.addWeakObserver
import sgui.components.IComponent
import sgui.swing.SwIcon
import spirite.base.brains.IMasterControl
import spirite.base.imageData.groupTree.LayerNode
import spirite.base.imageData.layers.SimpleLayer
import spirite.base.imageData.layers.sprite.SpriteLayer
import spirite.gui.components.advanced.omniContainer.IOmniComponent
import spirite.sguiHybrid.Hybrid

class LayerPropertiesPanel( val master: IMasterControl) : IOmniComponent
{
    override val component: IComponent get() = imp
    override val icon: SwIcon? get() = null
    override val name: String get() = "Layer Properties"

    private val imp = Hybrid.ui.CrossPanel()
    init {imp.ref = this}

    private val spriteProperties by lazy { SpriteLayerPanel(master) }

    private val selectedNodeK = master.centralObservatory.selectedNode.addWeakObserver { new, _ ->
        spriteProperties.linkedSprite = null

        imp.setLayout {
            rows.add(when(new){
                null-> Hybrid.ui.Label("Null")
                is LayerNode -> when( new.layer) {
                    is SimpleLayer -> Hybrid.ui.Label("Simple Layer")
                    is SpriteLayer -> spriteProperties.also { it.linkedSprite = new.layer }
                    else -> Hybrid.ui.Label("Unknown")
                }
                else -> Hybrid.ui.Label("Unknown GroupNode Type")
            })
        }
    }
}