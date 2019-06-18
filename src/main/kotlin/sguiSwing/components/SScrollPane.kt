package sguiSwing.components

import sguiSwing.mouseSystem.adaptMouseSystem
import sguiSwing.skin.Skin.Global
import java.awt.*
import javax.swing.*
import javax.swing.plaf.basic.BasicScrollBarUI

/**
 * This is an implementation of a JScrollPane with a modern UI
 *
 * @author Philipp Danner
 * https://stackoverflow.com/questions/16373459/java-jscrollbar-design
 */
open class SScrollPane constructor(
        val view: Component,
        vsbPolicy: Int = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
        hsbPolicy: Int = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED)
    : JScrollPane()
{
    init { adaptMouseSystem()}

    private val isVerticalScrollBarfNecessary: Boolean
        get() {
            val viewRect = viewport.viewRect
            val viewSize = viewport.viewSize
            return viewSize.getHeight() > viewRect.getHeight()
        }

    private val isHorizontalScrollBarNecessary: Boolean
        get() {
            val viewRect = viewport.viewRect
            val viewSize = viewport.viewSize
            return viewSize.getWidth() > viewRect.getWidth()
        }

    init {
        this.background = Global.Bg.jcolor
        this.getViewport().background = Global.Bg.jcolor
        border = null

        // Set ScrollBar UI
        val verticalScrollBar = getVerticalScrollBar()
        verticalScrollBar.isOpaque = false
        verticalScrollBar.setUI(ModernScrollBarUI(this))

        val horizontalScrollBar = getHorizontalScrollBar()
        horizontalScrollBar.isOpaque = false
        horizontalScrollBar.setUI(ModernScrollBarUI(this))

        layout = object : ScrollPaneLayout() {
            override fun layoutContainer(parent: Container) {
                val availR = (parent as JScrollPane).bounds
                availR.y = 0
                availR.x = availR.y

                // viewport
                val insets = parent.getInsets()
                availR.x = insets.left
                availR.y = insets.top
                availR.width -= insets.left + insets.right
                availR.height -= insets.top + insets.bottom
                if (viewport != null) {
                    viewport.bounds = availR
                }

                val vsbNeeded = isVerticalScrollBarfNecessary
                val hsbNeeded = isHorizontalScrollBarNecessary

                // vertical scroll bar
                val vsbR = Rectangle()
                vsbR.width = SB_SIZE
                vsbR.height = availR.height - if (hsbNeeded) vsbR.width else 0
                vsbR.x = availR.x + availR.width - vsbR.width
                vsbR.y = availR.y
                if (vsb != null) {
                    vsb.bounds = vsbR
                }

                // horizontal scroll bar
                val hsbR = Rectangle()
                hsbR.height = SB_SIZE
                hsbR.width = availR.width - if (vsbNeeded) hsbR.height else 0
                hsbR.x = availR.x
                hsbR.y = availR.y + availR.height - hsbR.height
                if (hsb != null) {
                    hsb.bounds = hsbR
                }
            }
        }

        // Layering
        setComponentZOrder(getVerticalScrollBar(), 0)
        setComponentZOrder(getHorizontalScrollBar(), 1)
        setComponentZOrder(getViewport(), 2)

        viewport.view = view
    }

    /**
     * Class extending the BasicScrollBarUI and overrides all necessary methods
     */
    class ModernScrollBarUI(private val sp: JComponent) : BasicScrollBarUI() {

        override fun createDecreaseButton(orientation: Int): JButton {
            return InvisibleScrollBarButton()
        }

        override fun createIncreaseButton(orientation: Int): JButton {
            return InvisibleScrollBarButton()
        }

        override fun paintTrack(g: Graphics, c: JComponent?, trackBounds: Rectangle) {}

        override fun paintThumb(g: Graphics, c: JComponent?, thumbBounds: Rectangle) {
            val alpha = if (isThumbRollover) SCROLL_BAR_ALPHA_ROLLOVER else SCROLL_BAR_ALPHA
            val orientation = scrollbar.orientation
            val x = thumbBounds.x
            val y = thumbBounds.y

            var width = if (orientation == JScrollBar.VERTICAL) THUMB_SIZE else thumbBounds.width
            width = Math.max(width, THUMB_SIZE)

            var height = if (orientation == JScrollBar.VERTICAL) thumbBounds.height else THUMB_SIZE
            height = Math.max(height, THUMB_SIZE)

            val graphics2D = g.create() as Graphics2D
            graphics2D.color = Color(THUMB_COLOR.red, THUMB_COLOR.green, THUMB_COLOR.blue, alpha)
            graphics2D.fillRect(x, y, width, height)
            graphics2D.dispose()
        }

        override fun setThumbBounds(x: Int, y: Int, width: Int, height: Int) {
            super.setThumbBounds(x, y, width, height)
            sp.repaint()
        }

        /**
         * Invisible Buttons, to hide scroll bar buttons
         */
        private class InvisibleScrollBarButton: JButton() {

            init {
                isOpaque = false
                isFocusable = false
                isFocusPainted = false
                isBorderPainted = false
                border = BorderFactory.createEmptyBorder()
            }
        }
    }

    companion object {
        private val SCROLL_BAR_ALPHA_ROLLOVER = 100
        private val SCROLL_BAR_ALPHA = 50
        private val THUMB_SIZE = 8
        private val SB_SIZE = 10
        private val THUMB_COLOR = Color.BLACK
    }
}