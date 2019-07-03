package spirite.gui.views.animation.structureView.ffa

import rb.owl.bindable.addObserver
import sgui.UIPoint
import sgui.components.IComponent
import sgui.components.IComponent.BasicBorder.BEVELED_LOWERED
import sgui.components.crossContainer.ICrossPanel
import sgui.components.events.MouseEvent
import sgui.components.events.MouseEvent.MouseButton.RIGHT
import sgui.components.events.MouseEvent.MouseEventType.RELEASED
import sguiSwing.components.SwComponent
import sguiSwing.skin.Skin.FFAAnimation.Arrow
import sguiSwing.skin.Skin.Global.Bg
import sguiSwing.skin.Skin.Global.Fg
import spirite.base.brains.IMasterControl
import spirite.base.imageData.animation.ffa.*
import spirite.base.imageData.animation.ffa.FFALayer.FFAFrame
import spirite.base.imageData.animation.ffa.FfaFrameStructure.Marker.*
import spirite.gui.menus.dialogs.IDialog
import spirite.gui.menus.IContextMenus
import spirite.gui.menus.MenuItem
import spirite.gui.views.animation.structureView.AnimFFAStructPanel
import spirite.hybrid.Hybrid
import spirite.hybrid.customGui.ArrowPanel
import spirite.hybrid.customGui.DashedOutPanel
import rbJvm.glow.awt.ImageBI
import java.awt.image.BufferedImage
import java.io.InvalidClassException

fun IContextMenus.launchContextMenuFor(point: UIPoint, layer: IFfaLayer, dialog: IDialog) {
    val schema = mutableListOf<MenuItem>()


    schema.add(MenuItem("&Delete Layer", customAction = { layer.anim.removeLayer(layer) }))
    when( layer.asynchronous) {
        true -> schema.add(MenuItem("Make Layer &Synchronous", customAction = { layer.asynchronous = false }))
        false -> schema.add(MenuItem("Make Layer A&synchronous", customAction = { layer.asynchronous = true }))
    }
    schema.add(MenuItem("&Rename Layer", customAction = {
        layer.name = dialog.promptForString("Rename Layer", layer.name) ?: layer.name
    }))

    this.LaunchContextMenu(point, schema)
}

fun IContextMenus.launchContextMenuFor(point: UIPoint, frame: IFfaFrame) {
    val schema = mutableListOf<MenuItem>()
    val layer = frame.layer

    when(layer) {
        is FfaLayerGroupLinked -> {
            schema.add(MenuItem("Add &Gap After", customAction = { layer.addGapFrameAfter(frame, 1) }))
            schema.add(MenuItem("Add Gap &Before", customAction = { layer.addGapFrameAfter((frame as FFAFrame).previous, 1) }))
        }
    }
    this.LaunchContextMenu(point, schema)
}

class FFAFlatLayerBuilder(private val _master: IMasterControl) : IFfaStructViewBuilder
{
    override fun buildNameComponent(layer: IFfaLayer) = when(layer) {
        is FfaLayerLexical -> LexicalNameView(layer, _master.contextMenus, _master.dialog)
        is FFALayer -> NameView(layer, _master.contextMenus, _master.dialog)
        else -> throw InvalidClassException("Layer is not FFALayer")
    }

    override fun buildFrameComponent(layer: IFfaLayer, frame: IFfaFrame): IFFAStructView {
        frame as? FFAFrame ?: throw InvalidClassException("Frame is not FFAFrame")
        layer as? FFALayer ?: throw InvalidClassException("Layer is not FFALayer")

        return when(frame.marker) {
            FRAME -> ImageFrameView(frame, _master.contextMenus)
            GAP -> GapView(frame, _master.contextMenus)
            START_LOCAL_LOOP -> StartLoopView(frame, layer)
            END_LOCAL_LOOP -> EndLoopView(frame, layer)
        }
    }

