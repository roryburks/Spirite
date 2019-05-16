package spirite.gui.components.dialogs

import spirite.base.imageData.animation.ffa.FfaCascadingSublayerContract
import spirite.gui.components.basic.ICrossPanel
import spirite.hybrid.Hybrid


class FfaCascadingLayerDetailsPanel(val defaultInfo: FfaCascadingSublayerContract)
    :ICrossPanel by Hybrid.ui.CrossPanel(), IDialogPanel<FfaCascadingSublayerContract>
{

    private val _lexiconField = Hybrid.ui.TextField().also { it.text = defaultInfo.lexicalKey.toString() }
    private val _primaryLenField = Hybrid.ui.IntField().also { it.value = defaultInfo.primaryLen }

    init {
        setLayout {
            rows.add(Hybrid.ui.Label("Cascading Layer Properties for ${defaultInfo.group.name}"))
            rows += {
                addGap(20)
                add(Hybrid.ui.Label("Lexical Key: "), width = 120)
                add(_lexiconField, width = 24)
            }
            rows += {
                addGap(20)
                add(Hybrid.ui.Label("Primary Length: "), width = 120)
                add(_primaryLenField)
            }
        }
    }

    override val result get() = FfaCascadingSublayerContract(
            defaultInfo.group,
            _lexiconField.text.firstOrNull() ?: defaultInfo.lexicalKey,
            _primaryLenField.value)
}