package spirite.pc.gui.basic

import jspirite.gui.SScrollPane.ModernScrollBarUI
import spirite.gui.Orientation
import spirite.gui.Orientation.HORIZONATAL
import spirite.gui.Orientation.VERTICAL
import spirite.gui.components.basic.IComponent
import spirite.gui.components.basic.IScrollBar
import spirite.gui.components.basic.IScrollBarNonUIImp
import spirite.gui.components.basic.ScrollBarNonUI
import javax.swing.JComponent
import javax.swing.JScrollBar

class SwScrollBar
private constructor(minScroll: Int, maxScroll: Int, startScroll: Int, scrollWidth: Int, val imp: SwScrollBarImp)
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


    override var orientation: Orientation
        get() = if( imp.getOrientation() == JScrollBar.VERTICAL) Orientation.VERTICAL else HORIZONATAL
        set(value) { imp.orientation = if( value == VERTICAL) JScrollBar.VERTICAL else JScrollBar.HORIZONTAL}

    init {
        scrollBind.addListener { imp.value = it }
        scrollWidthBind.addListener { imp.visibleAmount = it }
        minScrollBind.addListener { imp.minimum = it}
        maxScrollBind.addListener { imp.maximum = it }
        imp.addAdjustmentListener {scroll = imp.value}
    }

    private class SwScrollBarImp(orientation: Orientation, context: IComponent) : JScrollBar() {
        init {
            isOpaque = false
            setUI( ModernScrollBarUI(context.component as JComponent))
            this.setOrientation(if( orientation == Orientation.VERTICAL) JScrollBar.VERTICAL else JScrollBar.HORIZONTAL)
        }
    }
}