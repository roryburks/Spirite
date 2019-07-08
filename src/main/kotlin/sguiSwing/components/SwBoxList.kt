package sguiSwing.components

import sgui.components.BoxList
import sgui.components.IComponent
import sgui.components.crossContainer.CrossInitializer
import sgui.components.events.MouseEvent.MouseButton.RIGHT
import sguiSwing.SwUtil
import sguiSwing.SwingComponentProvider
import sguiSwing.advancedComponents.CrossContainer.CrossLayout
import java.awt.Component
import java.awt.GridLayout
import java.awt.Point
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.KeyEvent
import javax.swing.Action
import javax.swing.KeyStroke
import javax.swing.SwingUtilities
import kotlin.math.max
import kotlin.math.min

class SwBoxList<T>
private constructor(boxWidth: Int, boxHeight: Int, entries: Collection<T>?, private val imp: SwBoxListImp)
    : BoxList<T>( boxWidth, boxHeight, entries, SwingComponentProvider, imp)
    where T : Any
{
    constructor(boxWidth:Int, boxHeight: Int, entries: Collection<T>? = null) : this(boxWidth, boxHeight, entries, SwBoxListImp())

    fun getIndexFromComponent( component: Component) : Int?{
        val t = _componentMap.entries
                .firstOrNull { SwingUtilities.isDescendingFrom(it.value.component.jcomponent,component) }
                ?: return null
        val ti = data.entries.indexOf(t.key)
        return if( ti == -1) null else ti
    }

    init {
        imp.addComponentListener( object : ComponentAdapter(){
            override fun componentResized(e: ComponentEvent?) {rebuild()}
        })
        onMousePress += {e ->
            if( enabled) {
                imp.requestFocus()
                val comp = imp.content.getComponentAt(Point(e.point.x, e.point.y))
                val index = getIndexFromComponent(comp)
                if (index != null) {
                    val t = data.entries[index]

                    if( e.button != RIGHT) {
                        if (data.multiSelectEnabled && e.holdingCtrl)
                            data.addSelection(t)
                        else if (data.multiSelectEnabled && e.holdingShift)
                            data.removeSelection(t)
                        else
                            data.setSelection(t)
                    }

                    _componentMap[t]?.onClick(e)
                }
            }
        }
    }
    init /*Map*/ {
        val actionMap = HashMap<KeyStroke, Action>(4)

        actionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), SwUtil.buildAction {
            if (data.selectedIndex != -1 && enabled)
                data.selectedIndex = max(0, data.selectedIndex - numPerRow)
        })
        actionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), SwUtil.buildAction {
            if (data.selectedIndex != -1 && enabled)
                data.selectedIndex = min(data.entries.size - 1, data.selectedIndex + numPerRow)
        })
        actionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), SwUtil.buildAction {
            if (data.selectedIndex != -1 && enabled)
                data.selectedIndex = max(0, data.selectedIndex - 1)
        })
        actionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), SwUtil.buildAction {
            if (data.selectedIndex != -1 && enabled)
                data.selectedIndex = min(data.entries.size - 1, data.selectedIndex + 1)
        })
        actionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.SHIFT_DOWN_MASK), SwUtil.buildAction {
            if (data.selectedIndex != -1 && data.selectedIndex != 0 && enabled) {
                if (attemptMove(data.selectedIndex, data.selectedIndex - 1))
                    data.selectedIndex -= 1
            }
        })
        actionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.SHIFT_DOWN_MASK), SwUtil.buildAction {
            if (data.selectedIndex != -1 && data.selectedIndex != data.entries.size - 1 && enabled) {
                if (attemptMove(data.selectedIndex, data.selectedIndex + 1))
                    data.selectedIndex += 1
            }
        })
        actionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.SHIFT_DOWN_MASK), SwUtil.buildAction {
            val to = Math.max(0, data.selectedIndex - numPerRow)

            if (data.selectedIndex != -1 && to != data.selectedIndex && enabled) {
                if (attemptMove(data.selectedIndex, to))
                    data.selectedIndex = to
            }
        })
        actionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.SHIFT_DOWN_MASK), SwUtil.buildAction {
            val to = Math.min(data.entries.size - 1, data.selectedIndex + numPerRow)

            if (data.selectedIndex != -1 && to != data.selectedIndex && enabled) {
                if (attemptMove(data.selectedIndex, to))
                    data.selectedIndex = to
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