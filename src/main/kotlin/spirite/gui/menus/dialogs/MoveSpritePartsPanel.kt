package spirite.gui.menus.dialogs

import cwShared.dialogSystem.IDialogPanel
import rb.owl.bindable.addObserver
import sgui.components.crossContainer.ICrossPanel
import sguiSwing.hybrid.Hybrid
import spirite.base.imageData.groupTree.GroupTree.LayerNode
import spirite.base.imageData.layers.sprite.SpriteLayer
import spirite.base.imageData.layers.sprite.SpriteLayer.SpritePart

class MoveSpritePartsPanel(
        val parts: List<SpritePart>) :ICrossPanel by Hybrid.ui.CrossPanel(), IDialogPanel<SpriteLayer?>
{
    override val result: SpriteLayer? get() = _comboBox.selectedItem?.first

    // UI Components
    private val _comboBox = Hybrid.ui.ComboBox(getAllCandidates(false).toTypedArray())
    private val _searchAllCheckbox = Hybrid.ui.CheckBox().also { it.check = false }

    init /* Layout */ {
        _comboBox.renderer = {value, index, isSelected, hasFocus -> Hybrid.ui.Label(value?.second ?: "MISSING") }

        setLayout {
            rows.add(_comboBox)
            rows += {
                add(Hybrid.ui.Label("SearchAll"))
                add(_searchAllCheckbox)
            }
        }
    }

    init /* Bindings */ {
        _searchAllCheckbox.checkBind.addObserver { new, old ->
            _comboBox.setValues(getAllCandidates(new))
        }
    }

    // Internal Methods
    private fun getAllCandidates(searchAll: Boolean) : List<Pair<SpriteLayer,String>> {
        val spriteLayer = parts.firstOrNull()?.context

        if( searchAll) {
            return  spriteLayer?.workspace?.groupTree?.root
                    ?.getAllNodesSuchThat({true})
                    ?.filterIsInstance<LayerNode>()
                    ?.mapNotNull {
                        val slayer = it.layer as? SpriteLayer ?: return@mapNotNull null
                        if( slayer == spriteLayer) null
                        else Pair(slayer, it.name)
                    }
                    ?: emptyList()
        }
        else {

            val parent = spriteLayer?.workspace?.groupTree?.root
                    ?.getAllNodesSuchThat({ (it as? LayerNode)?.layer == spriteLayer })
                    ?.firstOrNull()
                    ?.parent

            return parent?.children
                    ?.filterIsInstance<LayerNode>()
                    ?.mapNotNull {
                        val slayer = it.layer as? SpriteLayer ?: return@mapNotNull null
                        if( slayer == spriteLayer) null
                        else Pair(slayer, it.name)
                    }
                    ?: emptyList()
        }
    }

}