package spirite.gui.views.animation.structureView.ffa

import sgui.generic.components.IComponent
import sgui.generic.components.IComponent.BasicBorder.BEVELED_LOWERED
import sgui.generic.components.crossContainer.ICrossPanel
import sgui.generic.components.events.MouseEvent.MouseButton.RIGHT
import sgui.generic.components.events.MouseEvent.MouseEventType.RELEASED
import spirite.base.brains.IMasterControl
import spirite.base.imageData.animation.ffa.FfaCascadingSublayerContract
import spirite.base.imageData.animation.ffa.FfaLayerCascading
import spirite.base.imageData.animation.ffa.FfaLayerCascading.CascadingFrame
import spirite.base.imageData.animation.ffa.IFfaFrame
import spirite.base.imageData.animation.ffa.IFfaLayer
import spirite.gui.resources.SpiriteIcons
import spirite.hybrid.Hybrid
import java.io.InvalidClassException

private object SettingStore {
    fun setExpanded( o: Any, expanded: Boolean) {_mem[o] = expanded}
    fun getExpanded(o: Any) = _mem[o] ?: false

    private val _mem = mutableMapOf<Any,Boolean>()
}

class FfaCascadingLayerBuilder(
        private val _master: IMasterControl,
        private val _rebuildTrigger: ()->Unit)
    : IFfaStructViewBuilder
{
    private val MainHeight = 32
    private val SubHeight = 24
    private val ElipsesHeight = 8
    private val ButtonDims = 12

    override fun buildNameComponent(layer: IFfaLayer): IFFAStructView = when(layer) {
        is FfaLayerCascading -> NameView(layer, SettingStore.getExpanded(layer))
        else -> throw InvalidClassException("Trying to buildInto FfaCascadingLayer view on other kind of layer")
    }

    override fun buildFrameComponent(layer: IFfaLayer, frame: IFfaFrame) : IFFAStructView = when {
        layer !is FfaLayerCascading || frame !is CascadingFrame -> throw InvalidClassException("Bad Frame type for FfaCascadingBuilder")
        else -> FrameView(frame, layer, SettingStore.getExpanded(layer))
    }

    private inner class NameView(
            val layer: FfaLayerCascading,
            expanded: Boolean)
        :IFFAStructView
    {
        val primaryView = PrimaryNameView(layer, expanded)
        val imp : IComponent

        override val component: IComponent get() = imp
        override val height: Int
        override val dragBrain = AnimDragBrain { evt, context ->
            if( evt.type == RELEASED && evt.button == RIGHT)
                _master.contextMenus.launchContextMenuFor(evt.point, layer, _master.dialog)
            else if( evt.type == RELEASED)
                primaryView.label.requestFocus()

            null
        }

        init {
            if( expanded) {
                val subLayers = layer.getAllSublayers()
                imp = Hybrid.ui.CrossPanel {
                    rows.add(primaryView, height = MainHeight)
                    subLayers.forEach {
                        rows.add(SubLayerNameView(layer, it), height = SubHeight)
                    }
                }
                height = subLayers.count() * SubHeight + MainHeight
            }
            else {
                imp = primaryView
                height = MainHeight
            }
        }
    }


    private inner class PrimaryNameView(
            val layer: FfaLayerCascading,
            val expanded: Boolean,
            val imp : ICrossPanel = Hybrid.ui.CrossPanel())
        : IComponent by imp
    {
        val label = Hybrid.ui.EditableLabel(layer.name)
        val lexiconButton = Hybrid.ui.Button("Lexicon")
        val jsonButton = Hybrid.ui.Button("JSON")
        val expandButton = Hybrid.ui.Button().also {
            it.setIcon(if( expanded)SpiriteIcons.SmallIcons.Unexpanded else SpiriteIcons.SmallIcons.Expanded)
        }

        init {
            imp.setBasicBorder(BEVELED_LOWERED)
            lexiconButton.action = {layer.lexicon = _master.dialog.promptForString("Enter new Lexicon:", layer.lexicon ?: "") ?: layer.lexicon}
            jsonButton.action = {_master.dialog.invokeNewFfaJsonImport(layer)}
            expandButton.action = {
                SettingStore.setExpanded(layer, !expanded)
                _rebuildTrigger()
            }

            imp.setLayout {
                rows += {
                    add(expandButton, ButtonDims, ButtonDims)
                    add(label)
                }
                rows += {
                    add(lexiconButton)
                    add(jsonButton)
                }
            }
        }
    }

    private inner class SubLayerNameView(
            val layer: FfaLayerCascading,
            val info: FfaCascadingSublayerContract,
            val imp : ICrossPanel = Hybrid.ui.CrossPanel())
        : IComponent by imp
    {
        val settingsButton = Hybrid.ui.Button().also { it.setIcon(SpiriteIcons.SmallIcons.Unexpanded) }
        val label = Hybrid.ui.Label("SubLayer ${info.lexicalKey}")

        init {
            settingsButton.action = {
                val newInfo = _master.dialog.invokeNewFfaCascadingLayerDetails(info)
                if( newInfo != null && newInfo != info) {
                    layer.setSublayerInfo(newInfo)
                }
            }

            imp.setLayout {
                cols += {
                    addGap((SubHeight - ButtonDims) / 2)
                    add(settingsButton, ButtonDims, ButtonDims)
                    addGap((SubHeight - ButtonDims) / 2)
                }
                cols.add(label)
                cols.height = SubHeight
            }
        }
    }

    private inner class FrameView(
            val frame: CascadingFrame,
            val layer: FfaLayerCascading,
            expanded: Boolean)
        :IFFAStructView
    {
        val primaryView = PrimaryFrameView(frame)
        val imp : IComponent

        override val component: IComponent get() = imp
        override val height: Int
        override val dragBrain = AnimDragBrain { evt, context ->
            null
        }

        init {
            if( expanded)
            {
                val subLayers = layer.getAllSublayers()
                imp = Hybrid.ui.CrossPanel {
                    rows.add(primaryView, height = MainHeight)
                    subLayers.forEach {
                        rows.add(Hybrid.ui.Label("blah"), height = SubHeight)
                    }
                }
                height = MainHeight
            }
            else {
                imp = primaryView
                height = MainHeight
            }
        }
    }

    private inner class PrimaryFrameView(
            val frame: CascadingFrame,
            val imp : ICrossPanel = Hybrid.ui.CrossPanel())
        : IComponent by imp
    {
    }

    private inner class SubFrameView(
            val frame: CascadingFrame,
            val imp : ICrossPanel = Hybrid.ui.CrossPanel())
        : IComponent by imp
    {
    }
}