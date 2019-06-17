package spirite.gui.views.work

import rbJvm.owl.addWeakObserver
import rb.owl.IObservable
import rb.owl.Observable
import rb.owl.bindable.addObserver
import rb.vectrix.linear.Vec2f
import rb.vectrix.mathUtil.f
import rb.vectrix.mathUtil.round
import spirite.base.brains.IMasterControl
import spirite.base.brains.IWorkspaceSet.WorkspaceObserver
import spirite.base.imageData.IImageWorkspace
import spirite.base.pen.Penner
import spirite.base.util.linear.Rect
import sgui.generic.Orientation.HORIZONTAL
import sgui.generic.Orientation.VERTICAL
import sgui.generic.components.IComponent
import sgui.generic.components.crossContainer.ICrossPanel
import spirite.hybrid.Hybrid
import sgui.swing.components.SwPanel
import java.awt.Font
import javax.swing.SwingUtilities


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

class WorkSection(val master: IMasterControl, val panel: ICrossPanel = Hybrid.ui.CrossPanel())
    : IComponent by panel
{
    private val views = mutableMapOf<IImageWorkspace, WorkSectionView>()
    val penner = Penner(this, master.toolsetManager, master.renderEngine, master.paletteManager)

    var currentWorkspace: IImageWorkspace? = null ; private set
    val currentView : WorkSectionView? get() {
        return views[currentWorkspace ?: return null]
    }

    private val scrollRatio = 10
    private val scrollBuffer = 100

    fun calibrateScrolls(hs: Int = hScroll.scroll, vs: Int = vScroll.scroll) {
        val workspace = currentWorkspace
        if( workspace != null) {
            val view = currentView

            val viewWidth = workAreaContainer.width
            val viewHeight = workAreaContainer.height
            val hMin  = -viewWidth + scrollBuffer
            val vMin = -viewHeight + scrollBuffer
            val hMax = workspace.width * (view?.zoom ?: 1f) - scrollBuffer
            val vMax = workspace.height * (view?.zoom ?: 1f) - scrollBuffer

            val ratio = scrollRatio.f
            hScroll.minScroll = Math.round(hMin / ratio)
            vScroll.minScroll = Math.round(vMin / ratio)
            hScroll.maxScroll = Math.round(hMax / ratio) + hScroll.scrollWidth
            vScroll.maxScroll = Math.round(vMax / ratio) + vScroll.scrollWidth

            hScroll.scroll = hs
            vScroll.scroll = vs
        }
    }

    fun doPreservingMousePoint(point: Vec2f, lambda: () -> Unit) {
        val view = currentView
        val pointInWorkspace = view?.tScreenToWorkspace?.apply(point) ?: Vec2f.Zero
        lambda.invoke()
        if( view != null) {
            hScroll.scroll = ((pointInWorkspace.xf * view.zoom - point.xf ) / scrollRatio).round
            vScroll.scroll = ((pointInWorkspace.yf * view.zoom - point.yf ) / scrollRatio).round
        }
    }

    fun refreshCoordinates( x: Int, y: Int) {
        val workspace = currentWorkspace
        if( workspace != null && Rect(0,0,workspace.width, workspace.height).contains(x, y))
            coordinateLabel.text = "$x,$y"
        else coordinateLabel.text = ""

    }

    // Region UI
    private val workAreaContainer = Hybrid.ui.CrossPanel()
    private val coordinateLabel = Hybrid.ui.Label()
    private val messageLabel = Hybrid.ui.Label()
    private val vScroll = Hybrid.ui.ScrollBar(VERTICAL, this)
    private val hScroll = Hybrid.ui.ScrollBar(HORIZONTAL, this)
    private val zoomPanel = SwPanel { g ->
        val view = currentView
        when {
            view == null -> {
            }
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



    init /* components */ {
        vScroll.scrollWidth = 50
        hScroll.scrollWidth = 50

        val glWorkArea = JOGLWorkAreaPanel(this, penner)
        workAreaContainer.setLayout { rows.add(glWorkArea) }

        hScroll.scrollBind.addObserver { new, _ -> currentView?.offsetX = new * scrollRatio}
        vScroll.scrollBind.addObserver { new, _ -> currentView?.offsetY = new * scrollRatio}
        workAreaContainer.onMouseWheelMoved = {
            doPreservingMousePoint(Vec2f(it.point.x.f, it.point.y.f)) {
                if( it.moveAmount > 0) currentView?.zoomOut()
                if( it.moveAmount < 0) currentView?.zoomIn()
                calibrateScrolls()
            }
        }

        workAreaContainer.onResize += {calibrateScrolls()}
        Hybrid.timing.createTimer(15, true) {Hybrid.gle.runInGLContext { penner.step() }}

        coordinateLabel.text = "Coordinate Label"
        messageLabel.text = "Message Label"

        this.onResize += {
            calibrateScrolls()
            redraw()
        }


        val barSize = 16
        panel.setLayout {
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

//    val imageObserver : ImageObserver =  object: ImageObserver {
//        override fun imageChanged(evt: ImageChangeEvent) {
//            redraw()
//        }
//    }.apply { master.centralObservatory.trackingImageObserver.addObserver(this) }

    val workspaceObserverContract =  master.workspaceSet.workspaceObserver.addWeakObserver(
            object: WorkspaceObserver {
                override fun workspaceCreated(newWorkspace: IImageWorkspace) {
                    val newView = WorkSectionView(newWorkspace)
                    views.put(newWorkspace, newView)

                    // By default centered
                    newView.offsetX = (newWorkspace.width/2 - workAreaContainer.width/2)
                    newView.offsetY = (newWorkspace.height/2 - workAreaContainer.height/2)
                }

                override fun workspaceRemoved(removedWorkspace: IImageWorkspace) {
                    views.remove(removedWorkspace)
                }

                override fun workspaceChanged(selectedWorkspace: IImageWorkspace?, previousSelected: IImageWorkspace?) {
                    _viewObservable.trigger { it.invoke() }
                    currentWorkspace = selectedWorkspace

                    SwingUtilities.invokeLater {    // TODO: Bad.  There should be a better solution.
                        when (selectedWorkspace) {
                            null -> {
                                hScroll.enabled = false
                                vScroll.enabled = false
                            }
                            else -> {
                                hScroll.enabled = true
                                vScroll.enabled = true
                                val view = views.get(selectedWorkspace)!!
                                calibrateScrolls(view.offsetX / scrollRatio, view.offsetY / scrollRatio)
                            }
                        }
                    }
                }
            }
    )

    val viewObservable : IObservable<()->Unit> get() = _viewObservable
    private val _viewObservable = Observable<()->Unit>()

    init {
    }
}