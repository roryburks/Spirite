package demonstration

import rb.glow.Colors
import rb.vectrix.functions.InvertibleFunction
import sgui.core.Orientation.HORIZONTAL
import sgui.components.IButton
import sguiSwing.components.ResizeContainerPanel
import sguiSwing.components.jcomponent
import sguiSwing.hybrid.Hybrid
import java.awt.GridLayout
import javax.swing.JFrame


fun main( args: Array<String>) {
    DemoLauncher.launch(SGradientSliderDemo())
}

class SGradientSliderDemo : JFrame() {
    init {
        layout = GridLayout()

        val sliderThing = Hybrid.ui.GradientSlider(label = "Thing: ")

        val buttonReset : IButton = Hybrid.ui.Button("SetThing")
        buttonReset.action = { sliderThing.value = 0.5f }


        val sliderBound1 = Hybrid.ui.GradientSlider(label = "Bound 1 (xi^2):")
        val sliderBound2 = Hybrid.ui.GradientSlider(label = "Bound 2:")
        sliderBound1.mutatorPositionToValue = object: InvertibleFunction<Float> {
            override fun perform(x: Float): Float = Math.pow(x.toDouble(), 2.0).toFloat()
            override fun invert(x: Float): Float= Math.pow(x.toDouble(), 1.0/2.0).toFloat()
        }
        sliderBound2.valueBind.bindTo(sliderBound1.valueBind)
        sliderBound1.fgGradLeft = Colors.BLUE
        sliderBound1.fgGradRight = Colors.BLACK

        val disabledSlider = Hybrid.ui.GradientSlider(label = "Disabled")
        disabledSlider.value = 0.4f
        //disabledSlider.imp.isEnabled = false

        val resize = ResizeContainerPanel(sliderThing, HORIZONTAL)
        resize.minStretch = 100
        resize.addPanel(buttonReset, 100,100,-999)
        resize.addPanel(disabledSlider, 100,100,-999)
        resize.addPanel(sliderBound1, 100,100,999)
        resize.addPanel(sliderBound2, 100,100,999)

        add( resize.jcomponent)
    }
}