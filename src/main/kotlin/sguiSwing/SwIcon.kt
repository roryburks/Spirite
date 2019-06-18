package sguiSwing

import sgui.IIcon
import javax.swing.ImageIcon

interface SwIcon : IIcon
{
    val icon: ImageIcon
}

object NilIcon : IIcon

enum class PrimaryIcon {
    SmallExpanded,
    SmallExpandedHighlighted,
    SmallUnexpanded,
    SmallUnexpandedHighlighted,

    SmallArrowN,
    SmallArrowS,
    SmallArrowE,
    SmallArrowW
}

object SwPrimaryIconSet {
    private val _iconMap = mutableMapOf<PrimaryIcon, SwIcon>()

    fun getIcon(icon: PrimaryIcon)  = _iconMap[icon] ?: NilIcon
    fun setIcon(key: PrimaryIcon, icon: SwIcon) {
        _iconMap[key] =icon
    }
}