    // region Menu Components
    private class NameView(
            val layer: FFALayer,
            val contextMenu: IContextMenus,
            val dialog: IDialog,
            private val imp: ICrossPanel = Hybrid.ui.CrossPanel())
        :IFFAStructView
    {
        override val component get() = imp
        override val height: Int get() = 32
        override val dragBrain = AnimDragBrain { evt: MouseEvent, context : AnimFFAStructPanel ->
            if( evt.type == RELEASED && evt.button == RIGHT)
                contextMenu.launchContextMenuFor(evt.point, layer, dialog)
            else if( evt.type == RELEASED)
                label.requestFocus()

            null
        }

        val label = Hybrid.ui.EditableLabel(layer.name)

        init {
            imp.setBasicBorder(BEVELED_LOWERED)

            imp.setLayout { rows.add(label) }
            imp.onMouseRelease += { label.requestFocus() }

            label.textBind.addObserver { new, _ -> layer.name = new }
        }
    }

    private class LexicalNameView(
            val layer: FfaLayerLexical,
            val contextMenu: IContextMenus,
            val dialog: IDialog,
            private val imp: ICrossPanel = Hybrid.ui.CrossPanel())
        : IFFAStructView
    {
        override val component: IComponent get() = imp
        override val height: Int get() = 32
        override val dragBrain: IAnimDragBrain? = AnimDragBrain { evt: MouseEvent, context : AnimFFAStructPanel ->
            if( evt.type == RELEASED && evt.button == RIGHT)
                contextMenu.launchContextMenuFor(evt.point, layer, dialog)
            else if( evt.type == RELEASED)
                label.requestFocus()

             null
        }

        val label = Hybrid.ui.EditableLabel(layer.name)
        val lexiconButton = Hybrid.ui.Button("Lexicon").also { it.action = {redoLexicon()} }

        init {
            imp.setBasicBorder(BEVELED_LOWERED)

            imp.setLayout {
                rows.add(label)
                rows.add(lexiconButton)
            }

            label.textBind.addObserver { new, _ -> layer.name = new }
        }

        fun redoLexicon() {
            layer.lexicon = dialog.promptForString("Enter new Lexicon:",layer.lexicon) ?: return
        }
    }
    // endregion

    // region Frame Components
    private class GapView(
            val frame: FFAFrame,
            val contextMenu: IContextMenus)
        :IFFAStructView
    {
        override val component = Hybrid.ui.CrossPanel {
            rows.add(SwComponent(DashedOutPanel(Bg.jcolor, Fg.jcolor)), width = 32)
        }
        override val height: Int get() = 32
        override val dragBrain: IAnimDragBrain? = object : ResizeableBrain(frame) {
            override fun interpretMouseEvent(evt: MouseEvent, context: AnimFFAStructPanel): IAnimDragBehavior? {
                val behavior = super.interpretMouseEvent(evt, context)
                if( behavior != null) return behavior
                if( evt.type == RELEASED && evt.button == RIGHT)
                    contextMenu.launchContextMenuFor(evt.point, frame)

                return null
            }
        }
    }

    private inner class ImageFrameView(
            val frame: FFAFrame,
            val contextMenu: IContextMenus,
            val tickLen : Int = 32)
        : IFFAStructView
    {
        val imgBox = Hybrid.ui.ImageBox(ImageBI(BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR)))
        override val component = Hybrid.ui.CrossPanel {
            cols.add(imgBox, width = tickLen)
            if( frame.length > 1) {
                cols.add(SwComponent(ArrowPanel(null, Arrow.jcolor, sgui.Direction.RIGHT)), width = tickLen * (frame.length - 1))
            }
        }
        override val height: Int get() = tickLen
        override val dragBrain: IAnimDragBrain? = object : ResizeableBrain(frame) {
            override fun interpretMouseEvent(evt: MouseEvent, context: AnimFFAStructPanel): IAnimDragBehavior? {
                val behavior = super.interpretMouseEvent(evt, context)
                if( behavior != null) return behavior
                if( evt.type == RELEASED && evt.button == RIGHT)
                    contextMenu.launchContextMenuFor(evt.point, frame)

                return null
            }
        }

        val k = _master.nativeThumbnailStore.contractThumbnail(frame.node!!, frame.layer.anim.workspace)
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
