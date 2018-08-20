package spirite.pc.gui.basic

import spirite.pc.gui.basic.SScrollPane.ModernScrollBarUI
import spirite.gui.Orientation
import spirite.gui.Orientation.HORIZONTAL
import spirite.gui.Orientation.VERTICAL
import spirite.gui.components.basic.IComponent
import spirite.gui.components.basic.IScrollBar
import spirite.gui.components.basic.IScrollBarNonUIImp
import spirite.gui.components.basic.ScrollBarNonUI
import javax.swing.JComponent
import javax.swing.JScrollBar

class SwScrollBar
private constructor(minScroll: Int, maxScroll: Int, startScroll: Int, scrollWidth: Int, val imp: JScrollBar)
    : IScrollBar,
        IScrollBarNonUIImp by ScrollBarNonUI(minScroll, maxScroll, startScroll, scrollWidth),
        ISwComponent by SwComponent(imp)
{
    constructor(
            orientation: Orientation,
            context: IComponent,
            minScroll: Int = 0,
            maxScroll: Int = 100,
            startScroll: Int = 0,
            scrollWidth : Int = 10) : this( minScroll, maxScroll, startScroll, scrollWidth, SwScrollBarImp(orientation, context))

    constructor(imp: JScrollBar) : this( imp.minimum, imp.maximum, imp.value, imp.visibleAmount, imp)




    override var orientation: Orientation
        get() = map(imp.orientation)
        set(value) { imp.orientation = if( value == VERTICAL) JScrollBar.VERTICAL else JScrollBar.HORIZONTAL}

    init {
        scrollBind.addListener { new, old ->imp.value = new }
        scrollWidthBind.addListener { new, old ->imp.visibleAmount = new }
        minScrollBind.addListener { new, old ->imp.minimum = new}
        maxScrollBind.addListener { new, old ->imp.maximum = new }
        imp.addAdjustmentListener {scroll = imp.value}
    }

    private class SwScrollBarImp(orientation: Orientation, context: IComponent) : JScrollBar() {
        init {
            isOpaque = false
            setUI( ModernScrollBarUI(context.component as JComponent))
            this.setOrientation(if( orientation == Orientation.VERTICAL) JScrollBar.VERTICAL else JScrollBar.HORIZONTAL)
        }
    }

    companion object {
        fun map(jOrientation: Int) =  if( jOrientation == JScrollBar.VERTICAL) Orientation.VERTICAL else HORIZONTAL
        fun map(sOrientation: Orientation) =  if( sOrientation == VERTICAL) JScrollBar.VERTICAL else JScrollBar.HORIZONTAL
    }
}