package spirite.base.brains.palette

import spirite.base.brains.Bindable
import spirite.base.brains.palette.IPaletteManager.PaletteChangeEvent
import spirite.base.brains.palette.IPaletteManager.PaletteSetChangeEvent
import spirite.hybrid.MDebug
import spirite.hybrid.MDebug.WarningType.STRUCTURAL

abstract class PaletteSet {
    abstract val onPaletteSetChangeTrigger: (PaletteSetChangeEvent) -> Unit
    abstract val onPaletteChangeTrigger: (PaletteChangeEvent) -> Unit

    val palettes : List<Palette> get() = _palettes
    private val _palettes = mutableListOf<Palette>(PSPalette("default"))

    val currentPaletteBind = Bindable<Palette?>(null)
    var currentPalette
        get() = currentPaletteBind.field
        set(value) {
            when {
                value == null || _palettes.contains(value) -> currentPaletteBind.field = value
                else -> TODO("Tried to select palette outside of tis set.  Add proper error handling.")
            }
        }

    fun resetPalettes( newPalette: Collection<Palette>) {
        _palettes.clear()
        _palettes.addAll( newPalette)
        onPaletteSetChangeTrigger(PaletteSetChangeEvent(this))
    }

    fun addPalette( name: String, select: Boolean, raw: ByteArray? = null) : Palette {
        val palette = PSPalette(name, raw)
        _palettes.add(palette)
        if( select)
            currentPalette = palette
        onPaletteSetChangeTrigger(PaletteSetChangeEvent(this))

        return palette
    }

    fun removePalette( index: Int) {
        if( index < 0 || index >= _palettes.size) {
            MDebug.handleWarning(STRUCTURAL, "Attempt to remove Palette Out of Bounds ($index not in [0,${_palettes.size})")
            return
        }

        val removed = _palettes.removeAt(index)
        if( currentPalette == removed) {
            currentPalette = when {
                index == 0 -> _palettes.firstOrNull()
                else -> _palettes.getOrNull(index-1)
            }
        }
        if( _palettes.size == 0) {
            val defPalette = PSPalette("Default")
            _palettes.add(defPalette )
            currentPalette = defPalette
        }
        onPaletteSetChangeTrigger(PaletteSetChangeEvent(this))
    }

    inner class PSPalette( string: String, raw: ByteArray? = null) : Palette(string, raw) {
        override var onChangeTrigger: (Palette) -> Unit = { onPaletteChangeTrigger.invoke(PaletteChangeEvent(it))}
    }
}