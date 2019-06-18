package spirite.gui.components.advanced.omniContainer

import sgui.Orientation

sealed class OmniPart {
    abstract val components: List<IOmniComponent>
    abstract val parts: OmniPart
}

class OmniContainerPart(
        leading: List<IOmniComponent>,
        trailing: List<IOmniComponent>,
        center: IOmniComponent,
        orientation: sgui.Orientation,
        defaultSize: Int,
        minSize: Int
) : OmniPart() {
    private val _leading = leading.toMutableList()
    private val _trailing = trailing.toMutableList()
    private var center = center

    override val components get() = _leading.plus(_trailing).plus(center)
    override val parts: OmniPart
        get() = TODO("not implemented")
}