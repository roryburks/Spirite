package spirite.gui.components.major.tool

import javafx.scene.input.KeyCode
import spirite.base.brains.IMasterControl
import spirite.base.brains.palette.IPaletteManager.*
import spirite.base.brains.palette.Palette
import spirite.gui.components.advanced.omniContainer.IOmniComponent
import spirite.gui.components.basic.IColorSquare
import spirite.gui.components.basic.IComponent
import spirite.gui.components.basic.IComponent.BasicBorder.BEVELED_LOWERED
import spirite.gui.components.basic.ICrossPanel
import spirite.gui.components.basic.events.MouseEvent.MouseButton.RIGHT
import spirite.gui.menus.ContextMenus.MenuItem
import spirite.gui.resources.IIcon
import spirite.gui.resources.SwIcons
import spirite.hybrid.Hybrid
import spirite.pc.gui.basic.SwComponent
import spirite.pc.gui.jcolor
import java.awt.*
import java.awt.event.KeyEvent
import javax.swing.JPanel


class PaletteSection(
        private val master: IMasterControl,
        val imp : ICrossPanel = Hybrid.ui.CrossPanel())
    : IOmniComponent
{
    override val component: IComponent get() = imp
    override val icon: IIcon? get() = null
    override val name: String get() = "Palette"

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
    private val paletteView = PaletteView(master)
    private val paletteChooserView = PaletteChooserView(master)


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
                add(paletteChooserView)
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
    }

    // endregion


    private val _paletteBind = master.paletteManager.currentPaletteBind.addWeakListener { _, _ -> paletteView.redraw()}

    override fun close() {
        paletteView.onClose()
        paletteChooserView.onClose()
    }
}

private class PaletteChooserView
constructor(
        val master: IMasterControl,
        val imp: ICrossPanel = Hybrid.ui.CrossPanel())
    :IComponent by imp
{
    private val currentWorkspace get() = master.workspaceSet.currentWorkspace
    private val paletteManager get() = master.paletteManager

    private val btnNewPalette = Hybrid.ui.Button().also { it.setIcon(SwIcons.SmallIcons.Palette_NewColor) }
    private val btnRemovePalette = Hybrid.ui.Button().also { it.setIcon(SwIcons.SmallIcons.Rig_Remove) }
    private val btnSavePalette = Hybrid.ui.Button().also { it.setIcon(SwIcons.SmallIcons.Palette_Save) }
    private val btnLoadPalette = Hybrid.ui.Button().also { it.setIcon(SwIcons.SmallIcons.Palette_Load) }

    private val cbPaletteSelector = Hybrid.ui.ComboBox<Palette?>(arrayOf(null))

    init { // Layout
        imp.setLayout {
            rows.add(cbPaletteSelector, height = 20)
            rows.addGap(2)
            rows += {
                addGap(2)
                add(btnNewPalette)
                addGap(1)
                add(btnRemovePalette)
                addGap(1)
                add(btnSavePalette)
                addGap(1)
                add(btnLoadPalette)
                addGap(2)
                height = 16
            }
            rows.addGap(2)
        }
        rebuild()
    }

    init { // Bindings
        cbPaletteSelector.selectedItemBind.addRootListener { new, old ->
            if( new != null) {
                currentWorkspace?.paletteSet?.currentPalette = when( new) {
                    master.paletteManager.globalPalette -> null
                    else -> new
                }
            }
        }

        cbPaletteSelector.addEventOnKeypress(KeyEvent.VK_F2, 0) {
            val cwPalette = currentWorkspace?.paletteSet?.currentPalette
            if( cwPalette != null) {
                val name = master.dialog.promptForString("Rename Palette:", cwPalette.name)
                if( name != null) {
                    cwPalette.name = name
                }
            }
        }


        btnNewPalette.action = {
            val name = master.dialog.promptForString("Enter name for the new Palette:", "New Palette")
            if( name != null) {
                master.workspaceSet.currentWorkspace?.paletteSet?.addPalette(name, true)
            }
        }
        btnRemovePalette.action = {
            val index = currentWorkspace?.paletteSet?.palettes?.indexOf(master.paletteManager.currentPalette)
            if( index != null && index >= 0) {
                currentWorkspace?.paletteSet?.removePalette(index)
            }
        }
        btnSavePalette.action = {
            val name = master.dialog.promptForString("Enter name to save the palette as:", master.paletteManager.currentPalette.name)
            if( name != null)
                master.paletteManager.savePaletteInPrefs(name, master.paletteManager.currentPalette)
        }
        btnLoadPalette.action = {
            val scheme = master.settingsManager.paletteList.map { MenuItem(it, customAction = {
                val data = master.settingsManager.getRawPalette(it)
                currentWorkspace?.paletteSet?.addPalette(it, true, data)
            })}
            master.contextMenus.LaunchContextMenu(btnLoadPalette.topLeft, scheme)
        }
    }


    private fun rebuild() {
        val workspace = master.workspaceSet.currentWorkspace

        val items : List<Palette?> = when(workspace) {
            null -> listOf(master.paletteManager.globalPalette)
            else -> {
                val list = mutableListOf<Palette?>()

                list.addAll(workspace.paletteSet.palettes)
                list.add(null)
                list.add(master.paletteManager.globalPalette)

                list
            }
        }

        cbPaletteSelector.setValues(items, master.paletteManager.currentPalette)

    }

    // region Observers
    private val _workspaceListener = master.workspaceSet.currentWorkspaceBind.addListener { new, old ->
        rebuild()
    }
    private val _paletteManaager = object : PaletteObserver {
        override fun paletteChanged(evt: PaletteChangeEvent) {}
        override fun paletteSetChanged(evt: PaletteSetChangeEvent) {
            rebuild()
        }
    }.also { master.paletteManager.paletteObservable.addObserver(it)}
    fun onClose() {
        _workspaceListener.unbind()
    }
    //endregion
}

private class PaletteView
private constructor(
        val master: IMasterControl,
        val imp: PaletteViewImp )
    :IComponent by SwComponent(imp)
{
    constructor(master: IMasterControl) : this(master, PaletteViewImp())

    val w get() = Math.max(width/12,1)
    val palette get() = master.paletteManager.currentPalette

    private val _paletteListener = master.paletteManager.currentPaletteBind.addListener { _, _ ->  imp.repaint() }

    fun onClose() {
        _paletteListener.unbind()
    }

    // region Imp
    init {imp.context = this}
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
    // endregion
}