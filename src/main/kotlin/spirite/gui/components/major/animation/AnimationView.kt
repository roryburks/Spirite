package spirite.gui.components.major.animation

import com.jogamp.opengl.GLAutoDrawable
import com.jogamp.opengl.GLEventListener
import com.jogamp.opengl.awt.GLJPanel
import spirite.base.brains.Bindable
import spirite.base.brains.IMasterControl
import spirite.base.graphics.gl.GLGraphicsContext
import spirite.base.graphics.gl.GLImage
import spirite.base.imageData.animation.Animation
import spirite.base.imageData.animation.ffa.FixedFrameAnimation
import spirite.base.util.*
import spirite.gui.components.advanced.omniContainer.IOmniComponent
import spirite.gui.components.basic.IComponent
import spirite.gui.components.basic.IComponent.BasicBorder.BEVELED_RAISED
import spirite.gui.resources.IIcon
import spirite.gui.resources.Skin
import spirite.gui.resources.SwIcons
import spirite.hybrid.Hybrid
import spirite.hybrid.ITimer
import spirite.pc.JOGL.JOGLProvider
import spirite.pc.graphics.ImageBI
import spirite.pc.gui.basic.SwComponent
import spirite.pc.gui.basic.SwPanel
import spirite.pc.gui.jcolor
import java.awt.Color
import java.awt.Graphics
import javax.swing.JPanel
import kotlin.math.max

class AnimationView(val masterControl: IMasterControl) : IOmniComponent {
    override val component: IComponent get() = imp
    override val icon: IIcon? get() = SwIcons.BigIcons.Frame_AnimationScheme

    var timer : ITimer = Hybrid.timing.createTimer(100, true) {tick()}

    private val viewPanel = AnimationViewPanel()
    private val btnPrev = Hybrid.ui.Button().also { it.setIcon(SwIcons.BigIcons.Anim_StepB) }
    private val btnPlay = Hybrid.ui.ToggleButton().also { it.setOffIcon(SwIcons.BigIcons.Anim_Play) }
    private val btnNext = Hybrid.ui.Button().also { it.setIcon(SwIcons.BigIcons.Anim_StepF) }
    private val ffFps = Hybrid.ui.FloatField()
    private val sliderMet = Hybrid.ui.Slider(0,100,0)
    private val bgColorBox = Hybrid.ui.ColorSquare(Skin.Global.Bg.scolor).also { it.setBasicBorder(BEVELED_RAISED) }

    val bgColor by bgColorBox.colorBind

    val imp = Hybrid.ui.CrossPanel {
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
            add(ffFps, width = 128)
            add(Hybrid.ui.Label("FPS"))
            addGap(3)
            add(bgColorBox, width = 24, height = 24)
        }
        rows += {
            add(sliderMet, width = 184)
        }
        rows.padding = 3

    }


    private val _curAnimBind = masterControl.centralObservatory.currentAnimationBind.addListener { new, old ->buildFromAnim(new)}
    var animation : Animation? = null

    private val metBind = Bindable(0f) { new, _ -> sliderMet.value = (new * 100).floor ; viewPanel.redraw()}
    init {
        sliderMet.valueBind.addListener { new, _ ->  metBind.field = new / 100f }
    }

    private fun buildFromAnim( anim: Animation?) {
        animation = anim

        ffFps.valueBind.unbind()
        metBind.unbind()

        updateSlider()

        if( anim != null) {
            anim.state.speedBind.bind(ffFps.valueBind)
            anim.state.metBind.bind(metBind)
        }
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

    private fun tick() {
        if( !btnPlay.checked) return
        val anim = animation ?: return
        anim.state.met = MathUtil.cycle(anim.startFrame, anim.endFrame, anim.state.met + anim.state.speed)
    }


    init {
        viewPanel.imp.context = this
        buildFromAnim(masterControl.workspaceSet.currentWorkspace?.animationManager?.currentAnimation)

        bgColorBox.colorBind.addRootListener { new, old -> viewPanel.redraw() }
        btnNext.action = {
            println(animation?.state?.met)
            animation?.state?.met = 50f}
    }

    override fun close() {
        _curAnimBind.unbind()
        ffFps.valueBind.unbind()
    }
}

private class AnimationViewPanel(val imp : AnimationViewPanelImp = AnimationViewPanelImp()) : IComponent by SwComponent(imp) {

    class AnimationViewPanelImp : JPanel() {
        var context : AnimationView? = null

        override fun paintComponent(g: Graphics) {
            val context = context ?: return
            val anim = context.animation ?: return
            g.color = context.bgColor.jcolor
            g.fillRect(0,0,width, height)


            val image = Hybrid.imageCreator.createImage(width, height)
            val gc = image.graphics
            gc.preScale(anim.state.zoomF, anim.state.zoomF)

            anim.drawFrame(gc,anim.state.met)

            val bi = Hybrid.imageConverter.convert<ImageBI>(image)
            g.drawImage(bi.bi, 0, 0, null)
        }
    }
}