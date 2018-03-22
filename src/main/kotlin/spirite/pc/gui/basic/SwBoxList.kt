package spirite.pc.gui.basic

import jspirite.gui.SScrollPane
import spirite.gui.components.advanced.crossContainer.CrossInitializer
import spirite.gui.components.basic.BoxList
import spirite.gui.components.basic.IComponent
import spirite.pc.gui.SwUtil
import java.awt.GridLayout
import java.awt.event.*
import javax.swing.Action
import javax.swing.KeyStroke
import kotlin.math.max
import kotlin.math.min

class SwBoxList<T>
private constructor(boxWidth: Int, boxHeight: Int, entries: Collection<T>?, val imp: SwBoxListImp)
    : BoxList<T>( boxWidth, boxHeight, entries, imp)
{
    constructor(boxWidth:Int, boxHeight: Int, entries: Collection<T>? = null) : this(boxWidth, boxHeight, entries, SwBoxListImp())

    init {
        imp.addComponentListener( object : ComponentAdapter(){
            override fun componentResized(e: ComponentEvent?) {rebuild()}
        })
        imp.content.addMouseListener(object: MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                imp.requestFocus()
                val comp = imp.content.getComponentAt(e.point)
                val index = _entries.indexOfFirst { it.component.component.jcomponent == comp }
                selectedIndex = index
            }
        })
    }
    init /*Map*/ {
        val actionMap = HashMap<KeyStroke, Action>(4)

        actionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), SwUtil.buildAction {
            if (selectedIndex != -1)
                selectedIndex = max(0, selectedIndex - numPerRow)
        })
        actionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), SwUtil.buildAction {
            if (selectedIndex != -1)
                selectedIndex = min(_entries.size - 1, selectedIndex + numPerRow)
        })
        actionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), SwUtil.buildAction {
            if (selectedIndex != -1)
                selectedIndex = max(0, selectedIndex - 1)
        })
        actionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), SwUtil.buildAction {
            if (selectedIndex != -1)
                selectedIndex = min(_entries.size - 1, selectedIndex + 1)
        })
        actionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.SHIFT_DOWN_MASK), SwUtil.buildAction {
            if (selectedIndex != -1 && selectedIndex != 0) {
                if (attemptMove(selectedIndex, selectedIndex - 1))
                    selectedIndex -= 1
            }
        })
        actionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.SHIFT_DOWN_MASK), SwUtil.buildAction {
            if (selectedIndex != -1 && selectedIndex != _entries.size - 1) {
                if (attemptMove(selectedIndex, selectedIndex + 1))
                    selectedIndex += 1
            }
        })
        actionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.SHIFT_DOWN_MASK), SwUtil.buildAction {
            val to = Math.max(0, selectedIndex - numPerRow)

            if (selectedIndex != -1 && to != selectedIndex) {
                if (attemptMove(selectedIndex, to))
                    selectedIndex = to
            }
        })
        actionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.SHIFT_DOWN_MASK), SwUtil.buildAction {
            val to = Math.min(_entries.size - 1, selectedIndex + numPerRow)

            if (selectedIndex != -1 && to != selectedIndex) {
                if (attemptMove(selectedIndex, to))
                    selectedIndex = to
            }
        })

        SwUtil.buildActionMap(imp, actionMap)
    }

    private class SwBoxListImp  : SJPanel(), IBoxListImp
    {
        override val component: IComponent = SwComponent(this)
        override fun setLayout(constructor: CrossInitializer.() -> Unit) {
            content.removeAll()
            content.layout = CrossLayout.buildCrossLayout(content, constructor)
        }

        val content = SJPanel()
        val scroll = SScrollPane(content)

        init {
            layout = GridLayout()
            add(scroll)
        }
    }
}