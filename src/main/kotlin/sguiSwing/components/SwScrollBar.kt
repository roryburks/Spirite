package sguiSwing.components

import rb.owl.bindable.addObserver
import sgui.Orientation.HORIZONTAL
import sgui.Orientation.VERTICAL
import sgui.components.IComponent
import sgui.components.IScrollBar
import sgui.components.IScrollBarNonUIImp
import sgui.components.ScrollBarNonUI
import sguiSwing.components.SScrollPane.ModernScrollBarUI
import sguiSwing.mouseSystem.adaptMouseSystem
import javax.swing.JComponent
import javax.swing.JScrollBar

class SwScrollBar
private constructor(minScroll: Int, maxScroll: Int, startScroll: Int, scrollWidth: Int, val imp: JScrollBar)
    : IScrollBar,
        IScrollBarNonUIImp by ScrollBarNonUI(minScroll, maxScroll, startScroll, scrollWidth),
        ISwComponent by SwComponent(imp)
{
    constructor(
            orientation: sgui.Orientation,
            context: IComponent,
            minScroll: Int = 0,
            maxScroll: Int = 100,
            startScroll: Int = 0,
            scrollWidth : Int = 10) : this( minScroll, maxScroll, startScroll, scrollWidth, SwScrollBarImp(orientation, context))

    constructor(imp: JScrollBar) : this( imp.minimum, imp.maximum, imp.value, imp.visibleAmount, imp)




    override var orientation: sgui.Orientation
        get() = map(imp.orientation)
        set(value) { imp.orientation = if( value == VERTICAL) JScrollBar.VERTICAL else JScrollBar.HORIZONTAL}

    init {
        scrollBind.addObserver { new, _ -> imp.value = new }
        scrollWidthBind.addObserver { new, _ ->imp.visibleAmount = new }
        minScrollBind.addObserver { new, _ ->imp.minimum = new}
        maxScrollBind.addObserver { new, _ ->imp.maximum = new }
        imp.addAdjustmentListener {scroll = imp.value}
    }

    private class SwScrollBarImp(orientation: sgui.Orientation, context: IComponent) : JScrollBar() {
        init {
            adaptMouseSystem()
            isOpaque = false
            setUI( ModernScrollBarUI(context.component as JComponent))
            this.setOrientation(if( orientation == sgui.Orientation.VERTICAL) JScrollBar.VERTICAL else JScrollBar.HORIZONTAL)
        }
    }


    private fun map(jOrientation: Int) =  if( jOrientation == JScrollBar.VERTICAL) sgui.Orientation.VERTICAL else HORIZONTAL
    private fun map(sOrientation: sgui.Orientation) =  if( sOrientation == VERTICAL) JScrollBar.VERTICAL else JScrollBar.HORIZONTAL
}