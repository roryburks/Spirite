package spirite.gui.views.animation.structureView.ffa

import spirite.base.brains.IMasterControl
import spirite.base.imageData.animation.ffa.FFAFrameStructure.Marker.*
import spirite.base.imageData.animation.ffa.FFALayer
import spirite.base.imageData.animation.ffa.FFALayer.FFAFrame
import spirite.base.imageData.animation.ffa.FFALayerLexical
import spirite.base.imageData.animation.ffa.IFFAFrame
import spirite.base.imageData.animation.ffa.IFFALayer
import spirite.base.imageData.undo.NullAction
import spirite.gui.Direction
import spirite.gui.components.basic.IComponent
import spirite.gui.components.basic.IComponent.BasicBorder.BEVELED_LOWERED
import spirite.gui.components.basic.ICrossPanel
import spirite.gui.components.basic.events.MouseEvent
import spirite.gui.components.basic.events.MouseEvent.MouseButton.RIGHT
import spirite.gui.components.basic.events.MouseEvent.MouseEventType.RELEASED
import spirite.gui.components.dialogs.IDialog
import spirite.gui.menus.ContextMenus
import spirite.gui.menus.ContextMenus.MenuItem
import spirite.gui.resources.Skin
import spirite.gui.views.animation.structureView.AnimFFAStructPanel
import spirite.hybrid.Hybrid
import spirite.hybrid.customGui.ArrowPanel
import spirite.pc.graphics.ImageBI
import spirite.pc.gui.basic.SwComponent
import java.awt.image.BufferedImage
import java.io.InvalidClassException


class FFAFlatLayerBuilder(private val _master: IMasterControl) : IFFAStructViewBuilder
{
    override fun buildNameComponent(layer: IFFALayer) = when(layer) {
        is FFALayerLexical -> LexicalNameView(layer, _master.contextMenus,☻ _master.dialog)
        is FFALayer -> NameView(layer, _master.contextMenus)
        else -> throw InvalidClassException("Layer is not FFALayer")
    }

    override fun buildFrameComponent(layer: IFFALayer, frame: IFFAFrame): IFFAStructView {
        frame as? FFAFrame ?: throw InvalidClassException("Frame is not FFAFrame")
        layer as? FFALayer ?: throw InvalidClassException("Layer is not FFALayer")

        return when(frame.marker) {
            FRAME -> ImageFrameView(frame, layer)
            GAP -> GapView(frame, layer)
            START_LOCAL_LOOP -> StartLoopView(frame, layer)
            END_LOCAL_LOOP -> EndLoopView(frame, layer)
        }
    }

    // region Menu Components
    private class NameView(
            val layer: FFALayer,
            val contextMenu: ContextMenus,
            private val imp: ICrossPanel = Hybrid.ui.CrossPanel())
        :IFFAStructView
    {
        override val component get() = imp
        override val height: Int get() = 32
        override val dragBrain = object : IAnimDragBrain {
            override fun interpretMouseEvent(evt: MouseEvent, context : AnimFFAStructPanel): IAnimDragBehavior? {
                if( evt.type == RELEASED)
                    label.requestFocus()
                if( evt.type == RELEASED && evt.button == RIGHT)
                {
                    contextMenu.LaunchContextMenu(evt.point, listOf(
                            MenuItem("&Delete Layer", customAction = {layer.anim.removeLayer(layer)})
                    ), layer)
                }

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
            val contextMenu: ContextMenus,
            val dialog: IDialog,
            private val imp: ICrossPanel = Hybrid.ui.CrossPanel())
        : IFFAStructView
    {
        override val component: IComponent get() = imp
        override val height: Int get() = 32
        override val dragBrain: IAnimDragBrain? get() = object : IAnimDragBrain {
            override fun interpretMouseEvent(evt: MouseEvent, context : AnimFFAStructPanel): IAnimDragBehavior? {
                if( evt.type == RELEASED)
                    label.requestFocus()
                if( evt.type == RELEASED && evt.button == RIGHT)
                {
                    contextMenu.LaunchContextMenu(evt.point, listOf(
                            MenuItem("&Delete Layer", customAction = {layer.anim.removeLayer(layer)})
                    ), layer)
                }
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
    private class GapView(val frame: FFAFrame, val layer: FFALayer) :IFFAStructView
    {
        override val component = Hybrid.ui.CrossPanel {  }
        override val height: Int get() = 0
        override val dragBrain: IAnimDragBrain? get() = object : ResizeableBrain(frame) {}

        // TODO: Add Expose Drag-Drop behavior of Gap, and anim menu
    }

    private inner class ImageFrameView(val frame: FFAFrame, val layer: FFALayer, val tickLen : Int = 32) : IFFAStructView
    {
        val imgBox = Hybrid.ui.ImageBox(ImageBI(BufferedImage(1,1,BufferedImage.TYPE_4BYTE_ABGR)))
        override val component = Hybrid.ui.CrossPanel {
            cols.add(imgBox, width = tickLen)
            if( frame.length > 1) {
                cols.add(SwComponent(ArrowPanel(null, Skin.FFAAnimation.Arrow.jcolor, Direction.RIGHT)), width = tickLen * (frame.length - 1))
            }
        }
        override val height: Int get() = tickLen
        override val dragBrain: IAnimDragBrain = object : ResizeableBrain(frame) {}

        val k = _master.nativeThumbnailStore.contractThumbnail(frame.node!!, layer.anim.workspace)
                {imgBox.setImage(it)}
    }

    private class StartLoopView(val frame: FFAFrame, val layer: FFALayer) : IFFAStructView
    {
        override val component: IComponent get() = TODO("not implemented")
        override val height: Int get() = TODO("not implemented")
        override val dragBrain: IAnimDragBrain? get() = TODO("not implemented")
    }

    private class EndLoopView(val frame: FFAFrame, val layer: FFALayer) : IFFAStructView
    {
        override val component: IComponent get() = Hybrid.ui.CrossPanel {  }
        override val height: Int get() = 0
        override val dragBrain: IAnimDragBrain? get() = null
    }



    // endregion

}
