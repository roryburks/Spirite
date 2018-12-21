package spirite.base.brains.palette

import rb.jvm.owl.addWeakObserver
import spirite.base.brains.*
import spirite.base.brains.palette.IPaletteManager.*
import spirite.base.brains.settings.ISettingsManager
import spirite.base.util.binding.CruddyBindable
import spirite.base.util.binding.ICruddyOldBindable
import spirite.gui.components.dialogs.IDialog

interface IPaletteManager {
    val activeBelt : PaletteBelt

    val currentPaletteBind : ICruddyOldBindable<Palette>
    val currentPalette: Palette
    val globalPalette: Palette

    fun makePaletteSet() : PaletteSet
    fun savePaletteInPrefs(name: String, palette: Palette)

    interface PaletteObserver {
        fun paletteChanged( evt: PaletteChangeEvent)
        fun paletteSetChanged( evt: PaletteSetChangeEvent)
    }
    data class PaletteChangeEvent(val palette: Palette)
    data class PaletteSetChangeEvent(val paletteSet: PaletteSet)

    val paletteObservable : ICruddyOldObservable<PaletteObserver>
}

class PaletteManager(
        private val workspaceSet: IWorkspaceSet,
        private val settings: ISettingsManager,
        private val dialog: IDialog) : IPaletteManager {

    override val activeBelt: PaletteBelt = PaletteBelt()

    override val paletteObservable = CruddyOldObservable<PaletteObserver>()

    override val globalPalette = object : Palette("Global") {
        override val onChangeTrigger: (Palette) -> Unit = {triggerPaletteChange(PaletteChangeEvent(this))}
    }

    override val currentPaletteBind = CruddyBindable(globalPalette)
    override val currentPalette: Palette by currentPaletteBind

    override fun makePaletteSet(): PaletteSet {
        val newPaletteSet = object : PaletteSet() {
            override val onPaletteSetChangeTrigger: (PaletteSetChangeEvent) -> Unit = { triggerPaletteSetChange(it) }
            override val onPaletteChangeTrigger: (PaletteChangeEvent) -> Unit = { triggerPaletteChange(it) }
        }

        // DuckTape way of getting PaletteManager.currentPalette to track CurrentWorkspace.PaletteSet.CurrentPalette
        newPaletteSet.currentPaletteBind.addListener { new, old ->
            if( workspaceSet.currentWorkspace?.paletteSet == newPaletteSet)
                currentPaletteBind.field = new ?: globalPalette
        }

        return newPaletteSet
    }

    override fun savePaletteInPrefs(name: String, palette: Palette) {
        val palettes = settings.paletteList

        if( palettes.contains(name))
        {
            if( !dialog.promptVerify("Palette $name already exists.  Overwrite?"))
                return
        }
        settings.saveRawPalette(name, palette.compress())
    }

    // region Observer Bindings
    private val wsObsK = workspaceSet.currentWorkspaceBind.addWeakObserver {new, _ ->
        currentPaletteBind.field = new?.paletteSet?.currentPalette ?: globalPalette
    }

    private fun triggerPaletteChange(evt: PaletteChangeEvent)
            = paletteObservable.trigger { obs -> obs.paletteChanged(evt) }
    private fun triggerPaletteSetChange(evt: PaletteSetChangeEvent)
            = paletteObservable.trigger { obs -> obs.paletteSetChanged(evt) }
    // endregion
}

