package sguiSwing.components

import rb.owl.bindable.addObserver
import sgui.components.IComponent
import sgui.components.TabbedPanePartial
import sguiSwing.skin.Skin.BevelBorder.*
import sguiSwing.skin.Skin.TabbedPane.*
import java.awt.*
import javax.swing.JTabbedPane
import javax.swing.plaf.basic.BasicTabbedPaneUI

class SwTabbedPane
private constructor(
        private val imp : SwTabbedPaneImp
) : TabbedPanePartial(),
        ISwComponent by SwComponent(imp)
{
    constructor() : this(SwTabbedPaneImp())

    init {
        selectedIndexBind.addObserver { new, _ -> imp.selectedIndex = new }
        imp.addChangeListener { selectedIndex = imp.selectedIndex }
    }

    override fun addTab(title: String, component: IComponent?) {
        tabs.add(Tab(title, component))
        imp.addTab(title, component?.jcomponent)
    }

    override fun setComponentAt(index: Int, newComponent: IComponent?) {
        tabs.getOrNull(index)?.component = newComponent
        imp.setComponentAt(index, newComponent?.jcomponent)
    }

    override fun setTitleAt(index: Int, newTitle: String) {
        tabs.getOrNull(index)?.title = newTitle
        imp.setTitleAt(index, newTitle)
    }

    override fun removeTabAt(index: Int) {
        imp.removeTabAt(index)
        tabs.removeAt(index)
    }

    private class SwTabbedPaneImp : JTabbedPane()
    {
        init {
            val sui = StpUi()
            ui = sui
            sui.installUI(this)
            border = null
            font = Font("Tahoma", Font.PLAIN, 10)
        }

        private inner class StpUi : BasicTabbedPaneUI() {

            override fun createLayoutManager(): LayoutManager {
                return object : TabbedPaneLayout() {
                    override fun calculateTabRects(tabPlacement: Int, tabCount: Int) {
                        super.calculateTabRects(tabPlacement, tabCount)

                        rects.forEachIndexed { i, rect ->  rect.width -= 10 ; rect.x -= 10*i}
                    }
                }
            }


            override fun paintTab(g: Graphics, tabPlacement: Int, rects: Array<Rectangle>, tabIndex: Int, iconRect: Rectangle,
                                  textRect: Rectangle) {
                val title = getTitleAt(tabIndex)
                val g2 = g.create() as Graphics2D

                g2.color = if( selectedIndex == tabIndex) SelectedBg.jcolor else UnselectedBg.jcolor

                val x1 = rects[tabIndex].x
                val y1 = rects[tabIndex].y
                val x2 = rects[tabIndex].x + rects[tabIndex].width
                val y2 = rects[tabIndex].y + rects[tabIndex].height
                val x = intArrayOf(x1, x1, x1 + 5, x2 - 5, x2)
                val y = intArrayOf(y2, y1 + 5, y1, y1, y2)
                g2.fillPolygon(x, y, 5)

                g2.color = TabBorder.jcolor
                g2.drawPolyline(x, y, 5)

                g2.color = TabText.jcolor
                g2.drawString(title, x1 + 5, y2 - 5)

                g2.dispose()
            }

            override fun getContentBorderInsets(tabPlacement: Int) = Insets(2, 2, 4, 4)

            override fun paintContentBorderBottomEdge(g: Graphics, tabPlacement: Int, selectedIndex: Int, x: Int, y: Int,
                                                      w: Int, h: Int) {
                g.color = Med.jcolor
                g.fillRect(x, y, w, 2)
                g.fillRect(x, y, 2, h)

                g.color = Dark.jcolor
                g.fillRect(x + w - 4, y, 4, h)
                g.fillRect(x, y + h - 4, w, 4)


                g.color = Light.jcolor
                g.drawLine(x + 1, y + 1, x + w - 2, y + 1)
                g.drawLine(x + 1, y + 1, x + 1, y + h - 2)
                g.drawLine(x + 1, y + h - 2, x + w - 2, y + h - 2)
                g.drawLine(x + w - 2, y + 1, x + w - 2, y + h - 2)

                g.color = Darker.jcolor
                g.drawLine(x, y + h - 1, x + w - 1, y + h - 1)
                g.drawLine(x + w - 1, y, x + w - 1, y + h - 1)
            }

            override fun paintContentBorderLeftEdge(g: Graphics, tabPlacement: Int, selectedIndex: Int, x: Int, y: Int, w: Int, h: Int) {}
            override fun paintContentBorderRightEdge(g: Graphics, tabPlacement: Int, selectedIndex: Int, x: Int, y: Int, w: Int, h: Int) {}
            override fun paintContentBorderTopEdge(g: Graphics, tabPlacement: Int, selectedIndex: Int, x: Int, y: Int, w: Int, h: Int) {}
        }
    }
}