package spirite.gui.menus.dialogs

import cwShared.dialogSystem.IDialogPanel
import sgui.core.components.crossContainer.ICrossPanel
import sguiSwing.hybrid.Hybrid
import spirite.base.brains.IMasterControl

data class WorkspaceSizeReturn(
        val width: Int,
        val height: Int)

class WorkspaceSizePanel(
        private val master: IMasterControl
 ) : ICrossPanel by Hybrid.ui.CrossPanel(), IDialogPanel<WorkspaceSizeReturn>
{
    val settings get() = master.settingsManager
    val MAX_DIM get() = settings.MaxDimension

    private val widthField = Hybrid.ui.IntField(1, MAX_DIM, false)
    private val heightField = Hybrid.ui.IntField(1, MAX_DIM, false)

    init {
        widthField.value = settings.defaultImageWidth
        heightField.value = settings.defaultImageHeight

        setLayout {
            rows += {
                add(Hybrid.ui.Label("New Image: "))
            }
            rows += {
                addGap(20)
                add(Hybrid.ui.Label("Width: "), 120)
                add( widthField, width = 120)
            }
            rows += {
                addGap(20)
                add(Hybrid.ui.Label("Height: "), 120)
                add( heightField, width = 120)
            }
        }
    }

    override val result: WorkspaceSizeReturn get() = WorkspaceSizeReturn(widthField.value, heightField.value)
}