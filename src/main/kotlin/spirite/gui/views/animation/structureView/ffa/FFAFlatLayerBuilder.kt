package spirite.gui.views.animation.structureView.ffa

import com.sun.javaws.exceptions.InvalidArgumentException
import spirite.base.brains.IMasterControl
import spirite.base.imageData.animation.ffa.FFALayer
import spirite.base.imageData.animation.ffa.FFALayer.FFAFrame
import spirite.base.imageData.animation.ffa.FFALayerLexical
import spirite.base.imageData.animation.ffa.IFFAFrame
import spirite.base.imageData.animation.ffa.IFFALayer
import spirite.gui.components.basic.IComponent
import spirite.gui.components.basic.IComponent.BasicBorder.BEVELED_LOWERED
import spirite.gui.components.basic.ICrossPanel
import spirite.gui.components.basic.events.MouseEvent
import spirite.gui.components.basic.events.MouseEvent.MouseEventType.RELEASED
import spirite.gui.components.dialogs.IDialog
import spirite.hybrid.Hybrid
import java.io.InvalidClassException

class FFAFlatLayerBuilder(private val _master: IMasterControl) : IFFAStructViewBuilder
{
    override fun buildMenuComponent(layer: IFFALayer) = when(layer) {
        is FFALayerLexical -> LexicalNameView(layer, _master.dialog)
        is FFALayer -> NameView(layer)
        else -> throw InvalidClassException("Layer is not FFALayer")
    }

    override fun buildFrameComponent(layer: IFFALayer, frame: IFFAFrame): IFFAStructView {
        val ffaFrame = frame as? FFAFrame ?: throw InvalidClassException("Frame is not FFAFrame")

        TODO()
    }

    // region Menu Components
    private class NameView(val layer: FFALayer, private val imp: ICrossPanel = Hybrid.ui.CrossPanel())
        :IFFAStructView
    {
        override val component get() = imp
        override val height: Int get() = 32
        override val dragBrain = object : IAnimDragBrain {
            override fun interpretMouseEvent(evtMouseEvent: MouseEvent): IAnimDragBehavior? {
                if( evtMouseEvent.type == RELEASED)
                    label.requestFocus()
                return null
            }
        }

        val label = Hybrid.ui.EditableLabel("layer")

        init {
            imp.setBasicBorder(BEVELED_LOWERED)

            imp.setLayout { rows.add(label) }
            imp.onMouseRelease += { label.requestFocus() }
        }
    }

    private class LexicalNameView(
            val layer: FFALayerLexical,
            val dialog: IDialog,
            private val imp: ICrossPanel = Hybrid.ui.CrossPanel())
        : IFFAStructView
    {
        override val component: IComponent get() = imp
        override val height: Int get() = 32
        override val dragBrain: IAnimDragBrain? get() = object : IAnimDragBrain {
            override fun interpretMouseEvent(evtMouseEvent: MouseEvent): IAnimDragBehavior? {
                if( evtMouseEvent.type == RELEASED)
                    label.requestFocus()
                return null
            }
        }

        val label = Hybrid.ui.EditableLabel("layer")
        val lexiconButton = Hybrid.ui.Button("Lexicon").also { it.action = {redoLexicon()} }

        init {
            imp.setBasicBorder(BEVELED_LOWERED)

            val label = Hybrid.ui.EditableLabel("layer")
            imp.setLayout {
                rows.add(label)
                rows.add(lexiconButton)
            }
        }

        fun redoLexicon() {
            layer.lexicon = dialog.promptForString("Enter new Lexicon:",layer.lexicon) ?: return
        }
    }
    // endregion

    // region Frame Components

    // endregion

}