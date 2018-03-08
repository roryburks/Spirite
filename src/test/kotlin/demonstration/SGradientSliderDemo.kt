package demonstration

import spirite.base.util.InvertibleFunction
import spirite.gui.Basic.SGradientSlider
import spirite.gui.ResizeContainerPanel
import spirite.gui.ResizeContainerPanel.ContainerOrientation.HORIZONATAL
import java.awt.Color
import java.awt.GridLayout
import javax.swing.JButton
import javax.swing.JFrame


fun main( args: Array<String>) {
    DemoLauncher.launch(SGradientSliderDemo())
}

class SGradientSliderDemo : JFrame() {
    init {
        layout = GridLayout()

        val sliderThing = SGradientSlider(label = "Thing: ")

        var buttonReset = JButton("SetThing")
        buttonReset.addActionListener { sliderThing.value = 0.5f }


        val sliderBound1 = SGradientSlider(label = "Bound 1 (x^2):")
        val sliderBound2 = SGradientSlider(label = "Bound 2:")
        sliderBound1.mutator = object: InvertibleFunction<Float> {
            override fun perform(x: Float): Float = Math.pow(x.toDouble(), 2.0).toFloat()
            override fun invert(x: Float): Float= Math.pow(x.toDouble(), 1.0/2.0).toFloat()
        }
        sliderBound2.valueBind.bind(sliderBound1.valueBind)
        sliderBound1.fgGradLeft = Color(30, 160, 30)
        sliderBound1.fgGradRight = Color.BLACK

        val disabledSlider = SGradientSlider(label = "Disabled")
        disabledSlider.value = 0.4f
        disabledSlider.isEnabled = false



        val resize = ResizeContainerPanel( sliderThing, HORIZONATAL)
        resize.minStretch = 100
        resize.addPanel(buttonReset, 100,100,-999)
        resize.addPanel(disabledSlider, 100,100,-999)
        resize.addPanel(sliderBound1, 100,100,999)
        resize.addPanel(sliderBound2, 100,100,999)

        add( resize)
    }
}