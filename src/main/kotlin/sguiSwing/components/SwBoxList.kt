package sguiSwing.components

import sgui.components.BoxList
import sgui.components.IComponent
import sgui.components.crossContainer.CrossInitializer
import sguiSwing.SwUtil
import sguiSwing.SwingComponentProvider
import sguiSwing.advancedComponents.CrossContainer.CrossLayout
import java.awt.GridLayout
import java.awt.Point
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.KeyEvent
import javax.swing.Action
import javax.swing.KeyStroke
import kotlin.math.max
import kotlin.math.min

class SwBoxList<T>
private constructor(boxWidth: Int, boxHeight: Int, entries: Collection<T>?, private val imp: SwBoxListImp)
    : BoxList<T>( boxWidth, boxHeight, entries, SwingComponentProvider, imp)
{
    constructor(boxWidth:Int, boxHeight: Int, entries: Collection<T>? = null) : this(boxWidth, boxHeight, entries, SwBoxListImp())

    init {
        imp.addComponentListener( object : ComponentAdapter(){
            override fun componentResized(e: ComponentEvent?) {rebuild()}
        })
        onMousePress += {e ->
            if( enabled) {
                imp.requestFocus()
                val comp = imp.content.getComponentAt(Point(e.point.x, e.point.y))
                val index = _entries.indexOfFirst { it.component.component.jcomponent == comp }
                if( index != -1)
                    selectedIndex = index
            }
        }
    }
    init /*Map*/ {
        val actionMap = HashMap<KeyStroke, Action>(4)

        actionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), SwUtil.buildAction {
            if (selectedIndex != -1 && enabled)
                selectedIndex = max(0, selectedIndex - numPerRow)
        })
        actionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), SwUtil.buildAction {
            if (selectedIndex != -1 && enabled)
                selectedIndex = min(_entries.size - 1, selectedIndex + numPerRow)
        })
        actionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), SwUtil.buildAction {
            if (selectedIndex != -1 && enabled)
                selectedIndex = max(0, selectedIndex - 1)
        })
        actionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), SwUtil.buildAction {
            if (selectedIndex != -1 && enabled)
                selectedIndex = min(_entries.size - 1, selectedIndex + 1)
        })
        actionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.SHIFT_DOWN_MASK), SwUtil.buildAction {
            if (selectedIndex != -1 && selectedIndex != 0 && enabled) {
                if (attemptMove(selectedIndex, selectedIndex - 1))
                    selectedIndex -= 1
            }
        })
        actionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.SHIFT_DOWN_MASK), SwUtil.buildAction {
            if (selectedIndex != -1 && selectedIndex != _entries.size - 1 && enabled) {
                if (attemptMove(selectedIndex, selectedIndex + 1))
                    selectedIndex += 1
            }
        })
        actionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.SHIFT_DOWN_MASK), SwUtil.buildAction {
            val to = Math.max(0, selectedIndex - numPerRow)

            if (selectedIndex != -1 && to != selectedIndex && enabled) {
                if (attemptMove(selectedIndex, to))
                    selectedIndex = to
            }
        })
        actionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.SHIFT_DOWN_MASK), SwUtil.buildAction {
            val to = Math.min(_entries.size - 1, selectedIndex + numPerRow)

            if (selectedIndex != -1 && to != selectedIndex && enabled) {
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
            content.layout = CrossLayout.buildCrossLayout(content, constructor= constructor)
        }

        val content = SJPanel()
        val scroll = SScrollPane(content)

        init {
            layout = GridLayout()
            add(scroll)
        }
    }
}