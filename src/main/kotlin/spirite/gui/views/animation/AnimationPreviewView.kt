package spirite.gui.views.animation

import rb.global.IContract
import rb.glow.gle.IGLEngine
import rb.owl.bindable.Bindable
import rb.owl.bindable.addObserver
import rb.owl.observer
import rb.vectrix.mathUtil.*
import rbJvm.glow.awt.ImageBI
import sgui.components.IComponent
import sgui.components.IComponent.BasicBorder.BEVELED_RAISED
import sgui.core.components.IComponentProvider
import sgui.swing.SwIcon
import sgui.swing.components.SJPanel
import sguiSwing.components.SwComponent
import sgui.core.systems.ITimer
import sgui.core.systems.ITimerEngine
import sgui.swing.jcolor
import sgui.swing.skin.Skin
import spirite.base.brains.IMasterControl
import spirite.base.imageData.animation.Animation
import spirite.base.imageData.animation.AnimationUtil
import spirite.base.imageData.animation.services.IAnimationManagementSvc.AnimationStructureChangeObserver
import spirite.base.imageData.animation.ffa.FixedFrameAnimation
import spirite.gui.components.advanced.omniContainer.IOmniComponent
import spirite.gui.resources.SpiriteIcons
import spirite.sguiHybrid.Hybrid
import java.awt.Graphics
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.round

