package spirite.gui.components.major.tool

import spirite.base.brains.IMasterControl
import spirite.base.brains.palette.IPaletteManager.*
import spirite.base.brains.palette.Palette
import spirite.gui.components.advanced.omniContainer.IOmniComponent
import spirite.gui.components.basic.IColorSquare
import spirite.gui.components.basic.IComponent
import spirite.gui.components.basic.IComponent.BasicBorder.BEVELED_LOWERED
import spirite.gui.components.basic.ICrossPanel
import spirite.gui.components.basic.events.MouseEvent.MouseButton.RIGHT
import spirite.gui.resources.IIcon
import spirite.gui.resources.SwIcons
import spirite.hybrid.Hybrid
import spirite.pc.gui.basic.SwComponent
import spirite.pc.gui.jcolor
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import javax.swing.JPanel


class PaletteSection(
        private val master: IMasterControl,
        val imp : ICrossPanel = Hybrid.ui.CrossPanel())
    : IOmniComponent
{
    override val component: IComponent get() = imp
    override val icon: IIcon? get() = null

    private val __ASDF134 = object : PaletteObserver {
        override fun paletteChanged(evt: PaletteChangeEvent) {
            paletteView.redraw()
        }
        override fun paletteSetChanged(evt: PaletteSetChangeEvent) {
            paletteView.redraw()
        }
    }.also {paletteManager.paletteObservable.addObserver( it) }

    private val primaryColorSquare : IColorSquare = Hybrid.ui.ColorSquare()
    private val secondaryColorSquare : IColorSquare = Hybrid.ui.ColorSquare()
    private val paletteView = PaletteView(master, master.paletteManager.currentPalette)

    private val btnNewPalette = Hybrid.ui.Button().also { it.setIcon(SwIcons.SmallIcons.Palette_NewColor) }
    private val btnSavePalette = Hybrid.ui.Button().also { it.setIcon(SwIcons.SmallIcons.Palette_Save) }
    private val btnLoadPalette = Hybrid.ui.Button().also { it.setIcon(SwIcons.SmallIcons.Palette_Load) }

    private val paletteManager get() = master.paletteManager

    init {
        master.paletteManager.activeBelt.getColorBind(0).bind(primaryColorSquare.colorBind)
        master.paletteManager.activeBelt.getColorBind(1).bind(secondaryColorSquare.colorBind)
        primaryColorSquare.setBasicBorder(BEVELED_LOWERED)
        primaryColorSquare.enabled = false
        secondaryColorSquare.setBasicBorder(BEVELED_LOWERED)
        secondaryColorSquare.enabled = false

        primaryColorSquare.colorBind.addRootListener { _, _ -> paletteView.redraw() }
        secondaryColorSquare.colorBind.addRootListener { _, _ -> paletteView.redraw() }

        imp.setLayout {
            rows += {
                this += {
                    addGap(10)
                    addFlatGroup( {
                        addGap(10)
                        add(primaryColorSquare, 24,24)
                    })
                    addGap(10)
                    addFlatGroup( {
                        addGap(20)
                        add(secondaryColorSquare, 24,24)
                    })
                    width = 36
                }
                add(paletteView)
                flex = 100f
            }
            rows += {
                add(btnNewPalette)
                add(btnSavePalette)
                add(btnLoadPalette)
            }
        }
    }

    // region Palette Moving
    private var pressingIndex : Int? = null
    private var pressingStart: Long = 0
    private var lastPressIndex : Int = 0
    private var lastPressStart: Long = 0

    init {
        primaryColorSquare.onMousePress = {
            pressingIndex = -1
            pressingStart = Hybrid.timing.currentMilli
        }
        secondaryColorSquare.onMousePress = {
            pressingIndex = -2
            pressingStart = Hybrid.timing.currentMilli
        }

        paletteView.onMouseRelease = {evt ->
            val pressing = pressingIndex

            if( pressing != null) {
                if(Hybrid.timing.currentMilli - pressingStart > master.settingsManager.paletteDragMinTime) {
                    val point = evt.point.convert(paletteView)
                    if( point.x / 12 <= paletteView.w && point.x >= 0 && point.y >= 0 && point.y < imp.height) {
                        val index = (point.x / 12) + (point.y / 12 * paletteView.w)
                        val color = when {
                            pressing < 0 -> paletteManager.activeBelt.getColor(-pressing - 1)
                            else -> paletteView.palette.colors[pressing]
                        }
                        color?.also { paletteView.palette.setPaletteColor(index, it) }
                    }
                    else if( pressing >= 0) {
                        paletteView.palette.setPaletteColor(pressing, null)
                    }
                }

                lastPressIndex = pressing
                lastPressStart = pressingStart
                pressingIndex = null
            }
        }
        primaryColorSquare.onMouseRelease = paletteView.onMouseRelease
        secondaryColorSquare.onMouseRelease = paletteView.onMouseRelease

        paletteView.onMousePress = { evt ->
            if( evt.point.x / 12 <= paletteView.w && evt.point.x >= 0 && evt.point.y >= 0) {

                val index = (evt.point.x / 12) + (evt.point.y / 12 * paletteView.w)
                val color = paletteView.palette.colors[index]
                val acId = if( evt.button == RIGHT) 1 else 0

                if( color != null) master.paletteManager.activeBelt.setColor( acId, color)

                val time = Hybrid.timing.currentMilli

                pressingIndex = index
                pressingStart = time

                if( pressingIndex == lastPressIndex && time - lastPressStart < master.settingsManager.paletteDoubleclickTime)
                {
                    master.dialog.pickColor( color ?: master.paletteManager.activeBelt.getColor(acId))
                            ?.also {
                                paletteView.palette.setPaletteColor(index, it)
                                master.paletteManager.activeBelt.setColor(acId, it)
                            }
                }
            }
        }
    }

    // endregion
}

private class PaletteView
private constructor(
        val master: IMasterControl,
        palette: Palette,
        val imp: PaletteViewImp )
    :IComponent by SwComponent(imp)
{
    init {imp.context = this}
    constructor(master: IMasterControl, palette: Palette) : this(master, palette, PaletteViewImp())

    var palette : Palette = palette
        set(value) {
            field = value
            imp.repaint()
        }

    val w get() = Math.max(width/12,1)

    private class PaletteViewImp : JPanel() {
        var context: PaletteView? = null

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)

            val g2 = g as Graphics2D

            val context = context ?: return

            val w = Math.max(width / 12, 1)
            val activeColor1 = context.master.paletteManager.activeBelt.getColor(0).argb32
            val activeColor2 = context.master.paletteManager.activeBelt.getColor(1).argb32

            context.palette.colors.forEach { key, color ->
                g2.color = color.jcolor
                g2.fillRect((key % w) * 12, (key / w) * 12, 12, 12 )
                if( color.argb32 == activeColor1) {
                    g2.stroke = BasicStroke(2f)
                    g2.color = Color.BLACK
                    g2.drawRect((key % w) * 12, (key / w) * 12, 12, 12 )
                }
                if( color.argb32 == activeColor2) {
                    g2.stroke = BasicStroke(2f)
                    g2.color = Color.WHITE
                    g2.drawRect((key % w) * 12, (key / w) * 12, 12, 12 )
                }
            }
        }
    }
}