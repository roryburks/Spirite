package spirite.gui.major.work

import spirite.base.pen.Penner
import jspirite.gui.SScrollPane.ModernScrollBarUI
import spirite.base.imageData.IImageWorkspace
import spirite.base.util.f
import spirite.base.util.floor
import spirite.base.util.linear.Vec2
import spirite.base.util.linear.Vec2i
import spirite.base.util.round
import spirite.gui.Bindable
import spirite.gui.Orientation.HORIZONATAL
import spirite.gui.Orientation.VERTICAL
import spirite.gui.basic.*
import java.awt.Font
import java.awt.Graphics
import javax.swing.GroupLayout
import javax.swing.JScrollBar
import kotlin.math.roundToInt


/**
 *WorkSection is a container for all the elements of the Draw area.  All external
 *	classes should interact with this Form Element (for things like controlling
 *	zoom, etc) instead of going to the inner Panel as this Form controls things
 *	such as scroll bars which feeds information in a one-way scheme.
 *
 *Internal Panels should use WorkPanel to convert screen coordinates to image
 *	coordinates.
 *
 * A note about understanding the various coordinate systems:
 * -The Scrollbar Values correspond directly to the offset of the image in windospace
 *   So if you wanted to draw image point 0,0 in the top-left corner, the scrollbar
 * 	 should be set to the values 0,0.  With the scroll at 1,1, then the point in the top
 * 	 left corner would be <10,10> (with SCROLL_RATIO of 10), meaning the image is drawn
 * 	 with offset <-10,-10>
 *
 * @author Rory Burks
 */
interface IWorkSection {
    val penner: Penner
}

class WorkSection : SPanel(), IComponent {
    private val views = mutableMapOf<IImageWorkspace, WorkSectionView>()

    var currentWorkspace: IImageWorkspace? = null
        set(value) {field = value; calibrateScrolls()}
    val currentView : WorkSectionView? get() {
        return views[currentWorkspace ?: return null]
    }

    private val scrollRatio = 10
    private val scrollBuffer = 100

    fun calibrateScrolls() {
        val workspace = currentWorkspace
        when(workspace) {
            null -> {
                vScroll.enabled = false
                hScroll.enabled = false
            }
            else -> {
                val view = currentView ?: WorkSectionView(workspace).apply {
                    views.put(workspace, this)
                }

                vScroll.enabled = true
                hScroll.enabled = true

                val viewWidth = workAreaContainer.width
                val viewHeight = workAreaContainer.height
                val hMin  = -viewWidth + scrollBuffer
                val vMin = -viewHeight + scrollBuffer
                val hMax = workspace.width * view.zoom - scrollBuffer
                val vMax = workspace.height * view.zoom - scrollBuffer

                val ratio = scrollRatio.f
                val hs = hScroll.scroll
                val vs = vScroll.scroll
                hScroll.minScroll = Math.round(hMin / ratio)
                vScroll.minScroll = Math.round(vMin / ratio)
                hScroll.maxScroll = Math.round(hMax / ratio) + hScroll.scrollWidth
                vScroll.maxScroll = Math.round(vMax / ratio) + vScroll.scrollWidth
                hScroll.scroll = hs
                vScroll.scroll = vs
            }
        }
    }

    fun doPreservingMousePoint( point: Vec2, lambda: () -> Unit) {
        val view = currentView
        val pointInWorkspace = view?.tScreenToWorkspace?.apply(point) ?: Vec2.Zero
        lambda.invoke()
        if( view != null) {
            hScroll.scroll = ((pointInWorkspace.x * view.zoom - point.x ) / scrollRatio).round
            vScroll.scroll = ((pointInWorkspace.y * view.zoom - point.y ) / scrollRatio).round
        }
    }

    // Region UI
    private val workAreaContainer = SPanel()
    private val coordinateLabel = SLabel()
    private val messageLabel = SLabel()
    private val vScroll = SScrollBar(VERTICAL, this)
    private val hScroll = SScrollBar(HORIZONATAL, this)
    private val zoomPanel = SPanel {g ->
        val view = currentView
        when {
            view == null -> {}
            view.zoomLevel >= 0 -> {
                g.font = Font("Tahoma", Font.PLAIN, 12)
                g.drawString(Integer.toString(view.zoomLevel + 1), width - if (view.zoomLevel > 8) 16 else 12, height - 5)
            }
            else -> {
                g.font = Font("Tahoma", Font.PLAIN, 8)
                g.drawString("1/", this.width - 15, this.height - 10)
                g.font = Font("Tahoma", Font.PLAIN, 10)
                g.drawString(Integer.toString(-view.zoomLevel + 1), width - if (view.zoomLevel < -8) 10 else 8, height - 4)
            }
        }
    }

    fun initComponents() {
        vScroll.scrollWidth = 50
        hScroll.scrollWidth = 50

        val glWorkArea = GLWorkArea(this)
        workAreaContainer.setLayout { rows.add(glWorkArea) }

        hScroll.scrollBind.addListener {currentView?.offsetX = it * scrollRatio}
        vScroll.scrollBind.addListener {currentView?.offsetY = it * scrollRatio}
        workAreaContainer.onMouseWheelMoved = {
            doPreservingMousePoint(Vec2(it.point.x.f, it.point.y.f), {
                if( it.moveAmount > 0) currentView?.zoomOut()
                if( it.moveAmount < 0) currentView?.zoomIn()
                calibrateScrolls()
            })
        }

        coordinateLabel.label = "Coordinate Label"
        messageLabel.label = "Message Label"

        this.onResize = {calibrateScrolls()}


        val barSize = 16
        setLayout {
            rows += {
                add(workAreaContainer, flex = 200f)
                add(vScroll, width = barSize)
                flex = 200f
            }
            rows += {
                add(hScroll, flex = 200f)
                add(zoomPanel, width = barSize)
                height = barSize
            }
            rows += {
                add(coordinateLabel)
                addGap(0, 3, Int.MAX_VALUE)
                add(messageLabel)
                height = 24
            }
        }
    }
    // endregion


    init {
        initComponents()
    }
}