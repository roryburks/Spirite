package sgui.swing.components

import rb.owl.bindable.addObserver
import sgui.generic.Orientation
import sgui.generic.Orientation.HORIZONTAL
import sgui.generic.Orientation.VERTICAL
import sgui.generic.components.IComponent
import sgui.generic.components.IScrollBar
import sgui.generic.components.IScrollBarNonUIImp
import sgui.generic.components.ScrollBarNonUI
import sgui.swing.components.SScrollPane.ModernScrollBarUI
import sgui.swing.mouseSystem.adaptMouseSystem
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
        scrollBind.addObserver { new, _ -> imp.value = new }
        scrollWidthBind.addObserver { new, _ ->imp.visibleAmount = new }
        minScrollBind.addObserver { new, _ ->imp.minimum = new}
        maxScrollBind.addObserver { new, _ ->imp.maximum = new }
        imp.addAdjustmentListener {scroll = imp.value}
    }

    private class SwScrollBarImp(orientation: Orientation, context: IComponent) : JScrollBar() {
        init {
            adaptMouseSystem()
            isOpaque = false
            setUI( ModernScrollBarUI(context.component as JComponent))
            this.setOrientation(if( orientation == Orientation.VERTICAL) JScrollBar.VERTICAL else JScrollBar.HORIZONTAL)
        }
    }


    private fun map(jOrientation: Int) =  if( jOrientation == JScrollBar.VERTICAL) Orientation.VERTICAL else HORIZONTAL
    private fun map(sOrientation: Orientation) =  if( sOrientation == VERTICAL) JScrollBar.VERTICAL else JScrollBar.HORIZONTAL
}