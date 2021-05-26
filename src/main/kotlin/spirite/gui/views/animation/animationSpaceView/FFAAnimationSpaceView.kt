package spirite.gui.views.animation.animationSpaceView

import rb.vectrix.linear.Vec2f
import rb.vectrix.linear.Vec2i
import rb.vectrix.mathUtil.MathUtil
import rb.vectrix.mathUtil.f
import rb.vectrix.mathUtil.floor
import rb.vectrix.mathUtil.round
import rbJvm.owl.addWeakObserver
import sgui.core.UIPoint
import sgui.components.IComponent
import sgui.components.IComponent.BasicBorder.BASIC
import sgui.core.components.crossContainer.CrossInitializer
import sgui.core.components.crossContainer.ICrossPanel
import sgui.swing.advancedComponents.CrossContainer.CrossLayout
import sguiSwing.components.SwComponent
import sgui.hybrid.Hybrid
import spirite.base.imageData.animation.Animation
import spirite.base.imageData.animation.ffa.FixedFrameAnimation
import spirite.base.imageData.animationSpaces.FFASpace.FFAAnimationSpace
import spirite.base.imageData.animationSpaces.FFASpace.FFAAnimationSpace.SpacialLink
import spirite.base.imageData.animationSpaces.IAnimationSpaceView.InternalAnimationPlayObserver
import spirite.base.imageData.animationSpaces.IAnimationSpaceView.InternalAnimationSpaceObserver
import spirite.base.util.linear.Rect
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import javax.swing.JPanel
import kotlin.math.PI

