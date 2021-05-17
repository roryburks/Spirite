package spirite.gui.menus

import sgui.core.UIPoint


interface IContextMenus {
    fun LaunchContextMenu(point: UIPoint, scheme: List<MenuItem>, obj: Any? = null)
}

