package spirite.pc.menus

import spirite.base.brains.commands.ICentralCommandExecutor
import sgui.swing.SUIPoint
import sgui.generic.UIPoint
import spirite.gui.menus.ContextMenus
import spirite.hybrid.Hybrid
import spirite.hybrid.MDebug
import spirite.hybrid.MDebug.WarningType
import javax.swing.*

class SwContextMenus(commandExecuter: ICentralCommandExecutor) : ContextMenus(commandExecuter) {
    val cmenu = JPopupMenu()

    override fun LaunchContextMenu(point: UIPoint, scheme: List<MenuItem>, extra: Any?) {
        cmenu.removeAll()

        constructMenu(cmenu, scheme.toList(), extra)
        cmenu.show( (point as? SUIPoint)?.component, point.x, point.y)

        SwingUtilities.invokeLater { cmenu.requestFocus() } // Meh
    }

    fun constructMenu(root: JComponent, menuScheme: List<MenuItem>, extra: Any? = null) {
        val isMenuBar = root is JMenuBar
        val isPopup = root is JPopupMenu
        val activeRootTree = MutableList<JMenuItem?>(10, {null})

        // Atempt to construct menu from parsed data in menu_scheme
        var activeDepth = 0
        menuScheme.forEachIndexed { index, item ->
            var mnemonic = 0.toChar()

            // Determine the depth of the node and crop off the extra .'s
            var depth = _imCountLevel(item.lexicon)
            var lexiocon = item.lexicon.substring(depth)

            if( depth > activeDepth) {
                MDebug.handleWarning(WarningType.INITIALIZATION, "Bad Menu Scheme.")
                depth = activeDepth
            }
            activeDepth = depth+1

            if( lexiocon == "-") {
                if( depth == 0) {
                    if( isPopup) (root as JPopupMenu).addSeparator()
                }
                else
                    (activeRootTree[depth-1] as JMenu).addSeparator()

                activeDepth--
            }
            else {
                // Detect the Mnemonic
                val mnemIndex = lexiocon.indexOf('&')
                if( mnemIndex != -1 && mnemIndex != lexiocon.length-1) {
                    mnemonic = lexiocon[mnemIndex+1]
                    lexiocon = lexiocon.substring(0, mnemIndex) + lexiocon.substring(mnemIndex+1)
                }

                // Determine if it needs to be a Menu (which contains other options nested in it)
                //	or a plain MenuItem (which doesn't)
                val node = when {
                    (depth != 0 || !isMenuBar) && (index+1 == menuScheme.size || _imCountLevel(menuScheme[index+1].lexicon) <= depth)
                        -> JMenuItem(lexiocon).also { it.isEnabled = item.enabled }
                    else -> JMenu(lexiocon).also { it.isEnabled = item.enabled }
                }
                if( mnemonic != 0.toChar())
                    node.setMnemonic(mnemonic)

                if( item.command != null)
                    node.addActionListener { Hybrid.gle.runInGLContext { commandExecuter.executeCommand(item.command.commandString, extra)} }
                if( item.customAction != null)
                    node.addActionListener { Hybrid.gle.runInGLContext { item.customAction.invoke()} }

                if( item.icon != null)
                    TODO()

                // Add the MenuItem into the appropriate workspace
                when {
                    depth == 0 -> root.add( node)
                    else -> activeRootTree[depth-1]!!.add(node)
                }

                activeRootTree[depth] = node
            }
        }
    }


    private val MAX_LEVEL = 10
    fun _imCountLevel(s: String): Int {
        var r = 0
        while (r < s.length && s[r] == '.')
            r++
        return Math.min(r, MAX_LEVEL)
    }
}