class FFAAnimationSpaceView
private constructor(
        val animationSpace: FFAAnimationSpace,
        private val imp : ICrossPanel,
        private val innerImp : FFASpacePanelImp)
    : IComponent by imp
{
    init {innerImp.context = this}
    constructor(animationSpace: FFAAnimationSpace) : this(animationSpace, Hybrid.ui.CrossPanel(), FFASpacePanelImp())

    internal val blockSize = 24
    private val ffaBlocks = mutableListOf<FFABlock>()

    val btnAutoAssign = Hybrid.ui.Button("Auto-assign Chars")

    init /* Layout */ {
        imp.setLayout {
            rows.add(SwComponent(innerImp), flex = 100f)
            rows += {
                add(btnAutoAssign, height = 24)
                height = 24
            }
        }

        rebuild()
    }
    init /* input */ {
        btnAutoAssign.action = {
            var char = 'A'
            animationSpace.animations.forEach {
                animationSpace.stateView.setCharBind(it, char++)
            }
        }
        rebuild()
    }

    fun rebuild()
    {
        ffaBlocks.clear()

        innerImp.setLayout {

            animationSpace.animations.forEach {ffa->
                val location = animationSpace.stateView.logicalSpace[ffa] ?: Vec2i.Zero
                rows.addFlatGroup(location.yi) {
                    addGap(location.xi)
                    add(FFABlock(ffa).also { ffaBlocks.add(it) })
                    val char = animationSpace.stateView.charbinds[ffa]
                    if( char != null) add(Hybrid.ui.EditableLabel(char.toString()))
                }
            }
            rows += {add(Hybrid.ui.CrossPanel().also { it.opaque = false })}
        }
    }

    private fun getFrameFromPoint(x: Int, y: Int) : Pair<FFABlock, Int>?
    {
        for (block in ffaBlocks) {
            val bsX = x - block.x
            val bsY = y - block.y
            if( bsX >= 0 && bsX < block.animation.end * blockSize &&  bsY >= 0  && bsY < block.height)
                return Pair(block, bsX / blockSize)
        }

        return null
    }

    internal fun getFrameBounds(ffa: FixedFrameAnimation, frame: Int) : Rect?
    {
        val topleft = animationSpace.stateView.logicalSpace[ffa] ?: return null
        if( frame < ffa.start || frame >= ffa.end) return null

        return Rect(topleft.xi + blockSize*(frame - ffa.start), topleft.yi, blockSize, blockSize)
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
            imp.opaque = false
            imp.setLayout {

                (min until max).forEach {ind->
                    val comp = Hybrid.ui.Label(ind.toString())
                    comp.setBasicBorder(BASIC)
                    comp.opaque = false
                    comp.markAsPassThrough()
                    compMap[comp.component] =  ind
                    cols.add(comp, blockSize, blockSize)
                }

                val endLabel = Hybrid.ui.Label("-")
                endLabel.markAsPassThrough()
                endLabel.setBasicBorder(BASIC)
                endLabel.opaque = false
                cols.add(endLabel, blockSize/2, blockSize)
            }

            imp.onMousePress += {evt ->
                val frame = evt.point.convert(imp).x  / blockSize
                animationSpace.stateView.animation = animation
                animationSpace.stateView.met = frame.f

                behavior = behavior ?: when {
                    evt.holdingCtrl -> MoveAnimationBehavior()
                    else -> {
                        when {
                            frame == max -> CreateEndLinkBehavior()
                            frame < min || frame > max -> null
                            else -> CreateLinkBehavior(frame)
                        }
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
                    val w = this@FFABlock.width
                    val h = this@FFABlock.height
                    it.color = Color.BLACK
                    it.stroke = BasicStroke(2f)
                    it.drawRect(parentSpaceX + offsetX, parentSpaceY + offsetY, w, h)
                }
            }
            override fun onRelease(p: UIPoint) {
                val old = animationSpace.stateView.logicalSpace[animation]
                animationSpace.stateView.setLogicalSpace(animation, Vec2i((old?.xi
                        ?: 0) + p.x - startX, (old?.yi ?: 0) + p.y - startY))
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

            var toFrame : Pair<FFABlock,Int>? = null

            override fun onPress(p: UIPoint) {
                drawer = {
                    val w = this@FFABlock.width
                    val h = this@FFABlock.height

                    val dx = x + blockSize*(startFrame-min)
                    it.color = Color.BLACK
                    it.stroke = BasicStroke(2f)
                    it.drawRect(dx, y, blockSize, blockSize)

                    if( toX != Int.MIN_VALUE) {
                        it.drawLine(dx + blockSize/2, y + blockSize/2, toX, toY)
                    }
                    val toFrame = toFrame
                    if( toFrame != null) {
                        it.drawRect(toFrame.first.x + toFrame.second * blockSize, toFrame.first.y, blockSize, blockSize)
                    }
                }
            }

            override fun onRelease(p: UIPoint) {
                val toFrame = toFrame
                if( toFrame != null) {
                    val link = SpacialLink(
                            animation,
                            startFrame,
                            toFrame.first.animation,
                            toFrame.second)
                    if(animationSpace.links.contains(link))
                        animationSpace.removeLink(link)
                    else
                        animationSpace.addLink(link)
                }
                behavior = null
                drawer = null
            }

            override fun onMove(p: UIPoint) {
                val pThis = p.convert(this@FFAAnimationSpaceView)
                toX = pThis.x
                toY = pThis.y
                val getTo = getFrameFromPoint(toX, toY)
                if( getTo?.first != this@FFABlock)
                    toFrame = getTo
                this@FFAAnimationSpaceView.redraw()
            }
        }

        private inner class CreateEndLinkBehavior : Behavior()
        {
            var toX = Int.MIN_VALUE
            var toY = 0

            var toFrame : Pair<FFABlock,Int>? = null

            override fun onPress(p: UIPoint) {
                drawer = {g2->
                    val right = this@FFABlock.x + this@FFABlock.width
                    g2.stroke = BasicStroke(2f)
                    g2.color = Color.BLACK

                    if( toX != Int.MIN_VALUE)
                        g2.drawLine(right - blockSize/4, y + blockSize/2, toX, toY)

                    val to = toFrame
                    if( to == null) {
                        g2.color = Color.RED
                        g2.drawArc(right, this@FFABlock.y, blockSize / 2, blockSize, 90, -180)
                    }
                    else {
                        g2.drawRect(to.first.x + to.second * blockSize, to.first.y, blockSize, blockSize)
                    }
                }
            }

            override fun onRelease(p: UIPoint) {
                animationSpace.setOnEndBehavior(animation, toFrame?.run { Pair(first.animation, second) })
                behavior = null
                drawer = null
            }

            override fun onMove(p: UIPoint) {
                val pThis = p.convert(this@FFAAnimationSpaceView)
                toX = pThis.x
                toY = pThis.y
                val getTo = getFrameFromPoint(toX, toY)
                if( getTo?.first != this@FFABlock)
                    toFrame = getTo
                this@FFAAnimationSpaceView.redraw()
            }

        }
    }



    private val __listenerK =animationSpace.stateView.animationSpaceObservable.addWeakObserver( object : InternalAnimationSpaceObserver {
        override fun animationSpaceChanged(structureChange: Boolean) {
            rebuild()
        }
    })
    private val __listenerK2 =animationSpace.stateView.animationPlayObservable.addWeakObserver( object : InternalAnimationPlayObserver {
        override fun playStateChanged(animation: Animation?, frame: Float) {
            val ffa = animation as? FixedFrameAnimation ?: return
            val met = MathUtil.cycle(ffa.start, ffa.end, frame.floor)
            if( animation != innerImp.drawnFFA || met != innerImp.drawnFrame)
            {
                innerImp.repaint()
            }
        }
    })

    internal var drawer : ((Graphics2D)->Unit)? = null
        set(value) {field = value; redraw()}
}


private class FFASpacePanelImp : JPanel() {
    lateinit var context : FFAAnimationSpaceView

    var drawnFFA : FixedFrameAnimation? = null
    var drawnFrame: Int = 0

    fun setLayout( constructor: CrossInitializer.()->Unit) {
        removeAll()
        val list = mutableListOf<IComponent>()
        layout = CrossLayout.buildCrossLayout(this, list, constructor)
    }

    init {
        isOpaque = false
    }

    override fun paint(g: Graphics) {
        val g2 = (g as Graphics2D)
        g2.color = background
        g2.fillRect(0,0,width,height)

        paintMetBg(g2)
        paintLinks(g2)

        super.paintChildren(g2)
        context.drawer?.also{ it(g2)}
    }

    private fun paintMetBg(g2: Graphics2D)
    {
        drawnFFA = null
        val animation = context.animationSpace.stateView.animation ?: return
        val frame = MathUtil.cycle(animation.start, animation.end, context.animationSpace.stateView.met.floor)
        val region = context.getFrameBounds(animation, frame) ?: return

        drawnFFA = animation
        drawnFrame = frame

        g2.color = Color.YELLOW
        g2.fillRect(region.x, region.y, region.width, region.height + 8)
    }

    private fun paintLinks( g2: Graphics2D)
    {
        val space = context.animationSpace
        val logSpace = space.stateView.logicalSpace
        val size = context.blockSize

        fun drawLineFromTo(fromFFA: FixedFrameAnimation, fromFrame: Int, toFFA: FixedFrameAnimation, toFrame: Int)
        {
            val from = logSpace[fromFFA] ?: return
            val to = logSpace[toFFA] ?: return

            g2.color = Color.RED
            g2.stroke = BasicStroke(2f)


            val fromX = from.xi + size/2 + size * (fromFrame - fromFFA.start)
            val toX = to.xi + size/2 + size * (toFrame - toFFA.start)
            val fromY : Int
            val toY : Int
            if( from.yi < to.yi) {
                fromY = from.yi + size
                toY = to.yi
            }else {
                fromY = from.yi
                toY = to.yi + size
            }
            g2.drawLine(fromX, fromY,toX, toY)

            val vec = Vec2f(toX - fromX.f, toY - fromY.f).normalized
            val left = vec.rotate(PI.f*6f/5f)
            val right = vec.rotate(-PI.f*6f/5f)
            g2.drawLine(toX, toY, toX + (left.xf * 5).round, toY + (left.yf * 5).round)
            g2.drawLine(toX, toY, toX + (right.xf * 5).round, toY + (right.yf * 5).round)
        }

        for (link in space.links) {
            drawLineFromTo(link.origin, link.originFrame, link.destination, link.destinationFrame)
        }

        for( struct in space.animationStructs) {
            val to = struct.onEndLink ?: continue
            val animation = struct.animation
            drawLineFromTo(animation, animation.end, to.first, to.second)
        }
    }
}