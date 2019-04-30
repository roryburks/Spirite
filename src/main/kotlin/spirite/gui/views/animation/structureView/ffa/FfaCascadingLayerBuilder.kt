package spirite.gui.views.animation.structureView.ffa

import spirite.base.brains.IMasterControl
import spirite.base.imageData.animation.ffa.FfaLayerCascading
import spirite.base.imageData.animation.ffa.FfaLayerCascading.CascadingFrame
import spirite.base.imageData.animation.ffa.IFFAFrame
import spirite.base.imageData.animation.ffa.IFfaLayer
import spirite.gui.components.basic.IComponent
import spirite.gui.components.basic.IComponent.BasicBorder.BEVELED_LOWERED
import spirite.gui.components.basic.ICrossPanel
import spirite.gui.components.basic.events.MouseEvent.MouseButton.RIGHT
import spirite.gui.components.basic.events.MouseEvent.MouseEventType.RELEASED
import spirite.gui.components.dialogs.IDialog
import spirite.gui.menus.ContextMenus
import spirite.hybrid.Hybrid
import java.io.InvalidClassException

class FfaCascadingLayerBuilder(val _master: IMasterControl) : IFFAStructViewBuilder
{
    override fun buildNameComponent(layer: IFfaLayer): IFFAStructView = when(layer) {
        is FfaLayerCascading -> NameView(layer, _master.contextMenus, _master.dialog)
        else -> throw InvalidClassException("Trying to buildInto FfaCascadingLayer view on other kind of layer")
    }

    override fun buildFrameComponent(layer: IFfaLayer, frame: IFFAFrame) : IFFAStructView = when {
        layer !is FfaLayerCascading || frame !is CascadingFrame -> throw InvalidClassException("Bad Frame type for FfaCascadingBuilder")
        else -> FrameView(frame)
    }

    private class NameView(
            val layer: FfaLayerCascading,
            val contextMenus: ContextMenus,
            val dialog: IDialog,
            val imp : ICrossPanel = Hybrid.ui.CrossPanel())
        :IFFAStructView
    {
        override val component: IComponent get() = imp
        override val height: Int get() = 32
        override val dragBrain = AnimDragBrain { evt, context ->
            if( evt.type == RELEASED && evt.button == RIGHT)
                contextMenus.launchContextMenuFor(evt.point, layer, dialog)
            else if( evt.type == RELEASED)
                label.requestFocus()

            null
        }

        val label = Hybrid.ui.EditableLabel(layer.name)
        val button = Hybrid.ui.Button("Lexicon")

        init {
            imp.setBasicBorder(BEVELED_LOWERED)
            button.action = {layer.lexicon = dialog.promptForString("Enter new Lexicon:", layer.lexicon ?: "") ?: layer.lexicon}

            imp.setLayout {
                rows.add(label)
                rows.add(button)
            }
        }
    }

    private class FrameView(
            val frame: CascadingFrame,
            val imp : ICrossPanel = Hybrid.ui.CrossPanel())
        :IFFAStructView
    {
        override val component: IComponent get() = imp
        override val height: Int get() = 32
        override val dragBrain = AnimDragBrain { evt, context ->
            null
        }
    }
}