package spirite.gui.menus.dialogs

import cwShared.dialogSystem.IDialogPanel
import sgui.core.components.crossContainer.ICrossPanel
import sgui.hybrid.Hybrid
import spirite.base.imageData.animation.ffa.FfaCascadingSublayerContract


class FfaCascadingLayerDetailsPanel(val defaultInfo: FfaCascadingSublayerContract)
    : ICrossPanel by Hybrid.ui.CrossPanel(), IDialogPanel<FfaCascadingSublayerContract>
{

    private val _lexicalKeyField = Hybrid.ui.TextField().also { it.text = defaultInfo.lexicalKey.toString() }
    private val _primaryLenField = Hybrid.ui.IntField().also { it.value = defaultInfo.primaryLen }
    private val _lexiconField = Hybrid.ui.TextField().also { it.text = defaultInfo.lexicon ?: "" }

    init {
        setLayout {
            rows.add(Hybrid.ui.Label("Cascading Layer Properties for ${defaultInfo.group.name}"))
            rows += {
                addGap(20)
                add(Hybrid.ui.Label("Lexical Key: "), width = 120)
                add(_lexicalKeyField, width = 24)
            }
            rows += {
                addGap(20)
                add(Hybrid.ui.Label("Primary Length: "), width = 120)
                add(_primaryLenField)
            }
            rows += {
                addGap(10)
                add(Hybrid.ui.Label("Lexicon: "), width = 120)
                add(_lexiconField, width = 240)
            }
        }
    }

    override val result get() = FfaCascadingSublayerContract(
            defaultInfo.group,
            _lexicalKeyField.text.firstOrNull() ?: defaultInfo.lexicalKey,
            _primaryLenField.value,
            when(val lex = _lexiconField.text) {
                "" -> null
                else -> lex
            })
}