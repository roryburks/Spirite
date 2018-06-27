package spirite.gui.resources

import spirite.base.brains.toolset.Tool
import java.awt.Graphics

object ToolIcons {
    val toolSheet by lazy { SwIcons.loadIconSheet("tool_icons.png") }

    fun drawToolIcon(g: Graphics, x: Int, y: Int, tool: Tool) {
        g.drawImage(toolSheet, x, y, x+24, y+24,
                tool.iconX*25, tool.iconY*25, tool.iconX*25 + 24, tool.iconY*25 + 24, null)
    }
}