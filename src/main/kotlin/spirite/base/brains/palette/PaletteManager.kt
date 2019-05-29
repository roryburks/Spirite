package spirite.base.brains.palette

import rb.jvm.owl.addWeakObserver
import rb.owl.IObservable
import rb.owl.Observable
import rb.owl.bindable.Bindable
import rb.owl.bindable.IBindable
import rb.owl.bindable.addObserver
import spirite.base.brains.ICentralObservatory
import spirite.base.brains.ITopLevelFeedbackSystem
import spirite.base.brains.IWorkspaceSet
import spirite.base.brains.palette.IPaletteManager.*
import spirite.base.brains.palette.PaletteSwapDriver.IPaletteSwapDriver
import spirite.base.brains.palette.PaletteSwapDriver.TrackingPaletteSwapDriver
import spirite.base.brains.settings.ISettingsManager
import spirite.gui.components.dialogs.IDialog

interface IPaletteManager {
    val activeBelt : PaletteBelt

    val currentPaletteBind : IBindable<Palette>
    val currentPalette: Palette
    val globalPalette: Palette

    var driver : IPaletteSwapDriver

    fun makePaletteSet() : PaletteSet
    fun savePaletteInPrefs(name: String, palette: Palette)

    // Events
    interface PaletteObserver {
        fun paletteChanged( evt: PaletteChangeEvent)
        fun paletteSetChanged( evt: PaletteSetChangeEvent)
    }
    data class PaletteChangeEvent(val palette: Palette)
    data class PaletteSetChangeEvent(val paletteSet: PaletteSet)

    val paletteObservable : IObservable<PaletteObserver>
}

class PaletteManager(
        private val _workspaceSet: IWorkspaceSet,
        private val _settings: ISettingsManager,
        private val _dialog: IDialog,
        private val _centralObservatory: ICentralObservatory) : IPaletteManager
{

    override val activeBelt: PaletteBelt = PaletteBelt()

    override val paletteObservable = Observable<PaletteObserver>()

    override val globalPalette = object : Palette("Global") {
        override val onChangeTrigger: (Palette) -> Unit = {triggerPaletteChange(PaletteChangeEvent(this))}
    }

    override val currentPaletteBind = Bindable(globalPalette)
    override val currentPalette: Palette by currentPaletteBind

    override var driver : IPaletteSwapDriver = TrackingPaletteSwapDriver

    override fun makePaletteSet(): PaletteSet {
        val newPaletteSet = object : PaletteSet() {
            override val onPaletteSetChangeTrigger: (PaletteSetChangeEvent) -> Unit = { triggerPaletteSetChange(it) }
            override val onPaletteChangeTrigger: (PaletteChangeEvent) -> Unit = { triggerPaletteChange(it) }
        }

        // DuckTape way of getting PaletteManager.currentPalette to track CurrentWorkspace.PaletteSet.CurrentPalette
        newPaletteSet.currentPaletteBind.addObserver { new, old ->
            if( _workspaceSet.currentWorkspace?.paletteSet == newPaletteSet)
                currentPaletteBind.field = new ?: globalPalette
        }

        return newPaletteSet
    }

    override fun savePaletteInPrefs(name: String, palette: Palette) {
        val palettes = _settings.paletteList

        if( palettes.contains(name))
        {
            if( !_dialog.promptVerify("Palette $name already exists.  Overwrite?"))
                return
        }
        _settings.saveRawPalette(name, palette.compress())
    }

    // region Observer Bindings
    private val _wsObsK = _workspaceSet.currentWorkspaceBind.addWeakObserver { new, _ ->
        currentPaletteBind.field = new?.paletteSet?.currentPalette ?: globalPalette
    }

    private val _activePartK = _centralObservatory.activeDataBind.addWeakObserver { new, _ ->
        if( new != null) driver.onMediumChenge(new)
    }

    private fun triggerPaletteChange(evt: PaletteChangeEvent)
            = paletteObservable.trigger { obs -> obs.paletteChanged(evt) }
    private fun triggerPaletteSetChange(evt: PaletteSetChangeEvent)
            = paletteObservable.trigger { obs -> obs.paletteSetChanged(evt) }
    // endregion
}

