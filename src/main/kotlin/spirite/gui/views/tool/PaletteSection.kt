package spirite.gui.views.tool

import rb.jvm.owl.addWeakObserver
import rb.owl.bindable.addObserver
import rb.owl.observer
import spirite.base.brains.IMasterControl
import spirite.base.brains.palette.IPaletteManager.*
import spirite.base.brains.palette.Palette
import spirite.gui.components.advanced.omniContainer.IOmniComponent
import sgui.generic.components.IColorSquare
import sgui.generic.components.IComponent
import sgui.generic.components.IComponent.BasicBorder.BEVELED_LOWERED
import sgui.generic.components.ICrossPanel
import sgui.generic.components.events.MouseEvent.MouseButton.RIGHT
import spirite.gui.menus.ContextMenus.MenuItem
import sgui.swing.SwIcon
import spirite.gui.resources.SpiriteIcons
import spirite.hybrid.Hybrid
import sgui.swing.components.SJPanel
import sgui.swing.components.SwComponent
import sgui.swing.jcolor
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.event.KeyEvent


class PaletteSection(
        private val master: IMasterControl,
        val imp : ICrossPanel = Hybrid.ui.CrossPanel())
    : IOmniComponent
{
    override val component: IComponent get() = imp
    override val icon: SwIcon? get() = null
    override val name: String get() = "Palette"

    private val primaryColorSquare : IColorSquare = Hybrid.ui.ColorSquare()
    private val secondaryColorSquare : IColorSquare = Hybrid.ui.ColorSquare()
    private val paletteView = PaletteView(master)
    private val paletteChooserView = PaletteChooserView(master)


    private val paletteManager get() = master.paletteManager

    val _cBind0 = primaryColorSquare.colorBind.bindTo(master.paletteManager.activeBelt.getColorBind(0))
    val _cBind1 = secondaryColorSquare.colorBind.bindTo(master.paletteManager.activeBelt.getColorBind(1))
    init {
        primaryColorSquare.setBasicBorder(BEVELED_LOWERED)
        primaryColorSquare.enabled = false
        secondaryColorSquare.setBasicBorder(BEVELED_LOWERED)
        secondaryColorSquare.enabled = false

        primaryColorSquare.colorBind.addObserver { _, _ -> paletteView.redraw() }
        secondaryColorSquare.colorBind.addObserver { _, _ -> paletteView.redraw() }

        imp.setLayout {
            rows += {
                this += {
                    addGap(10)
                    addFlatGroup {
                        addGap(10)
                        add(primaryColorSquare, 24,24)
                    }
                    addGap(10)
                    addFlatGroup {
                        addGap(20)
                        add(secondaryColorSquare, 24,24)
                    }
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
        primaryColorSquare.onMousePress += {
            pressingIndex = -1
            pressingStart = Hybrid.timing.currentMilli
        }
        secondaryColorSquare.onMousePress += {
            pressingIndex = -2
            pressingStart = Hybrid.timing.currentMilli
        }

        primaryColorSquare.onMouseRelease += {evt ->paletteView.onMouseRelease.triggers.forEach { it(evt) }}
        secondaryColorSquare.onMouseRelease += {evt ->paletteView.onMouseRelease.triggers.forEach { it(evt) }}

        paletteView.onMousePress += { evt ->
            paletteView.requestFocus()
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

        paletteView.onMouseRelease += {evt ->
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


    private val _curPlttK = master.paletteManager.currentPaletteBind.addWeakObserver { _, _ -> paletteView.redraw()}


    private val _paletObsK =paletteManager.paletteObservable.addObserver(
            object : PaletteObserver {
                override fun paletteChanged(evt: PaletteChangeEvent) {
                    paletteView.redraw()
                }
                override fun paletteSetChanged(evt: PaletteSetChangeEvent) {
                    paletteView.redraw()
                }
            }.observer()
    )

    override fun close() {
        paletteView.onClose()
        paletteChooserView.onClose()
        _curPlttK.void()
        _cBind0.void()
        _cBind1.void()
        _paletObsK.void()
    }
}

private class PaletteChooserView
constructor(
        val master: IMasterControl,
        val imp: ICrossPanel = Hybrid.ui.CrossPanel())
    : IComponent by imp
{
    private val currentWorkspace get() = master.workspaceSet.currentWorkspace
    private val paletteManager get() = master.paletteManager

    private val btnNewPalette = Hybrid.ui.Button().also { it.setIcon(SpiriteIcons.SmallIcons.Palette_NewColor) }
    private val btnRemovePalette = Hybrid.ui.Button().also { it.setIcon(SpiriteIcons.SmallIcons.Rig_Remove) }
    private val btnSavePalette = Hybrid.ui.Button().also { it.setIcon(SpiriteIcons.SmallIcons.Palette_Save) }
    private val btnLoadPalette = Hybrid.ui.Button().also { it.setIcon(SpiriteIcons.SmallIcons.Palette_Load) }

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
        cbPaletteSelector.selectedItemBind.addObserver { new, _ ->
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
    private val _workspaceListenerK = master.workspaceSet.currentWorkspaceBind.addWeakObserver {  new, old ->
        rebuild()
    }
    private val _paletteObsK =master.paletteManager.paletteObservable.addObserver(
        object : PaletteObserver {
            override fun paletteChanged(evt: PaletteChangeEvent) {}
            override fun paletteSetChanged(evt: PaletteSetChangeEvent) {
                rebuild()
            }
        }.observer()
    )
    fun onClose() {
        _workspaceListenerK.void()
        _paletteObsK.void()
    }
    //endregion
}

private class PaletteView
private constructor(
        val master: IMasterControl,
        val imp: PaletteViewImp )
    : IComponent by SwComponent(imp)
{
    constructor(master: IMasterControl) : this(master, PaletteViewImp())

    val w get() = Math.max(width/12,1)
    val palette get() = master.paletteManager.currentPalette

    private val _curPlttK = master.paletteManager.currentPaletteBind.addWeakObserver { _, _ ->  imp.repaint() }

    fun onClose() {
        _curPlttK.void()
    }

    // region Imp
    init {imp.context = this}
    private class PaletteViewImp : SJPanel() {
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