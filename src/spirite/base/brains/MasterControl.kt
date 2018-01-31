package spirite.base.brains

import spirite.base.brains.palette.IPaletteManager
import spirite.base.brains.tools.IToolsetManager
import spirite.base.file.ISaveEngine

class MasterControl(
        val hotkeyManager: IHotkeyManager,
        val toolsetManager: IToolsetManager,
        //val settingsManager: ISettingsManager,
        //frameManager: IFrameManager,
        val paletteManager: IPaletteManager,
        saveEngine: ISaveEngine
        //loadEngine: ILoadEngine,
        //dialogs: IDialogs,
        //contextMenus: IContextMenus,
) {

}