class AnimationPreviewView(
    val masterControl: IMasterControl,
    private val _ui : IComponentProvider = Hybrid.ui,
    private val _gle : IGLEngine = Hybrid.gle
) : IOmniComponent
{
    override val component: IComponent get() = imp
    override val icon: SwIcon? get() = SpiriteIcons.BigIcons.Frame_AnimationScheme
    override val name: String get() = "Animation Preview"


    val offsetXBind = Bindable(0)
    val offsetYBind = Bindable(0)
    var offsetX by offsetXBind
    var offsetY by offsetYBind

    private val viewPanel = AnimationViewPanel()
    private val btnPrev = _ui.Button().also { it.setIcon(SpiriteIcons.BigIcons.Anim_StepB) }
    private val btnPlay = _ui.ToggleButton().also { it.setOffIcon(SpiriteIcons.BigIcons.Anim_Play) }
    private val btnNext = _ui.Button().also { it.setIcon(SpiriteIcons.BigIcons.Anim_StepF) }
    private val btnRecenter = _ui.Button("Center")
    private val ffFps = _ui.FloatField()
    private val sliderMet = _ui.Slider(0,100,0).also { it.snapsToTick = false }
    private val bgColorBox = _ui.ColorSquare(Skin.Global.Bg.scolor).also { it.setBasicBorder(BEVELED_RAISED) }
    private val ifZoom = _ui.IntField(allowsNegative = true).also { it.valueBind.addObserver { _,_ -> viewPanel.redraw()} }
    private val zoomP = _ui.Button("+").also { it.action = {++ifZoom.value} }
    private val zoomM = _ui.Button("-").also { it.action = {--ifZoom.value} }
    private val ifMet = _ui.IntField(allowsNegative =  false)

    val bgColor by bgColorBox.colorBind

    private val imp = _ui.CrossPanel {
        rows += {
            add( viewPanel, flex = 800f)
            flex = 800f
        }
        rows.addGap(3)
        rows += {
            add(btnPrev, width = 24, height = 24)
            addGap(2)
            add(btnPlay, width = 24, height = 24)
            addGap(2)
            add(btnNext, width = 24, height = 24)
            addGap(5)
            add(ffFps, width = 128, height = 24)
            add(_ui.Label("FPS"))
            addGap(3)
            add(bgColorBox, width = 24, height = 24)
            addGap(10)
            add(_ui.Label("Zoom:"))
            add(ifZoom, height = 24, width = 28)
            this += {
                add(zoomP, width = 12, height =  12)
                add(zoomM, width = 12, height =  12)
            }
        }
        rows += {
            add(sliderMet, width = 184)
            add(ifMet, width = 64, height = 24)
            addGap(0, 0, Short.MAX_VALUE.i)
            add(btnRecenter)
        }
        rows.padding = 3

    }

    // region Timer Stuff
    private val _timing : ITimerEngine get() = Hybrid.timing
    private val _timer : ITimer = _timing.createTimer(15, true) {_gle.runInGLContext { tick()}}

    private var _prevTime : Long? = null
    private fun tick() {
        // Set _prevTime to null in case early exit, means tick's not running
        val prevTime = _prevTime
        _prevTime = null

        // Return if not playing
        if( !btnPlay.checked) return
        val anim = animation ?: return

        // Uptick the animation using the actual ms passed, rather than relying it on being close to 15
        val now = _timing.currentMilli
        val actualMsElapsed = if( prevTime == null) 15 else now - prevTime
        anim.stateBind.met = anim.stateBind.met + anim.stateBind.speed/(1000f/actualMsElapsed)
        _prevTime = now
    }
    // endregion


    // region Bindings
    init {
        offsetXBind.addObserver { _, _ -> viewPanel.redraw() }
        offsetYBind.addObserver { _, _ -> viewPanel.redraw() }
    }

    private val _curAnimK = masterControl.centralObservatory.currentAnimationBind.addObserver { new, _ ->
        buildFromAnim(new)
        viewPanel.redraw()
    }
    private val _animStructObsK = masterControl.centralObservatory.trackingAnimationStateObserver.addObserver(
        object : AnimationStructureChangeObserver {
            override fun animationStructureChanged(animation: Animation) {
                if( animation == this@AnimationPreviewView.animation) {
                    updateSlider()
                }
            }
        }.observer()
    )
    var animation : Animation? = null

    private val metBind = Bindable(0f)
            .also { it.addObserver { new, _ ->
                sliderMet.value = ((new % (animation?.endFrame ?: 0f)) * 100).floor
                viewPanel.redraw()
            } }
    init {
        sliderMet.onMouseDrag += {  metBind.field = sliderMet.value / 100f }
        sliderMet.onMouseRelease +=  {it -> if( !btnPlay.checked)metBind.field = round(metBind.field) }
        btnPlay.checkBind.addObserver { new, _ -> if(!new) metBind.field = floor(metBind.field) }
        metBind.addObserver { new, _ ->ifMet.value = (new  % (animation?.endFrame ?: 0f)) .floor}
    }

    private var _fpsK : IContract? = null
    private var _zoomK : IContract? = null
    private var _metK : IContract? = null
    private var _offsetXK : IContract? = null
    private var _offsetYK : IContract? = null

    private fun unbind() {
        _fpsK?.void()
        _metK?.void()
        _zoomK?.void()
        _offsetXK?.void()
        _offsetYK?.void()
    }

    var i = 1.0f

    private fun rebind(anim: Animation)
    {
        _fpsK = ffFps.valueBind.bindTo(anim.stateBind.speedBind)
        _metK = metBind.bindTo(anim.stateBind.metBind)
        _zoomK = ifZoom.valueBind.bindTo(anim.stateBind.zoomBind)
        _offsetXK = offsetXBind.bindTo(anim.stateBind.offsetXBind)
        _offsetYK = offsetYBind.bindTo(anim.stateBind.offsetYBind)
    }
    // endregion

    // region Mouse Controls
    init {
        var curX : Int? = null
        var curY = 0
        viewPanel.onMousePress += {
            if( Hybrid.keypressSystem.holdingSpace) {
                viewPanel.requestFocus()
                curX = it.point.x
                curY = it.point.y
            }
        }
        viewPanel.onMouseDrag += {
            val nowX = curX
            if( nowX != null && Hybrid.keypressSystem.holdingSpace) {
                offsetX -= ((it.point.x - nowX) * (animation?.stateBind?.zoomF ?: 1f)).round
                offsetY -= ((it.point.y - curY) * (animation?.stateBind?.zoomF ?: 1f)).round
                curX = it.point.x
                curY = it.point.y
            }
            else curX = null
        }
        viewPanel.onMouseRelease += {curX = null}
    }
    // endregion

    private fun buildFromAnim( anim: Animation?) {
        animation = anim

        unbind()
        updateSlider()
        if( anim != null) rebind(anim)
    }


    private fun updateSlider() {
        val anim = animation

        if( anim != null) {
            sliderMet.min = (100 * anim.startFrame).floor
            sliderMet.max = (100 * anim.endFrame).ceil
            sliderMet.tickSpacing = max(100,((anim.endFrame - anim.startFrame)*100 / 20).round)
            sliderMet.setLabels(mapOf(sliderMet.min to anim.startFrame.toString(), sliderMet.max to anim.endFrame.toString()))
        }
        else {
            sliderMet.min = 0
            sliderMet.max = 100
            sliderMet.value = 0
        }
    }



    init {
        imp.ref = this
        viewPanel.imp.context = this
        buildFromAnim(masterControl.workspaceSet.currentWorkspace?.animationManager?.currentAnimation)

        bgColorBox.colorBind.addObserver { _, _ -> viewPanel.redraw() }
        btnNext.action = {animation?.also {it.stateBind.met = MathUtil.cycle(it.startFrame, it.endFrame, it.stateBind.met + 1f)  }}
        btnPrev.action = {animation?.also {it.stateBind.met = MathUtil.cycle(it.startFrame, it.endFrame, it.stateBind.met - 1f)  }}
        btnRecenter.action = {
            (animation as? FixedFrameAnimation)?.run {
                val drawBoundary = AnimationUtil.getAnimationBoundaries(this)
                offsetX = -drawBoundary.x1i
                offsetY = -drawBoundary.y1i
            }
        }
    }

    override fun close() {
        unbind()
        _curAnimK.void()
        _animStructObsK.void()
        _timer.stop()
    }
}

private class AnimationViewPanel(val imp : AnimationViewPanelImp = AnimationViewPanelImp()) : IComponent by SwComponent(imp) {

    class AnimationViewPanelImp : SJPanel() {
        var context : AnimationPreviewView? = null

        override fun paintComponent(g: Graphics) {
            val context = context ?: return
            val anim = context.animation ?: return
            g.color = context.bgColor.jcolor
            g.fillRect(0,0,width, height)

            Hybrid.gle.runInGLContext {
                val image = Hybrid.imageCreator.createImage(width, height)
                val gc = image.graphics
                gc.preScale(anim.stateBind.zoomF.d, anim.stateBind.zoomF.d)
                gc.preTranslate(anim.stateBind.offsetX.d, anim.stateBind.offsetY.d)

                anim.drawFrame(gc,anim.stateBind.met)

                val bi = Hybrid.imageConverter.convert(image,ImageBI::class) as ImageBI
                image.flush()
                g.drawImage(bi.bi, 0, 0, null)
                bi.flush()
            }
        }
    }
}