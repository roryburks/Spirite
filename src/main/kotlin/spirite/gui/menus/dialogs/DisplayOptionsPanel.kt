package spirite.gui.menus.dialogs

import cwShared.dialogSystem.IDialogPanel
import sgui.components.crossContainer.ICrossPanel
import sguiSwing.hybrid.Hybrid
import spirite.gui.menus.dialogs.DisplayOptionsPanel.DisplayOptions
import spirite.gui.resources.SpiriteIcons

class DisplayOptionsPanel(
        val defaultOptions : DisplayOptions? = null

)
    : ICrossPanel by Hybrid.ui.CrossPanel(), IDialogPanel<DisplayOptions>
{
    data class DisplayOptions(
            val alpha: Float,
            val isVisible: Boolean)

    private val btnVisible = Hybrid.ui.ToggleButton(defaultOptions?.isVisible ?: true)
    private val sliderAlpha = Hybrid.ui.GradientSlider(0f, 1f, "Alpha")

    var isVisible by btnVisible.checkBind
    var alpha by sliderAlpha.valueBind

    init {
        btnVisible.setOnIcon( SpiriteIcons.BigIcons.VisibleOn)
        btnVisible.setOffIcon( SpiriteIcons.BigIcons.VisibleOff)

        sliderAlpha.value = defaultOptions?.alpha ?: 1f
        setLayout {
            rows += {
                add(btnVisible, 24,24)
                addGap(2)
                add(sliderAlpha, 100, 24)
            }
        }
    }

    override val result get() = DisplayOptions(alpha, isVisible)
}