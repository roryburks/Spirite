package spirite.gui.views.animation

import rb.extendo.delegates.OnChangeDelegate
import rb.vectrix.mathUtil.d
import rbJvm.glow.SColor
import rbJvm.glow.awt.ImageBI
import sgui.components.IComponent
import sguiSwing.components.SwComponent
import sguiSwing.hybrid.Hybrid
import sguiSwing.jcolor
import sguiSwing.skin.Skin.Global.Bg
import spirite.base.imageData.animation.Animation
import spirite.base.imageData.animation.ffa.FixedFrameAnimation
import java.awt.Graphics
import javax.swing.JPanel
import kotlin.math.floor

class AnimationPlayView
private constructor(private val imp: Imp) : IComponent by SwComponent(imp)
{
    //TODO: Add bindings such that if Animation Structure Changes, redraw.

    init { imp.context = this}
    constructor() : this(Imp())

    var animation: Animation? by OnChangeDelegate<Animation?>(null) { redraw() }
    var frame: Float = 0f
        set(value) {
            val newValue = if( animation is FixedFrameAnimation) floor(value) else value
            if( field != newValue) {
                field = newValue
                redraw()
            }
        }

    var bgColor: SColor by OnChangeDelegate(Bg.scolor) { redraw() }

    private class Imp : JPanel() {
        var context: AnimationPlayView? = null

        override fun paintComponent(g: Graphics) {
            val context = context?: return
            val anim = context.animation ?: return
            g.color = context.bgColor.jcolor
            g.fillRect(0,0,width, height)

            Hybrid.gle.runInGLContext {
                val image = Hybrid.imageCreator.createImage(width, height)
                val gc = image.graphics
                gc.preScale(anim.state.zoomF.d, anim.state.zoomF.d)

                anim.drawFrame(gc,context.frame)

                val bi = Hybrid.imageConverter.convert(image,ImageBI::class) as ImageBI
                image.flush()
                g.drawImage(bi.bi, 0, 0, null)
                bi.flush()
            }
        }
    }
}