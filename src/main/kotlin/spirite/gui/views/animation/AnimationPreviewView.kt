package spirite.gui.views.animation

import spirite.base.util.binding.Bindable
import spirite.base.brains.IMasterControl
import spirite.base.imageData.animation.Animation
import spirite.base.imageData.animation.IAnimationManager.AnimationStructureChangeObserver
import spirite.base.util.MathUtil
import spirite.base.util.ceil
import spirite.base.util.floor
import spirite.base.util.round
import spirite.gui.components.advanced.omniContainer.IOmniComponent
import spirite.gui.components.basic.IComponent
import spirite.gui.components.basic.IComponent.BasicBorder.BEVELED_RAISED
import spirite.gui.resources.IIcon
import spirite.gui.resources.Skin
import spirite.gui.resources.SwIcons
import spirite.hybrid.Hybrid
import spirite.hybrid.ITimer
import spirite.pc.graphics.ImageBI
import spirite.pc.gui.basic.SwComponent
import spirite.pc.gui.jcolor
import java.awt.Graphics
import javax.swing.JPanel
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.round

class AnimationPreviewView(val masterControl: IMasterControl) : IOmniComponent {
    override val component: IComponent get() = imp
    override val icon: IIcon? get() = SwIcons.BigIcons.Frame_AnimationScheme
    override val name: String get() = "Animation Preview"

    private val viewPanel = AnimationViewPanel()
    private val btnPrev = Hybrid.ui.Button().also { it.setIcon(SwIcons.BigIcons.Anim_StepB) }
    private val btnPlay = Hybrid.ui.ToggleButton().also { it.setOffIcon(SwIcons.BigIcons.Anim_Play) }
    private val btnNext = Hybrid.ui.Button().also { it.setIcon(SwIcons.BigIcons.Anim_StepF) }
    private val ffFps = Hybrid.ui.FloatField()
    private val sliderMet = Hybrid.ui.Slider(0,100,0).also { it.snapsToTick = false }
    private val bgColorBox = Hybrid.ui.ColorSquare(Skin.Global.Bg.scolor).also { it.setBasicBorder(BEVELED_RAISED) }
    private val ifZoom = Hybrid.ui.IntField(allowsNegative = true).also { it.valueBind.addRootListener { new, old -> viewPanel.redraw()} }
    private val zoomP = Hybrid.ui.Button("+").also { it.action = {++ifZoom.value} }
    private val zoomM = Hybrid.ui.Button("-").also { it.action = {--ifZoom.value} }

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
        }
        rows.padding = 3

    }

    private val timer : ITimer = Hybrid.timing.createTimer(15, true) {Hybrid.gle.runInGLContext { tick()}}

    private fun tick() {
        if( !btnPlay.checked) return
        val anim = animation ?: return
        anim.state.met = MathUtil.cycle(anim.startFrame, anim.endFrame, anim.state.met + anim.state.speed/66.666f)
    }


    // region Bindings
    private val _curAnimBind = masterControl.centralObservatory.currentAnimationBind.addListener { new, old ->buildFromAnim(new); viewPanel.redraw()}
    private val _animstructureObs = object : AnimationStructureChangeObserver {
        override fun animationStructureChanged(animation: Animation) {
            if( animation == this@AnimationPreviewView.animation) {
                updateSlider()
            }
        }
    }.also { masterControl.centralObservatory.trackingAnimationStateObserver.addObserver(it) }
    var animation : Animation? = null

    private val metBind = Bindable(0f) { new, _ ->
        sliderMet.value = (new * 100).floor
        viewPanel.redraw()
    }
    init {
        sliderMet.onMouseDrag += {  metBind.field = sliderMet.value / 100f }
        sliderMet.onMouseRelease +=  {it -> if( !btnPlay.checked)metBind.field = round(metBind.field) }
        btnPlay.checkBind.addRootListener { new, _ -> if(!new) metBind.field = floor(metBind.field) }
    }

    private fun unbind() {
        ffFps.valueBind.unbind()
        metBind.unbind()
        ifZoom.valueBind.unbind()
    }

    private fun rebind(anim: Animation)
    {
        anim.state.speedBind.bind(ffFps.valueBind)
        anim.state.metBind.bind(metBind)
        anim.state.zoomBind.bind(ifZoom.valueBind)
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

        bgColorBox.colorBind.addRootListener { new, old -> viewPanel.redraw() }
        btnNext.action = {animation?.also {it.state.met = MathUtil.cycle(it.startFrame, it.endFrame, it.state.met + 1f)  }}
        btnPrev.action = {animation?.also {it.state.met = MathUtil.cycle(it.startFrame, it.endFrame, it.state.met - 1f)  }}
    }

    override fun close() {
        unbind()
        _curAnimBind.unbind()
        timer.stop()
    }
}

private class AnimationViewPanel(val imp : AnimationViewPanelImp = AnimationViewPanelImp()) : IComponent by SwComponent(imp) {

    class AnimationViewPanelImp : JPanel() {
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

                anim.drawFrame(gc,anim.state.met)

                val bi = Hybrid.imageConverter.convert<ImageBI>(image)
                image.flush()
                g.drawImage(bi.bi, 0, 0, null)
                bi.flush()
            }
        }
    }
}