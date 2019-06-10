package spirite.gui.views.animation

import rb.IContract
import rb.owl.bindable.Bindable
import rb.owl.bindable.addObserver
import rb.owl.observer
import rb.vectrix.mathUtil.*
import spirite.base.brains.IMasterControl
import spirite.base.imageData.animation.Animation
import spirite.base.imageData.animation.IAnimationManager.AnimationStructureChangeObserver
import spirite.gui.components.advanced.omniContainer.IOmniComponent
import sgui.generic.components.IComponent
import sgui.generic.components.IComponent.BasicBorder.BEVELED_RAISED
import spirite.gui.resources.IIcon
import sgui.skin.Skin
import spirite.gui.resources.SwIcons
import spirite.hybrid.Hybrid
import spirite.hybrid.ITimer
import spirite.pc.graphics.ImageBI
import sgui.swing.components.SJPanel
import sgui.swing.components.SwComponent
import spirite.pc.gui.jcolor
import java.awt.Graphics
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.round

class AnimationPreviewView(val masterControl: IMasterControl) : IOmniComponent {
    override val component: IComponent get() = imp
    override val icon: IIcon? get() = SwIcons.BigIcons.Frame_AnimationScheme
    override val name: String get() = "Animation Preview"


    val offsetXBind = Bindable(0)
    val offsetYBind = Bindable(0)
    var offsetX by offsetXBind
    var offsetY by offsetYBind

    private val viewPanel = AnimationViewPanel()
    private val btnPrev = Hybrid.ui.Button().also { it.setIcon(SwIcons.BigIcons.Anim_StepB) }
    private val btnPlay = Hybrid.ui.ToggleButton().also { it.setOffIcon(SwIcons.BigIcons.Anim_Play) }
    private val btnNext = Hybrid.ui.Button().also { it.setIcon(SwIcons.BigIcons.Anim_StepF) }
    private val ffFps = Hybrid.ui.FloatField()
    private val sliderMet = Hybrid.ui.Slider(0,100,0).also { it.snapsToTick = false }
    private val bgColorBox = Hybrid.ui.ColorSquare(Skin.Global.Bg.scolor).also { it.setBasicBorder(BEVELED_RAISED) }
    private val ifZoom = Hybrid.ui.IntField(allowsNegative = true).also { it.valueBind.addObserver { _,_ -> viewPanel.redraw()} }
    private val zoomP = Hybrid.ui.Button("+").also { it.action = {++ifZoom.value} }
    private val zoomM = Hybrid.ui.Button("-").also { it.action = {--ifZoom.value} }
    private val ifMet = Hybrid.ui.IntField(allowsNegative =  false)

    val bgColor by bgColorBox.colorBind

    private val imp = Hybrid.ui.CrossPanel {
        rows += {
            add( viewPanel, flex = 300f)
            flex = 300f
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
            add(Hybrid.ui.Label("FPS"))
            addGap(3)
            add(bgColorBox, width = 24, height = 24)
            addGap(10)
            add(Hybrid.ui.Label("Zoom:"))
            add(ifZoom, height = 24, width = 28)
            this += {
                add(zoomP, width = 12, height =  12)
                add(zoomM, width = 12, height =  12)
            }
        }
        rows += {
            add(sliderMet, width = 184)
            add(ifMet, width = 64, height = 24)
        }
        rows.padding = 3

    }

    private val timer : ITimer = Hybrid.timing.createTimer(15, true) {Hybrid.gle.runInGLContext { tick()}}

    private fun tick() {
        if( !btnPlay.checked) return
        val anim = animation ?: return
        anim.state.met = anim.state.met + anim.state.speed/66.666f
    }


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
        _fpsK = ffFps.valueBind.bindTo(anim.state.speedBind)
        _metK = metBind.bindTo(anim.state.metBind)
        _zoomK = ifZoom.valueBind.bindTo(anim.state.zoomBind)
        _offsetXK = offsetXBind.bindTo(anim.state.offsetXBind)
        _offsetYK = offsetYBind.bindTo(anim.state.offsetYBind)
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
                offsetX -= ((it.point.x - nowX) * (animation?.state?.zoomF ?: 1f)).round
                offsetY -= ((it.point.y - curY) * (animation?.state?.zoomF ?: 1f)).round
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
        btnNext.action = {animation?.also {it.state.met = MathUtil.cycle(it.startFrame, it.endFrame, it.state.met + 1f)  }}
        btnPrev.action = {animation?.also {it.state.met = MathUtil.cycle(it.startFrame, it.endFrame, it.state.met - 1f)  }}
    }

    override fun close() {
        unbind()
        _curAnimK.void()
        _animStructObsK.void()
        timer.stop()
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
                gc.preScale(anim.state.zoomF, anim.state.zoomF)
                gc.preTranslate(anim.state.offsetX.f, anim.state.offsetY.f)

                anim.drawFrame(gc,anim.state.met)

                val bi = Hybrid.imageConverter.convert<ImageBI>(image)
                image.flush()
                g.drawImage(bi.bi, 0, 0, null)
                bi.flush()
            }
        }
    }
}