package spirite.gui.components.dialogs

import sgui.generic.components.crossContainer.ICrossPanel
import spirite.base.imageData.animation.ffa.FfaCascadingSublayerContract
import spirite.base.imageData.animation.ffa.FfaLayerCascading
import spirite.hybrid.Hybrid


data class FfaCascadingJsonContract(
        val lexicon: String?,
        val sublayerDetails: Map<String,FfaCascadingJsonContractSub>)

data class FfaCascadingJsonContractSub(
        val primartLen: Int,
        val lexicon: String?)

class FfaCascadingJsonPanel(val layer: FfaLayerCascading)
    : ICrossPanel by Hybrid.ui.CrossPanel(), IDialogPanel<List<FfaCascadingSublayerContract>>
{
    private val _textArea = Hybrid.ui.TextArea()

    init {
        setLayout {
            rows.add(Hybrid.ui.Label("Cascading Layer Json Import/Export For ${layer.groupLink.name}"))
            rows += {
                addGap(20)
                add(Hybrid.ui.Label("Json: "), width = 120)
            }
            rows.add(_textArea, width = 240, height = 240)

        }
    }

    override val result: List<FfaCascadingSublayerContract> get() = emptyList()
}