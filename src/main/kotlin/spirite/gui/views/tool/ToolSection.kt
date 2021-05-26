package spirite.gui.views.tool

import rb.glow.Colors
import rb.owl.Observer
import rb.owl.bindable.addObserver
import sgui.core.components.IBoxList
import sgui.core.components.IBoxList.IBoxComponent
import sgui.components.IComponent
import sgui.swing.SwIcon
import sgui.swing.components.SwToggleButton
import sgui.hybrid.Hybrid
import sgui.swing.systems.mouseSystem.adaptMouseSystem
import sgui.swing.skin.Skin
import spirite.base.brains.IMasterControl
import spirite.base.brains.toolset.Tool
import spirite.base.imageData.drawer.NillImageDrawer
import spirite.gui.components.advanced.omniContainer.IOmniComponent
import spirite.gui.resources.ToolIcons
import java.awt.Color
import java.awt.GradientPaint
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.RoundRectangle2D
import javax.swing.JToggleButton

private const val BUTTON_WIDTH = 24

class ToolSection (
        private val master: IMasterControl,
        val imp : IBoxList<Tool> = Hybrid.ui.BoxList(BUTTON_WIDTH, BUTTON_WIDTH, null))
    :IOmniComponent
{
    override val component: IComponent get() = imp
    override val icon: SwIcon? get() = null
    override val name: String get() = "Tools"

    val currentTool get() = master.toolsetManager.selectedTool
    val toolset get() = master.toolsetManager.toolset
    val workspace get() = master.workspaceSet.currentWorkspace

    val activeDataBindK = master.centralObservatory.trackingActiveDrawerObserver.addObserver(Observer{
        imp.data.clear()
        imp.data.add(toolset.toolsForDrawer(workspace?.run { activeDrawer } ?: NillImageDrawer))
        imp.data.selected = currentTool
    }, true)

    init {
        imp.enabled = false
        imp.renderer = { tool ->
            object : IBoxComponent {
                override val component = ToolButton(tool).apply {
                    onMouseClick += {evt -> master.toolsetManager.selectedTool = tool}
                }

                override fun setSelected(selected: Boolean) {
                    component.checked = selected
                }
            }
        }
    }
    private val _selectedToolK = master.toolsetManager.selectedToolBinding.addObserver { new, _ ->imp.data.selected = new }

    override fun close() {
        _selectedToolK.void()
    }
}

class ToolButton( val tool: Tool) : SwToggleButton(false, SwToolButtonImp(tool))
{
    init {
        plainStyle = true
        background = Colors.TRANSPARENT
        opaque = false
    }

    private class SwToolButtonImp(val tool: Tool) : JToggleButton() {
        var hover: Boolean = false

        override fun paintComponent(g: Graphics) {
            // I'm not sure if this is the best way to go about calling super.paintComponent
            //	without it actually drawing anything.
            val r = g.clipBounds
            g.setClip(0, 0, 0, 0)
            super.paintComponent(g)
            g.setClip(r.x, r.y, r.width, r.height)

            val w = this.width
            val h = this.height
            val ew = w - BUTTON_WIDTH


            if (isSelected) {
                g.color = Skin.Toolbutton.SelectedBackground.jcolor
                g.fillRect(0, 0, w, h)
            }

            val g2 = g as Graphics2D
            g2.translate(ew / 2, 0)
            ToolIcons.drawToolIcon(g2, 0, 0, tool)
            g2.translate(-ew / 2, 0)


            if (hover) {
                val shape = RoundRectangle2D.Float(0f, 0f, this.width - 1f, this.height - 1f, 5f, 5f)

                val grad = GradientPaint(w.toFloat(), 0f, Color(0, 0, 0, 0), w.toFloat(), h.toFloat(), Color(255, 255, 255, 128))
                g2.paint = grad
                g2.fill(shape)

                g2.color = Color.black
                g2.draw(shape)
            }
        }

        init {
            isEnabled = false
            addMouseListener(object : MouseAdapter() {
                override fun mouseEntered(e: MouseEvent?) {hover = true; repaint()}
                override fun mouseExited(e: MouseEvent?) {hover = false; repaint()}
            })
            adaptMouseSystem()
        }
    }
}