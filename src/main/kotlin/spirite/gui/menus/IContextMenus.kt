package spirite.gui.menus

import sgui.UIPoint
import spirite.base.brains.commands.ICentralCommandExecutor


interface IContextMenus {
    fun LaunchContextMenu(point: UIPoint, scheme: List<MenuItem>, obj: Any? = null)
}

