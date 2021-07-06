package spirite.gui.views.animation

import rb.global.IContract
import rb.glow.SColor
import rb.glow.gle.IGLEngine
import rb.owl.bindable.Bindable
import rb.owl.bindable.PushPullBind
import rb.owl.bindable.addObserver
import rb.owl.observer
import rb.vectrix.mathUtil.*
import rbJvm.glow.awt.ImageBI
import sgui.components.IComponent
import sgui.components.IComponent.BasicBorder.BEVELED_RAISED
import sgui.core.components.IComponentProvider
import sgui.core.systems.IKeypressSystem
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
import spirite.base.imageData.animation.services.AnimationStateBind
import spirite.gui.components.advanced.omniContainer.IOmniComponent
import spirite.gui.resources.SpiriteIcons
import spirite.sguiHybrid.Hybrid
import spirite.sguiHybrid.IHybrid
import java.awt.Graphics
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.round

class AnimationPreviewView(
    val masterControl: IMasterControl,
    private val _ui : IComponentProvider = Hybrid.ui,
    private val _gle : IGLEngine = Hybrid.gle,
    private val _timing : ITimerEngine = Hybrid.timing,
    private val _keypressSystem: IKeypressSystem = Hybrid.keypressSystem
) : IOmniComponent
{
    override val component: IComponent get() = imp
    override val icon: SwIcon? get() = SpiriteIcons.BigIcons.Frame_AnimationScheme
    override val name: String get() = "Animation Preview"

    private val _controller = AnimationPreviewController()


    private val viewPanel = SwComponent(AnimationViewPanel(_controller))
    private val btnPrev = _ui.Button().also { it.setIcon(SpiriteIcons.BigIcons.Anim_StepB) }
    private val btnPlay = _ui.ToggleButton().also { it.setOffIcon(SpiriteIcons.BigIcons.Anim_Play) }
    private val btnNext = _ui.Button().also { it.setIcon(SpiriteIcons.BigIcons.Anim_StepF) }
    private val btnRecenter = _ui.Button("Center")
    private val ffFps = _ui.FloatField()
    private val sliderMet = _ui.Slider(0,100,0).also { it.snapsToTick = false }
    private val bgColorBox = _ui.ColorSquare(_controller.color).also { it.setBasicBorder(BEVELED_RAISED) }
    private val ifZoom = _ui.IntField(allowsNegative = true).also { it.valueBind.addObserver { _,_ -> viewPanel.redraw()} }
    private val zoomP = _ui.Button("+").also { it.action = {++ifZoom.value} }
    private val zoomM = _ui.Button("-").also { it.action = {--ifZoom.value} }
    private val ifMet = _ui.IntField(allowsNegative =  false)

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
    private val _timer : ITimer = _timing.createTimer(15, true) {_gle.runInGLContext { tick()}}

    private var _prevTime : Long? = null
    private fun tick() {
        // Set _prevTime to null in case early exit, means tick's not running
        val prevTime = _prevTime
        _prevTime = null

        // Return if not playing
        if( !btnPlay.checked) return

        // Uptick the animation using the actual ms passed, rather than relying it on being close to 15
        val now = _timing.currentMilli
        val actualMsElapsed = if( prevTime == null) 15 else now - prevTime
        _controller.advanceAnimation(actualMsElapsed)
        _prevTime = now
    }
    // endregion

    // region Bindings
    init {
        _controller.animBind.offsetXBind.addObserver { _, _ -> viewPanel.redraw() }
        _controller.animBind.offsetYBind.addObserver { _, _ -> viewPanel.redraw() }

        _controller.animBind.metBind
            .addObserver { new, _ ->
                sliderMet.value = ((new % (_controller.animation?.endFrame ?: 0f)) * 100).floor
                viewPanel.redraw()
            }
        _controller.animBind.metBind
            .addObserver { new, _ ->ifMet.value = (new  % (_controller.animation?.endFrame ?: 0f)) .floor}

        ifZoom.valueBind.bindTo(_controller.animBind.zoomBind)
        bgColorBox.colorBind.bindTo(_controller.colorBind)
    }

    private val _curAnimK = masterControl.centralObservatory.currentAnimationBind.addObserver { new, _ ->
        buildFromAnim(new)
        viewPanel.redraw()
    }
    private val _animStructObsK = masterControl.centralObservatory.trackingAnimationStateObserver.addObserver(
        object : AnimationStructureChangeObserver {
            override fun animationStructureChanged(animation: Animation) {
                if( animation == this@AnimationPreviewView._controller.animation) {
                    updateSlider()
                }
            }
        }.observer()
    )

    init {
        sliderMet.onMouseDrag += {  _controller.animBind.met = sliderMet.value / 100f }
        sliderMet.onMouseRelease +=  {it -> if( !btnPlay.checked) _controller.animBind.met = round(_controller.animBind.met) }
        btnPlay.checkBind.addObserver { new, _ -> if(!new) _controller.animBind.met = floor(_controller.animBind.met) }
    }
    // endregion

    //  Mouse Controls
    init /* Mouse Controls */ {
        var curX : Int? = null
        var curY = 0
        viewPanel.onMousePress += {
            if( _keypressSystem.holdingSpace) {
                viewPanel.requestFocus()
                curX = it.point.x
                curY = it.point.y
            }
        }
        viewPanel.onMouseDrag += {
            val nowX = curX
            if( nowX != null && _keypressSystem.holdingSpace) {
                _controller.animBind.offsetX -= ((it.point.x - nowX) * (_controller.animBind.zoomF ?: 1f)).round
                _controller.animBind.offsetY -= ((it.point.y - curY) * (_controller.animBind.zoomF ?: 1f)).round
                curX = it.point.x
                curY = it.point.y
            }
            else curX = null
        }
        viewPanel.onMouseRelease += {curX = null}
    }

    init /* Action Bindings */{

        bgColorBox.colorBind.addObserver { _, _ -> viewPanel.redraw() }
        btnNext.action = {_controller.offsetAnimation(1f, true)}
        btnPrev.action = {_controller.offsetAnimation(-1f, true)}
        btnRecenter.action = {
            (_controller.animation as? FixedFrameAnimation)?.run {
                val drawBoundary = AnimationUtil.getAnimationBoundaries(this)
                _controller.animBind.offsetX = -drawBoundary.x1i
                _controller.animBind.offsetY = -drawBoundary.y1i
            }
        }
        ffFps.valueBind.bindTo(_controller.animBind.speedBind)
    }
    // endregion

    init {
        imp.ref = this
        buildFromAnim(masterControl.workspaceSet.currentWorkspace?.animationManager?.currentAnimation)
    }

    private fun buildFromAnim( anim: Animation?) {
        _controller.setAnimation(anim)
        updateSlider()
    }

    private fun updateSlider() {
        val anim = _controller.animation

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

    override fun close() {
        _controller.setAnimation(null) // Unsets Binds
        _curAnimK.void()
        _animStructObsK.void()
        _timer.stop()
    }
}

private class AnimationPreviewController {
    val colorBind : Bindable<SColor> = Bindable(Skin.Global.Bg.scolor)
    var color : SColor by colorBind

    var animation: Animation? = null ; private set
    val animBind : AnimationStateBind = AnimationStateBind()
    private var bindContracts : List<IContract> = emptyList()

    fun setAnimation(anim: Animation?) {
        if( anim == animation)
            return

        bindContracts.forEach { it.void() }
        if( anim == null) {
            bindContracts = emptyList()
        }
        else {
            val stateBind = anim.workspace.animationStateSvc.getState(anim)
            bindContracts = listOf(
                PushPullBind(animBind.metBind, stateBind.metBind),
                PushPullBind(animBind.zoomBind, stateBind.zoomBind),
                PushPullBind(animBind.offsetXBind, stateBind.offsetXBind),
                PushPullBind(animBind.offsetYBind, stateBind.offsetYBind),
                PushPullBind(animBind.speedBind, stateBind.speedBind) )
        }

        animation = anim
    }

    fun offsetAnimation(offset: Float, round: Boolean = false ) {
        val anim = animation ?: return
        val state = anim.workspace.animationStateSvc.getState(anim)
        val unrounded = MathUtil.cycle(anim.startFrame, anim.endFrame, state.met + offset)
        state.met = if( round) round(unrounded) else unrounded
    }

    fun advanceAnimation(elapsedMs: Long) {
        val anim = animation ?: return
        val state = anim.workspace.animationStateSvc.getState(anim)
        state.met = state.met + state.speed/(1000f/elapsedMs)
    }
}

private class AnimationViewPanel( val controller: AnimationPreviewController ) : SJPanel() {

    override fun paintComponent(g: Graphics) {
        val controller = controller
        val anim = controller.animation ?: return
        val animState = anim.workspace.animationStateSvc.getState(anim)

        g.color = controller.color.jcolor
        g.fillRect(0,0,width, height)

        Hybrid.gle.runInGLContext {
            val image = Hybrid.imageCreator.createImage(width, height)
            val gc = image.graphics
            gc.preScale(animState.zoomF.d, animState.zoomF.d)
            gc.preTranslate(animState.offsetX.d, animState.offsetY.d)

            anim.drawFrame(gc,animState.met)

            val bi = Hybrid.imageConverter.convert(image,ImageBI::class) as ImageBI
            image.flush()
            g.drawImage(bi.bi, 0, 0, null)
            bi.flush()
        }
    }
}