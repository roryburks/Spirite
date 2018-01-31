package spirite.base.v2.brains

import spirite.base.v2.brains.palette.IPaletteManager
import spirite.base.v2.brains.tools.IToolsetManager
import spirite.base.v2.file.ISaveEngine

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