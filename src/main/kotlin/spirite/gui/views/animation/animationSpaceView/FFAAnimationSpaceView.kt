package spirite.gui.views.animation.animationSpaceView

import spirite.base.imageData.animation.ffa.FixedFrameAnimation
import spirite.base.imageData.animationSpaces.FFAAnimationSpace
import spirite.base.imageData.animationSpaces.IAnimationSpaceView.InternalAnimationSpaceObserver
import spirite.base.util.linear.Vec2i
import spirite.gui.UIPoint
import spirite.gui.components.advanced.crossContainer.CrossInitializer
import spirite.gui.components.basic.IButton
import spirite.gui.components.basic.IComponent
import spirite.gui.components.basic.ICrossPanel
import spirite.gui.resources.Skin.FFAAnimation
import spirite.hybrid.Hybrid
import spirite.pc.gui.basic.SwComponent
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import javax.swing.JPanel

class FFAAnimationSpaceView
private constructor(
        val animationSpace: FFAAnimationSpace,
        private val imp : FFASpacePanelImp)
    : IComponent by SwComponent(imp)
{
    init {
        imp.context = this
        rebuild()
    }
    public constructor(animationSpace: FFAAnimationSpace) : this(animationSpace, FFASpacePanelImp())

    fun rebuild()
    {
        imp.setLayout {

            animationSpace.animations.forEach {ffa->
                val location = animationSpace.stateView.logicalSpace[ffa] ?: Vec2i.Zero
                rows.addFlatGroup(location.y) {
                    addGap(location.x)
                    add(FFABlock(ffa))
                }
            }
            rows += {
                add(Hybrid.ui.CrossPanel())
            }
        }
    }

    fun getFrameFromPoint(x: Int, y: Int) : Pair<FFAAnimation, Int>
    {
        TODO()
    }

    private inner class FFABlock(
            val animation: FixedFrameAnimation,
            val imp : ICrossPanel = Hybrid.ui.CrossPanel())
        : IComponent by imp
    {
        val min = animation.start
        val max = animation.end

        val compMap = mutableMapOf<Any,Int>()
        init {
            imp.setLayout {

                (min until max).forEach {
                    val comp = Hybrid.ui.Button(it.toString())
                    comp.markAsPassThrough()
                    compMap[comp.component] =  it
                    cols.add(comp, 24, 24)
                }
            }

            imp.onMousePress += {evt ->
                behavior = behavior ?: when {
                    evt.holdingCtrl -> MoveAnimationBehavior()
                    else -> {
                        val frame = evt.point.convert(imp).x  / 24
                        if( frame < min || frame >= max) null
                        else CreateLinkBehavior(frame)
                    }
                }
                behavior?.onPress(evt.point)
            }
            imp.onMouseDrag += {evt ->behavior?.onMove(evt.point)}
            imp.onMouseRelease += {evt -> behavior?.onRelease(evt.point)}
        }

        private var behavior : Behavior? = null
        private abstract inner class Behavior
        {
            abstract fun onPress(p: UIPoint)
            abstract fun onRelease(p: UIPoint)
            abstract fun onMove(p: UIPoint)
        }

        private inner class MoveAnimationBehavior : Behavior()
        {
            var startX = 0
            var startY = 0
            var parentSpaceX = 0
            var parentSpaceY = 0
            var offsetX = 0
            var offsetY = 0
            override fun onPress(p: UIPoint) {
                startX = p.x
                startY = p.y

                val inParentSpace = p.convert(this@FFAAnimationSpaceView)
                offsetX = this@FFABlock.x - inParentSpace.x
                offsetY = this@FFABlock.y - inParentSpace.y
                parentSpaceX = inParentSpace.x
                parentSpaceY = inParentSpace.y

                drawer = {
                    println("drawer")
                    val w = this@FFABlock.width
                    val h = this@FFABlock.height
                    it.color = Color.BLACK
                    it.stroke = BasicStroke(2f)
                    it.drawRect(parentSpaceX + offsetX, parentSpaceY + offsetY, w, h)
                }
            }
            override fun onRelease(p: UIPoint) {
                val old = animationSpace.stateView.logicalSpace[animation]
                animationSpace.stateView.setLogicalSpace(animation, Vec2i((old?.x ?:0) + p.x - startX,(old?.y ?:0) + p.y - startY))
                behavior = null
                drawer = null
            }
            override fun onMove(p: UIPoint) {
                val inParentSpace = p.convert(this@FFAAnimationSpaceView)
                parentSpaceX = inParentSpace.x
                parentSpaceY = inParentSpace.y
                this@FFAAnimationSpaceView.redraw()
            }
        }

        private inner class CreateLinkBehavior(val startFrame: Int) : Behavior()
        {
            var toX = Int.MIN_VALUE
            var toY = 0

            override fun onPress(p: UIPoint) {
                drawer = {
                    val w = this@FFABlock.width
                    val h = this@FFABlock.height

                    val dx = x + 24*(startFrame-min)
                    it.color = Color.BLACK
                    it.stroke = BasicStroke(2f)
                    it.drawRect(dx, y, 24, 24)

                    if( toX != Int.MIN_VALUE)
                    {
                        it.drawLine(dx + 12, y + 12, toX, toY)
                    }
                }
            }

            override fun onRelease(p: UIPoint) {
                behavior = null
                drawer = null
            }

            override fun onMove(p: UIPoint) {
                val pThis = p.convert(this@FFAAnimationSpaceView)
                toX = pThis.x
                toY = pThis.y
                this@FFAAnimationSpaceView.redraw()
            }
        }
    }



    private val __listener =animationSpace.stateView.animationSpaceObservable.addObserver( object : InternalAnimationSpaceObserver {
        override fun animationSpaceChanged(structureChange: Boolean) {
            rebuild()
        }
    })

    internal var drawer : ((Graphics2D)->Unit)? = null
        set(value) {field = value; redraw()}
}


private class FFASpacePanelImp : JPanel() {
    lateinit var context : FFAAnimationSpaceView

    fun setLayout( constructor: CrossInitializer.()->Unit) {
        removeAll()
        val list = mutableListOf<IComponent>()
        layout = CrossLayout.buildCrossLayout(this, list, constructor)
    }

    init {
    }

    override fun paint(g: Graphics) {
        val g2 = (g as Graphics2D)
        g2.color = background
        g2.fillRect(0,0,width,height)
        super.paintChildren(g2)
        context.drawer?.also{ it(g2)}
    }
}