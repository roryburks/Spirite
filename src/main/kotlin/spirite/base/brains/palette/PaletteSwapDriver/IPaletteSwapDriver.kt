package spirite.base.brains.palette.PaletteSwapDriver

import spirite.base.imageData.MediumHandle

interface IPaletteSwapDriver {
    fun onMediumChenge(mediumHandle: MediumHandle)
}

object NilPaletteSwapDriver : IPaletteSwapDriver {
    override fun onMediumChenge(mediumHandle: MediumHandle) {}
}